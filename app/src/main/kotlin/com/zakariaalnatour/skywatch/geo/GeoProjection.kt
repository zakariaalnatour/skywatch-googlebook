package com.zakariaalnatour.skywatch.geo

import com.zakariaalnatour.skywatch.model.GeoPoint
import com.zakariaalnatour.skywatch.model.MapTileLayer
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sinh
import kotlin.math.tan

data class WorldPoint(val x: Double, val y: Double)
data class ScreenPoint(val x: Float, val y: Float)

data class MapTileCoordinate(
  val z: Int,
  val x: Int,
  val y: Int,
  val screenLeft: Float,
  val screenTop: Float,
  val tileDrawSizePx: Float,
)

data class ViewportState(
  val centerLat: Double,
  val centerLon: Double,
  val zoom: Float,
)

/**
 * Web Mercator slippy-map projection and keyless tile URL builder for Esri World Dark Gray
 * Base + Reference (`{z}/{y}/{x}`) with OpenStreetMap (`{z}/{x}/{y}`) fallback.
 */
object GeoProjection {
  const val TILE_SIZE_PX = 256.0
  const val MIN_LAT = -85.05112878
  const val MAX_LAT = 85.05112878
  const val MIN_ZOOM = 2.0f
  const val MAX_ZOOM = 14.0f

  fun clampLat(lat: Double): Double = lat.coerceIn(MIN_LAT, MAX_LAT)

