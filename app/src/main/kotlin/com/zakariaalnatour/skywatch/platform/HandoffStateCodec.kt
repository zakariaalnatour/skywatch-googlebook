package com.zakariaalnatour.skywatch.platform

import android.os.Bundle
import android.os.PersistableBundle
import com.zakariaalnatour.skywatch.model.AircraftFilter
import com.zakariaalnatour.skywatch.model.FlightHubs
import com.zakariaalnatour.skywatch.model.MapTileLayer
import java.util.Locale

data class SkyWatchHandoffState(
  val hubId: String = FlightHubs.DEFAULT.id,
  val selectedHex: String? = null,
  val centerLat: Double = FlightHubs.DEFAULT.latitude,
  val centerLon: Double = FlightHubs.DEFAULT.longitude,
  val zoom: Float = FlightHubs.DEFAULT.defaultZoom,
  val searchQuery: String = "",
  val filter: AircraftFilter = AircraftFilter.ALL,
  val tileLayer: MapTileLayer = MapTileLayer.ESRI_DARK_GRAY,
  val showTrails: Boolean = true,
  val showGreatCircle: Boolean = true,
  val followSelected: Boolean = false,
)

/**
 * Encodes and decodes SkyWatch workspace state for Android 17 Continue On (`HandoffActivityData`),
 * multi-instance window launches, and App Widget deep-links.
 */
object HandoffStateCodec {
  const val KEY_HUB_ID = "skywatch_hub_id"
  const val KEY_SELECTED_HEX = "skywatch_selected_hex"
  const val KEY_CENTER_LAT = "skywatch_center_lat"
  const val KEY_CENTER_LON = "skywatch_center_lon"
  const val KEY_ZOOM = "skywatch_zoom"
  const val KEY_SEARCH_QUERY = "skywatch_search_query"
  const val KEY_FILTER = "skywatch_filter"
  const val KEY_TILE_LAYER = "skywatch_tile_layer"
  const val KEY_SHOW_TRAILS = "skywatch_show_trails"
  const val KEY_SHOW_GREAT_CIRCLE = "skywatch_show_great_circle"
  const val KEY_FOLLOW_SELECTED = "skywatch_follow_selected"

  fun toStringMap(state: SkyWatchHandoffState): Map<String, String> = buildMap {
    put(KEY_HUB_ID, state.hubId)
    state.selectedHex?.takeIf { it.isNotBlank() }?.let { put(KEY_SELECTED_HEX, it.lowercase()) }
    put(KEY_CENTER_LAT, String.format(Locale.US, "%.5f", state.centerLat))
    put(KEY_CENTER_LON, String.format(Locale.US, "%.5f", state.centerLon))
    put(KEY_ZOOM, String.format(Locale.US, "%.2f", state.zoom))
    if (state.searchQuery.isNotBlank()) put(KEY_SEARCH_QUERY, state.searchQuery)
    put(KEY_FILTER, state.filter.name)
    put(KEY_TILE_LAYER, state.tileLayer.id)
    put(KEY_SHOW_TRAILS, state.showTrails.toString())
    put(KEY_SHOW_GREAT_CIRCLE, state.showGreatCircle.toString())
    put(KEY_FOLLOW_SELECTED, state.followSelected.toString())
  }

  fun fromStringMap(map: Map<String, String?>): SkyWatchHandoffState? {
    val hubIdRaw = map[KEY_HUB_ID]
    val selectedHex = map[KEY_SELECTED_HEX]?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
    val lat = map[KEY_CENTER_LAT]?.toDoubleOrNull()
    val lon = map[KEY_CENTER_LON]?.toDoubleOrNull()
    val zoom = map[KEY_ZOOM]?.toFloatOrNull()
    if (hubIdRaw.isNullOrBlank() && selectedHex == null && lat == null && lon == null) {
      return null
    }
    val hub = FlightHubs.findById(hubIdRaw) ?: FlightHubs.DEFAULT
    val filter = map[KEY_FILTER]?.let { name ->
      AircraftFilter.entries.firstOrNull { it.name == name }
    } ?: AircraftFilter.ALL
    return SkyWatchHandoffState(
      hubId = hub.id,
      selectedHex = selectedHex,
      centerLat = (lat ?: hub.latitude).coerceIn(-85.0, 85.0),
      centerLon = (lon ?: hub.longitude).coerceIn(-180.0, 180.0),
      zoom = (zoom ?: hub.defaultZoom).coerceIn(2.0f, 14.0f),
      searchQuery = map[KEY_SEARCH_QUERY].orEmpty(),
      filter = filter,
      tileLayer = MapTileLayer.fromId(map[KEY_TILE_LAYER]),
      showTrails = map[KEY_SHOW_TRAILS]?.toBooleanStrictOrNull() ?: true,
      showGreatCircle = map[KEY_SHOW_GREAT_CIRCLE]?.toBooleanStrictOrNull() ?: true,
      followSelected = map[KEY_FOLLOW_SELECTED]?.toBooleanStrictOrNull() ?: false,
    )
  }

  fun toPersistableBundle(state: SkyWatchHandoffState): PersistableBundle =
    PersistableBundle().apply {
      for ((key, value) in toStringMap(state)) {
        putString(key, value)
      }
    }

  fun toBundle(state: SkyWatchHandoffState): Bundle =
    Bundle().apply {
      for ((key, value) in toStringMap(state)) {
        putString(key, value)
      }
    }

  fun fromBundle(bundle: Bundle?): SkyWatchHandoffState? {
    if (bundle == null || bundle.isEmpty) return null
    val keys = listOf(
      KEY_HUB_ID,
      KEY_SELECTED_HEX,
      KEY_CENTER_LAT,
      KEY_CENTER_LON,
      KEY_ZOOM,
      KEY_SEARCH_QUERY,
      KEY_FILTER,
      KEY_TILE_LAYER,
      KEY_SHOW_TRAILS,
      KEY_SHOW_GREAT_CIRCLE,
      KEY_FOLLOW_SELECTED,
    )
    val map = keys.associateWith { bundle.getString(it) }
    return fromStringMap(map)
  }

  /**
   * Builds a live web fallback URI for Android 17 Continue On when the receiving device
   * does not have SkyWatch installed.
   */
  fun buildFallbackUriString(state: SkyWatchHandoffState): String {
    val hex = state.selectedHex?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
    return if (hex != null) {
      String.format(
        Locale.US,
        "https://globe.adsb.fi/?icao=%s&lat=%.4f&lon=%.4f&zoom=%.1f",
        hex,
        state.centerLat,
        state.centerLon,
        state.zoom,
      )
    } else {
      String.format(
        Locale.US,
        "https://globe.adsb.fi/?lat=%.4f&lon=%.4f&zoom=%.1f",
        state.centerLat,
        state.centerLon,
        state.zoom,
      )
    }
  }
}
