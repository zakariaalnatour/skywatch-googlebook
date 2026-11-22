package com.zakariaalnatour.skywatch.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.zakariaalnatour.skywatch.geo.GeoProjection
import com.zakariaalnatour.skywatch.model.MapTileLayer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class CompositeTileBitmaps(
  val base: Bitmap,
  val referenceOverlay: Bitmap? = null,
  val usedFallbackOsm: Boolean = false,
)

/**
 * Asynchronous keyless tile loader & two-tier cache (in-memory LRU + app cache dir)
 * supporting Esri World Dark Gray Base + Reference (`{z}/{y}/{x}`) with automatic
 * per-tile OpenStreetMap (`{z}/{x}/{y}`) failover.
 */
class MapTileLoader(
  cacheDir: File,
  private val httpClient: OkHttpClient = LiveFlightRepository.defaultHttpClient(),
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
  private val tileCacheDir = File(cacheDir, "skywatch_tiles").apply { mkdirs() }
  private val inFlight = ConcurrentHashMap.newKeySet<String>()

  // ~32 MB memory LRU cache for crisp zero-flicker panning/zooming on Googlebook displays
  private val memoryCache = object : LruCache<String, CompositeTileBitmaps>(32 * 1024) {
    override fun sizeOf(key: String, value: CompositeTileBitmaps): Int {
      val baseKb = value.base.allocationByteCount / 1024
      val overlayKb = (value.referenceOverlay?.allocationByteCount ?: 0) / 1024
      return (baseKb + overlayKb).coerceAtLeast(1)
    }
  }

  fun cacheKey(layer: MapTileLayer, z: Int, x: Int, y: Int): String = "${layer.id}_${z}_${x}_${y}"

  fun getCached(layer: MapTileLayer, z: Int, x: Int, y: Int): CompositeTileBitmaps? =
    memoryCache.get(cacheKey(layer, z, x, y))

  suspend fun loadTile(
    layer: MapTileLayer,
    z: Int,
    x: Int,
    y: Int,
  ): CompositeTileBitmaps? {
    val key = cacheKey(layer, z, x, y)
    memoryCache.get(key)?.let { return it }
    if (!inFlight.add(key)) return null

    return try {
      withContext(ioDispatcher) {
        val loaded = when (layer) {
          MapTileLayer.ESRI_DARK_GRAY -> loadEsriWithOsmFallback(z, x, y)
          MapTileLayer.OPEN_STREET_MAP -> loadOsmOnly(z, x, y)
        }
        if (loaded != null) {
          memoryCache.put(key, loaded)
        }
        loaded
      }
    } finally {
      inFlight.remove(key)
    }
  }

  private fun loadEsriWithOsmFallback(z: Int, x: Int, y: Int): CompositeTileBitmaps? {
    val baseBitmap = fetchOrReadDisk(
      diskName = "esri_base_${z}_${y}_${x}.jpg",
      url = GeoProjection.esriDarkGrayBaseUrl(z, x, y),
    )
    if (baseBitmap != null) {
      val refBitmap = fetchOrReadDisk(
        diskName = "esri_ref_${z}_${y}_${x}.png",
        url = GeoProjection.esriDarkGrayReferenceUrl(z, x, y),
      )
      return CompositeTileBitmaps(
        base = baseBitmap,
        referenceOverlay = refBitmap,
        usedFallbackOsm = false,
      )
    }

    // Automatic fallback to OpenStreetMap if Esri fails
    val osmBitmap = fetchOrReadDisk(
      diskName = "osm_${z}_${x}_${y}.png",
      url = GeoProjection.openStreetMapUrl(z, x, y),
    ) ?: return null

    return CompositeTileBitmaps(
      base = osmBitmap,
      referenceOverlay = null,
      usedFallbackOsm = true,
    )
  }

  private fun loadOsmOnly(z: Int, x: Int, y: Int): CompositeTileBitmaps? {
    val osmBitmap = fetchOrReadDisk(
      diskName = "osm_${z}_${x}_${y}.png",
      url = GeoProjection.openStreetMapUrl(z, x, y),
    ) ?: return null
    return CompositeTileBitmaps(base = osmBitmap, referenceOverlay = null, usedFallbackOsm = false)
  }

  private fun fetchOrReadDisk(diskName: String, url: String): Bitmap? {
    val diskFile = File(tileCacheDir, diskName)
    if (diskFile.exists() && diskFile.length() > 0L) {
      val decoded = runCatching { BitmapFactory.decodeFile(diskFile.absolutePath) }.getOrNull()
      if (decoded != null) return decoded
    }

    val bytes = runCatching {
      val req = Request.Builder()
        .url(url)
        .header("User-Agent", LiveFlightRepository.USER_AGENT)
        .get()
        .build()
      httpClient.newCall(req).execute().use { resp ->
        if (!resp.isSuccessful) return@use null
        resp.body?.bytes()
      }
    }.getOrNull() ?: return null

    val bitmap = runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()
      ?: return null

    runCatching { diskFile.writeBytes(bytes) }
    return bitmap
  }

  companion object {
    @Volatile
    private var sharedInstance: MapTileLoader? = null

    fun getInstance(context: Context): MapTileLoader =
      sharedInstance ?: synchronized(this) {
        sharedInstance ?: MapTileLoader(context.applicationContext.cacheDir).also {
          sharedInstance = it
        }
      }
  }
}