  fun wrapLon(lon: Double): Double {
    var wrapped = ((lon + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
    if (wrapped == -180.0 && lon > 0.0) wrapped = 180.0
    return wrapped
  }

  fun clampZoom(zoom: Float): Float = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)

  fun worldSizePx(zoom: Float): Double = TILE_SIZE_PX * 2.0.pow(zoom.toDouble())

  fun latLonToWorld(lat: Double, lon: Double, zoom: Float): WorldPoint {
    val clampedLat = clampLat(lat)
    val scale = worldSizePx(zoom)
    val x = (wrapLon(lon) + 180.0) / 360.0 * scale
    val latRad = Math.toRadians(clampedLat)
    val mercY = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0
    val y = mercY * scale
    return WorldPoint(x, y)
  }

  fun worldToLatLon(worldX: Double, worldY: Double, zoom: Float): GeoPoint {
    val scale = worldSizePx(zoom)
    val lon = wrapLon((worldX / scale) * 360.0 - 180.0)
    val n = PI - 2.0 * PI * (worldY / scale)
    val lat = clampLat(Math.toDegrees(atan(sinh(n))))
    return GeoPoint(lat, lon)
  }

  fun latLonToScreen(
    lat: Double,
    lon: Double,
    viewport: ViewportState,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
  ): ScreenPoint {
    val worldScale = worldSizePx(viewport.zoom)
    val centerWorld = latLonToWorld(viewport.centerLat, viewport.centerLon, viewport.zoom)
    val targetWorld = latLonToWorld(lat, lon, viewport.zoom)

    // Pick shortest horizontal wrap across the antimeridian relative to viewport center
    var dx = targetWorld.x - centerWorld.x
    if (dx > worldScale / 2.0) dx -= worldScale
    if (dx < -worldScale / 2.0) dx += worldScale

    val dy = targetWorld.y - centerWorld.y
    return ScreenPoint(
      x = (viewportWidthPx / 2.0 + dx).toFloat(),
      y = (viewportHeightPx / 2.0 + dy).toFloat(),
    )
  }

  fun screenToLatLon(
    screenX: Float,
    screenY: Float,
    viewport: ViewportState,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
  ): GeoPoint {
    val centerWorld = latLonToWorld(viewport.centerLat, viewport.centerLon, viewport.zoom)
    val worldX = centerWorld.x + (screenX - viewportWidthPx / 2.0)
    val worldY = centerWorld.y + (screenY - viewportHeightPx / 2.0)
    return worldToLatLon(worldX, worldY, viewport.zoom)
  }

  fun panByScreenDelta(
    viewport: ViewportState,
    dxPx: Float,
    dyPx: Float,
  ): ViewportState {
    val worldScale = worldSizePx(viewport.zoom)
    val centerWorld = latLonToWorld(viewport.centerLat, viewport.centerLon, viewport.zoom)
    val nextWorldX = centerWorld.x - dxPx
    val nextWorldY = (centerWorld.y - dyPx).coerceIn(0.0, worldScale)
    val nextGeo = worldToLatLon(nextWorldX, nextWorldY, viewport.zoom)
    return viewport.copy(centerLat = nextGeo.latitude, centerLon = nextGeo.longitude)
  }

  fun zoomAroundScreenPoint(
    viewport: ViewportState,
    newZoomRaw: Float,
    anchorScreenX: Float,
    anchorScreenY: Float,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
  ): ViewportState {
    val newZoom = clampZoom(newZoomRaw)
    if (newZoom == viewport.zoom) return viewport
    val anchorGeo = screenToLatLon(anchorScreenX, anchorScreenY, viewport, viewportWidthPx, viewportHeightPx)
    val anchorWorldNew = latLonToWorld(anchorGeo.latitude, anchorGeo.longitude, newZoom)
    val newCenterWorldX = anchorWorldNew.x - (anchorScreenX - viewportWidthPx / 2.0)
    val newCenterWorldY = (anchorWorldNew.y - (anchorScreenY - viewportHeightPx / 2.0))
      .coerceIn(0.0, worldSizePx(newZoom))
    val newCenter = worldToLatLon(newCenterWorldX, newCenterWorldY, newZoom)
    return ViewportState(
      centerLat = newCenter.latitude,
      centerLon = newCenter.longitude,
      zoom = newZoom,
    )
  }

  fun visibleTiles(
    viewport: ViewportState,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
  ): List<MapTileCoordinate> {
    if (viewportWidthPx <= 0f || viewportHeightPx <= 0f) return emptyList()

    val tileZ = floor(viewport.zoom.toDouble()).toInt().coerceIn(2, 14)
    val tileCount = 1 shl tileZ
    val scaleFactor = 2.0.pow((viewport.zoom - tileZ).toDouble())
    val drawTileSize = TILE_SIZE_PX * scaleFactor

    val centerWorld = latLonToWorld(viewport.centerLat, viewport.centerLon, viewport.zoom)
    val leftWorld = centerWorld.x - viewportWidthPx / 2.0
    val topWorld = centerWorld.y - viewportHeightPx / 2.0
    val rightWorld = centerWorld.x + viewportWidthPx / 2.0
    val bottomWorld = centerWorld.y + viewportHeightPx / 2.0

    val minTileX = floor(leftWorld / drawTileSize).toInt()
    val maxTileX = ceil(rightWorld / drawTileSize).toInt() - 1
    val minTileY = floor(topWorld / drawTileSize).toInt().coerceAtLeast(0)
    val maxTileY = (ceil(bottomWorld / drawTileSize).toInt() - 1).coerceAtMost(tileCount - 1)

    val tiles = ArrayList<MapTileCoordinate>()
    for (ty in minTileY..maxTileY) {
      for (tx in minTileX..maxTileX) {
        val wrappedX = ((tx % tileCount) + tileCount) % tileCount
        val screenLeft = (tx * drawTileSize - leftWorld).toFloat()
        val screenTop = (ty * drawTileSize - topWorld).toFloat()
        tiles += MapTileCoordinate(
          z = tileZ,
          x = wrappedX,
          y = ty,
          screenLeft = screenLeft,
          screenTop = screenTop,
          tileDrawSizePx = drawTileSize.toFloat(),
        )
      }
    }
    return tiles
  }

  /** Esri World Dark Gray Base tile URL (`{z}/{y}/{x}` order). */
  fun esriDarkGrayBaseUrl(z: Int, x: Int, y: Int): String =
    "https://server.arcgisonline.com/arcgis/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/$z/$y/$x"

  /** Esri World Dark Gray Reference labels overlay tile URL (`{z}/{y}/{x}` order). */
  fun esriDarkGrayReferenceUrl(z: Int, x: Int, y: Int): String =
    "https://server.arcgisonline.com/arcgis/rest/services/Canvas/World_Dark_Gray_Reference/MapServer/tile/$z/$y/$x"

  /** OpenStreetMap standard raster tile fallback URL (`{z}/{x}/{y}` order). */
  fun openStreetMapUrl(z: Int, x: Int, y: Int): String =
    "https://tile.openstreetmap.org/$z/$x/$y.png"

  fun primaryTileUrl(layer: MapTileLayer, z: Int, x: Int, y: Int): String = when (layer) {
    MapTileLayer.ESRI_DARK_GRAY -> esriDarkGrayBaseUrl(z, x, y)
    MapTileLayer.OPEN_STREET_MAP -> openStreetMapUrl(z, x, y)
  }

  fun overlayTileUrl(layer: MapTileLayer, z: Int, x: Int, y: Int): String? = when (layer) {
    MapTileLayer.ESRI_DARK_GRAY -> esriDarkGrayReferenceUrl(z, x, y)
    MapTileLayer.OPEN_STREET_MAP -> null
  }
}
