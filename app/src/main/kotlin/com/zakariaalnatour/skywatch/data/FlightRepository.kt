package com.zakariaalnatour.skywatch.data

import com.zakariaalnatour.skywatch.model.Aircraft
import com.zakariaalnatour.skywatch.model.AircraftMetadata
import com.zakariaalnatour.skywatch.model.FeedSource
import com.zakariaalnatour.skywatch.model.FlightEnrichment
import com.zakariaalnatour.skywatch.model.FlightFeedResult
import com.zakariaalnatour.skywatch.model.FlightHub
import com.zakariaalnatour.skywatch.model.FlightHubs
import com.zakariaalnatour.skywatch.model.FlightRoute
import com.zakariaalnatour.skywatch.model.HubFeedMode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

interface FlightRepository {
  suspend fun fetchHubFlights(hub: FlightHub): FlightFeedResult
  suspend fun searchGlobal(query: String): FlightFeedResult
  suspend fun fetchEnrichment(hex: String, callsign: String?): FlightEnrichment
}

/**
 * Live keyless ADS-B repository:
 * - Primary regional feed: `https://opendata.adsb.fi/api/v2/lat/{lat}/lon/{lon}/dist/{dist}`
 * - Automatic failover + military + global search + widebody: `https://api.adsb.lol/v2/...`
 * - Routes & aircraft photos: `https://api.adsbdb.com/v0/...` (cached in memory)
 */
