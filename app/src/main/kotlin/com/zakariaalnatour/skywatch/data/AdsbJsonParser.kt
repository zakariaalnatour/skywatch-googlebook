package com.zakariaalnatour.skywatch.data

import com.zakariaalnatour.skywatch.model.Aircraft
import com.zakariaalnatour.skywatch.model.AircraftMetadata
import com.zakariaalnatour.skywatch.model.AirlineInfo
import com.zakariaalnatour.skywatch.model.AirportInfo
import com.zakariaalnatour.skywatch.model.FlightRoute
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlin.math.roundToInt

/**
 * Fault-tolerant JSON parser for:
 * - `opendata.adsb.fi` (`"aircraft"` array)
 * - `api.adsb.lol` (`"ac"` array)
 * - `api.adsbdb.com/v0/callsign/{callsign}` (object OR `"unknown callsign"` string)
 * - `api.adsbdb.com/v0/aircraft/{hex}` (object OR `"unknown aircraft"` string)
 */
object AdsbJsonParser {
  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
  }

  fun parseAircraftList(rawJson: String): List<Aircraft> {
    if (rawJson.isBlank()) return emptyList()
    val root = runCatching { json.parseToJsonElement(rawJson) }.getOrNull() as? JsonObject
      ?: return emptyList()

    val array: JsonArray = (root["aircraft"] as? JsonArray)
      ?: (root["ac"] as? JsonArray)
      ?: return emptyList()

    val result = ArrayList<Aircraft>(array.size)
    for (element in array) {
      val obj = element as? JsonObject ?: continue
      val hex = obj.stringOrNull("hex")?.trim()?.lowercase()?.takeIf { it.isNotBlank() } ?: continue
      val lat = obj.doubleOrNull("lat") ?: continue
      val lon = obj.doubleOrNull("lon") ?: continue
      if (!lat.isFinite() || !lon.isFinite() || lat !in -90.0..90.0 || lon !in -180.0..180.0) {
        continue
      }

      val rawBaro = obj["alt_baro"]
      var onGround = false
      var baroAltFt: Int? = null
      if (rawBaro is JsonPrimitive) {
        val content = rawBaro.content.trim()
        if (content.equals("ground", ignoreCase = true)) {
          onGround = true
          baroAltFt = 0
        } else {
          baroAltFt = content.toDoubleOrNull()?.roundToInt()
        }
      }

      val geomAltFt = obj.doubleOrNull("alt_geom")?.roundToInt()

      result += Aircraft(
        hex = hex,
        callsign = obj.stringOrNull("flight")?.trim()?.takeIf { it.isNotBlank() },
        registration = obj.stringOrNull("r")?.trim()?.takeIf { it.isNotBlank() },
        aircraftType = obj.stringOrNull("t")?.trim()?.uppercase()?.takeIf { it.isNotBlank() },
        description = obj.stringOrNull("desc")?.trim()?.takeIf { it.isNotBlank() },
        latitude = lat,
        longitude = lon,
        baroAltitudeFt = baroAltFt,
        geomAltitudeFt = geomAltFt,
        onGround = onGround,
        groundSpeedKt = obj.doubleOrNull("gs"),
        iasKt = obj.doubleOrNull("ias")?.roundToInt(),
        tasKt = obj.doubleOrNull("tas")?.roundToInt(),
        mach = obj.doubleOrNull("mach"),
        trackDeg = obj.doubleOrNull("track"),
        magHeadingDeg = obj.doubleOrNull("mag_heading") ?: obj.doubleOrNull("true_heading"),
        baroRateFpm = obj.doubleOrNull("baro_rate")?.roundToInt()
          ?: obj.doubleOrNull("geom_rate")?.roundToInt(),
        squawk = obj.stringOrNull("squawk")?.trim()?.takeIf { it.isNotBlank() },
        emergency = obj.stringOrNull("emergency")?.trim()?.takeIf { it.isNotBlank() },
        category = obj.stringOrNull("category")?.trim()?.uppercase()?.takeIf { it.isNotBlank() },
        dbFlags = obj.intOrNull("dbFlags") ?: 0,
        seenSec = obj.doubleOrNull("seen") ?: obj.doubleOrNull("seen_pos"),
        rssiDb = obj.doubleOrNull("rssi"),
        navAltitudeMcpFt = obj.doubleOrNull("nav_altitude_mcp")?.roundToInt(),
        navQnhHpa = obj.doubleOrNull("nav_qnh"),
        distanceNm = obj.doubleOrNull("dst"),
      )
    }
    return result
  }

  fun parseAdsbdbRoute(rawJson: String): FlightRoute? {
    if (rawJson.isBlank()) return null
    val root = runCatching { json.parseToJsonElement(rawJson) }.getOrNull() as? JsonObject
      ?: return null
    val responseObj = root["response"] as? JsonObject ?: return null
    val routeObj = responseObj["flightroute"] as? JsonObject ?: return null

    val callsign = routeObj.stringOrNull("callsign")?.trim().orEmpty()
    val airlineObj = routeObj["airline"] as? JsonObject
    val airline = airlineObj?.stringOrNull("name")?.let { name ->
      AirlineInfo(
        name = name,
        iata = airlineObj.stringOrNull("iata"),
        icao = airlineObj.stringOrNull("icao"),
        country = airlineObj.stringOrNull("country"),
        countryIso = airlineObj.stringOrNull("country_iso"),
      )
    }

    val origin = (routeObj["origin"] as? JsonObject)?.toAirportInfo()
    val destination = (routeObj["destination"] as? JsonObject)?.toAirportInfo()

    if (origin == null && destination == null && airline == null) return null

    return FlightRoute(
      callsign = callsign,
      callsignIata = routeObj.stringOrNull("callsign_iata"),
      airline = airline,
      origin = origin,
      destination = destination,
    )
  }

  fun parseAdsbdbAircraft(rawJson: String): AircraftMetadata? {
    if (rawJson.isBlank()) return null
    val root = runCatching { json.parseToJsonElement(rawJson) }.getOrNull() as? JsonObject
      ?: return null
    val responseObj = root["response"] as? JsonObject ?: return null
    val acObj = responseObj["aircraft"] as? JsonObject ?: return null

    val modeS = acObj.stringOrNull("mode_s")?.trim()?.lowercase() ?: return null
    return AircraftMetadata(
      modeS = modeS,
      registration = acObj.stringOrNull("registration"),
      manufacturer = acObj.stringOrNull("manufacturer"),
      typeDescription = acObj.stringOrNull("type"),
      icaoType = acObj.stringOrNull("icao_type"),
      registeredOwner = acObj.stringOrNull("registered_owner"),
      countryName = acObj.stringOrNull("registered_owner_country_name"),
      countryIso = acObj.stringOrNull("registered_owner_country_iso_name"),
      photoUrl = sanitizeHttpsUrl(acObj.stringOrNull("url_photo")),
      thumbnailUrl = sanitizeHttpsUrl(acObj.stringOrNull("url_photo_thumbnail")),
    )
  }

  private fun JsonObject.toAirportInfo(): AirportInfo? {
    val name = stringOrNull("name") ?: return null
    return AirportInfo(
      iataCode = stringOrNull("iata_code"),
      icaoCode = stringOrNull("icao_code"),
      name = name,
      municipality = stringOrNull("municipality"),
      countryName = stringOrNull("country_name"),
      countryIso = stringOrNull("country_iso_name"),
      latitude = doubleOrNull("latitude"),
      longitude = doubleOrNull("longitude"),
    )
  }

  private fun sanitizeHttpsUrl(raw: String?): String? {
    val trimmed = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return when {
      trimmed.startsWith("https://", ignoreCase = true) -> trimmed
      trimmed.startsWith("http://", ignoreCase = true) -> "https://" + trimmed.removePrefix("http://")
      else -> null
    }
  }

  private fun JsonObject.stringOrNull(key: String): String? {
    val element: JsonElement = this[key] ?: return null
    if (element is JsonNull) return null
    return (element as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() && it != "null" }
  }

  private fun JsonObject.doubleOrNull(key: String): Double? {
    val element: JsonElement = this[key] ?: return null
    return (element as? JsonPrimitive)?.doubleOrNull
  }

  private fun JsonObject.intOrNull(key: String): Int? {
    val element: JsonElement = this[key] ?: return null
    return (element as? JsonPrimitive)?.intOrNull
  }
}