class LiveFlightRepository(
  private val httpClient: OkHttpClient = defaultHttpClient(),
  private val adsbFiBaseUrl: String = "https://opendata.adsb.fi/api",
  private val adsbLolBaseUrl: String = "https://api.adsb.lol",
  private val adsbdbBaseUrl: String = "https://api.adsbdb.com",
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
  private val clockMs: () -> Long = { System.currentTimeMillis() },
) : FlightRepository {

  private val routeCache = ConcurrentHashMap<String, CachedOptional<FlightRoute>>()
  private val metadataCache = ConcurrentHashMap<String, CachedOptional<AircraftMetadata>>()

  override suspend fun fetchHubFlights(hub: FlightHub): FlightFeedResult = when (hub.feedMode) {
    HubFeedMode.REGIONAL -> fetchRegionalWithFailover(hub.latitude, hub.longitude, hub.radiusNm)
    HubFeedMode.MILITARY -> fetchMilitaryFeed()
    HubFeedMode.WIDEBODY -> fetchGlobalWidebodyFeed()
  }

  private suspend fun fetchRegionalWithFailover(
    lat: Double,
    lon: Double,
    radiusNm: Int,
  ): FlightFeedResult = withContext(ioDispatcher) {
    val startMs = clockMs()
    val latStr = String.format(Locale.US, "%.4f", lat)
    val lonStr = String.format(Locale.US, "%.4f", lon)
    val primaryUrl = "$adsbFiBaseUrl/v2/lat/$latStr/lon/$lonStr/dist/$radiusNm"

    val primaryAttempt = runCatching { executeGet(primaryUrl) }
    if (primaryAttempt.isSuccess) {
      val parsed = AdsbJsonParser.parseAircraftList(primaryAttempt.getOrThrow())
      return@withContext FlightFeedResult(
        aircraft = parsed,
        source = FeedSource.ADSB_FI,
        latencyMs = (clockMs() - startMs).coerceAtLeast(1L),
        timestampMs = clockMs(),
        failoverActive = false,
      )
    }

    // Automatic failover to api.adsb.lol
    val failoverUrl = "$adsbLolBaseUrl/v2/lat/$latStr/lon/$lonStr/dist/$radiusNm"
    val failoverBody = executeGet(failoverUrl)
    val parsed = AdsbJsonParser.parseAircraftList(failoverBody)
    FlightFeedResult(
      aircraft = parsed,
      source = FeedSource.ADSB_LOL_FAILOVER,
      latencyMs = (clockMs() - startMs).coerceAtLeast(1L),
      timestampMs = clockMs(),
      failoverActive = true,
      note = "Primary opendata.adsb.fi unavailable; active failover on api.adsb.lol",
    )
  }

  private suspend fun fetchMilitaryFeed(): FlightFeedResult = withContext(ioDispatcher) {
    val startMs = clockMs()
    val body = executeGet("$adsbLolBaseUrl/v2/mil")
    val parsed = AdsbJsonParser.parseAircraftList(body)
    FlightFeedResult(
      aircraft = parsed,
      source = FeedSource.ADSB_LOL_MIL,
      latencyMs = (clockMs() - startMs).coerceAtLeast(1L),
      timestampMs = clockMs(),
      failoverActive = false,
    )
  }

  private suspend fun fetchGlobalWidebodyFeed(): FlightFeedResult = withContext(ioDispatcher) {
    val startMs = clockMs()
    val merged = LinkedHashMap<String, Aircraft>()
    var successCount = 0
    var lastError: Throwable? = null

    coroutineScope {
      val deferreds = FlightHubs.WIDEBODY_QUERY_TYPES.map { icaoType ->
        async(ioDispatcher) {
          runCatching {
            val body = executeGet("$adsbLolBaseUrl/v2/type/$icaoType")
            AdsbJsonParser.parseAircraftList(body)
          }
        }
      }
      for (result in deferreds.awaitAll()) {
        result.onSuccess { list ->
          successCount++
          for (ac in list) {
            merged[ac.hex] = ac
          }
        }.onFailure { err ->
          lastError = err
        }
      }
    }

    if (successCount == 0 && lastError != null) {
      throw lastError
    }

    FlightFeedResult(
      aircraft = merged.values.toList(),
      source = FeedSource.ADSB_LOL_WIDEBODY,
      latencyMs = (clockMs() - startMs).coerceAtLeast(1L),
      timestampMs = clockMs(),
      failoverActive = false,
    )
  }

  override suspend fun searchGlobal(query: String): FlightFeedResult = withContext(ioDispatcher) {
    val normalized = query.trim().uppercase(Locale.US)
    require(normalized.isNotBlank()) { "Search query must not be blank" }
    val startMs = clockMs()
    val encoded = URLEncoder.encode(normalized, Charsets.UTF_8.name())

    val candidateUrls = buildGlobalSearchUrls(normalized, encoded)
    val merged = LinkedHashMap<String, Aircraft>()
    var anySuccess = false
    var lastError: Throwable? = null

    for (url in candidateUrls) {
      val attempt = runCatching { executeGet(url) }
      attempt.onSuccess { body ->
        anySuccess = true
        for (ac in AdsbJsonParser.parseAircraftList(body)) {
          merged[ac.hex] = ac
        }
      }.onFailure { err ->
        lastError = err
      }
      if (merged.isNotEmpty()) break
    }

    if (!anySuccess && lastError != null) {
      throw lastError
    }

    FlightFeedResult(
      aircraft = merged.values.toList(),
      source = FeedSource.ADSB_LOL_SEARCH,
      latencyMs = (clockMs() - startMs).coerceAtLeast(1L),
      timestampMs = clockMs(),
      note = "Global search: $normalized",
    )
  }

  internal fun buildGlobalSearchUrls(normalized: String, encoded: String): List<String> = when {
    HEX_REGEX.matches(normalized) -> listOf(
      "$adsbLolBaseUrl/v2/hex/${normalized.lowercase(Locale.US)}",
      "$adsbFiBaseUrl/v2/hex/${normalized.lowercase(Locale.US)}",
      "$adsbLolBaseUrl/v2/callsign/$encoded",
    )
    SQUAWK_REGEX.matches(normalized) -> listOf(
      "$adsbLolBaseUrl/v2/squawk/$encoded",
    )
    normalized.contains('-') -> listOf(
      "$adsbLolBaseUrl/v2/reg/$encoded",
      "$adsbLolBaseUrl/v2/callsign/$encoded",
    )
    FlightHubs.isWidebodyType(normalized) || FlightHubs.isMilitaryType(normalized) -> listOf(
      "$adsbLolBaseUrl/v2/type/$encoded",
      "$adsbLolBaseUrl/v2/callsign/$encoded",
    )
    else -> listOf(
      "$adsbLolBaseUrl/v2/callsign/$encoded",
      "$adsbLolBaseUrl/v2/reg/$encoded",
      "$adsbLolBaseUrl/v2/type/$encoded",
    )
  }

  override suspend fun fetchEnrichment(hex: String, callsign: String?): FlightEnrichment =
    withContext(ioDispatcher) {
      val cleanHex = hex.trim().lowercase(Locale.US)
      val cleanCallsign = callsign?.trim()?.uppercase(Locale.US)?.takeIf { it.isNotBlank() }

      coroutineScope {
        val routeDeferred = async(ioDispatcher) {
          if (cleanCallsign == null) {
            null
          } else {
            routeCache[cleanCallsign]?.value ?: run {
              val fetched = runCatching {
                val encoded = URLEncoder.encode(cleanCallsign, Charsets.UTF_8.name())
                AdsbJsonParser.parseAdsbdbRoute(executeGet("$adsbdbBaseUrl/v0/callsign/$encoded"))
              }.getOrNull()
              routeCache[cleanCallsign] = CachedOptional(fetched)
              fetched
            }
          }
        }

        val metadataDeferred = async(ioDispatcher) {
          metadataCache[cleanHex]?.value ?: run {
            val fetched = runCatching {
              val encoded = URLEncoder.encode(cleanHex, Charsets.UTF_8.name())
              AdsbJsonParser.parseAdsbdbAircraft(executeGet("$adsbdbBaseUrl/v0/aircraft/$encoded"))
            }.getOrNull()
            metadataCache[cleanHex] = CachedOptional(fetched)
            fetched
          }
        }

        FlightEnrichment(
          hex = cleanHex,
          route = routeDeferred.await(),
          aircraftMetadata = metadataDeferred.await(),
          isLoading = false,
        )
      }
    }

  private fun executeGet(url: String): String {
    val request = Request.Builder()
      .url(url)
      .header("User-Agent", USER_AGENT)
      .header("Accept", "application/json")
      .get()
      .build()

    httpClient.newCall(request).execute().use { response ->
      if (!response.isSuccessful) {
        throw IOException("HTTP ${response.code} for $url")
      }
      return response.body?.string() ?: throw IOException("Empty response body for $url")
    }
  }

  private data class CachedOptional<T>(val value: T?)

  companion object {
    const val USER_AGENT =
      "SkyWatch-Googlebook/1.0 (Android 17; +https://github.com/zakariaalnatour/skywatch-googlebook)"

    private val HEX_REGEX = Regex("^[0-9A-F]{6}$")
    private val SQUAWK_REGEX = Regex("^[0-7]{4}$")

    fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
      .connectTimeout(8, TimeUnit.SECONDS)
      .readTimeout(12, TimeUnit.SECONDS)
      .callTimeout(15, TimeUnit.SECONDS)
      .retryOnConnectionFailure(true)
      .build()
  }
}
