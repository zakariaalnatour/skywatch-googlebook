package com.zakariaalnatour.skywatch.model

import kotlin.math.roundToInt

enum class HubGroup(val label: String) {
  MIDDLE_EAST("Middle East"),
  GLOBAL("Global"),
  SPECIAL("Feeds"),
}

enum class HubFeedMode {
  REGIONAL,
  WIDEBODY,
  MILITARY,
}

data class FlightHub(
  val id: String,
  val name: String,
  val icaoCode: String,
  val country: String,
  val latitude: Double,
  val longitude: Double,
  val defaultZoom: Float,
  val radiusNm: Int,
  val group: HubGroup,
  val feedMode: HubFeedMode = HubFeedMode.REGIONAL,
  val shortcutDigit: Int? = null,
)

data class GeoPoint(
  val latitude: Double,
  val longitude: Double,
)

data class TrackPoint(
  val latitude: Double,
  val longitude: Double,
  val altitudeFt: Int?,
  val timestampMs: Long,
)

enum class AircraftCategoryClass {
  WIDEBODY,
  NARROWBODY,
  REGIONAL_OR_LIGHT,
  HELICOPTER,
  MILITARY,
  UNKNOWN,
}

data class Aircraft(
  val hex: String,
  val callsign: String?,
  val registration: String?,
  val aircraftType: String?,
  val description: String?,
  val latitude: Double,
  val longitude: Double,
  val baroAltitudeFt: Int?,
  val geomAltitudeFt: Int?,
  val onGround: Boolean,
  val groundSpeedKt: Double?,
  val iasKt: Int?,
  val tasKt: Int?,
  val mach: Double?,
  val trackDeg: Double?,
  val magHeadingDeg: Double?,
  val baroRateFpm: Int?,
  val squawk: String?,
  val emergency: String?,
  val category: String?,
  val dbFlags: Int,
  val seenSec: Double?,
  val rssiDb: Double?,
  val navAltitudeMcpFt: Int?,
  val navQnhHpa: Double?,
  val distanceNm: Double?,
) {
  val displayCallsign: String
    get() = callsign?.takeIf { it.isNotBlank() }
      ?: registration?.takeIf { it.isNotBlank() }
      ?: hex.uppercase()

  val isMilitary: Boolean
    get() = (dbFlags and 1) != 0 || FlightHubs.isMilitaryType(aircraftType)

  val isWidebody: Boolean
    get() = FlightHubs.isWidebodyType(aircraftType)

  val isEmergency: Boolean
    get() = squawk in setOf("7500", "7600", "7700") ||
      (!emergency.isNullOrBlank() && !emergency.equals("none", ignoreCase = true))

  val effectiveAltitudeFt: Int?
    get() = if (onGround) 0 else (baroAltitudeFt ?: geomAltitudeFt)

  val effectiveHeadingDeg: Double
    get() = trackDeg ?: magHeadingDeg ?: 0.0

  val visualClass: AircraftCategoryClass
    get() = when {
      isMilitary -> AircraftCategoryClass.MILITARY
      category == "A7" || FlightHubs.isRotorcraftType(aircraftType) -> AircraftCategoryClass.HELICOPTER
      isWidebody || category == "A5" -> AircraftCategoryClass.WIDEBODY
      category in setOf("A3", "A4") -> AircraftCategoryClass.NARROWBODY
      category in setOf("A1", "A2") -> AircraftCategoryClass.REGIONAL_OR_LIGHT
      else -> AircraftCategoryClass.UNKNOWN
    }

  val formattedAltitude: String
    get() = when {
      onGround -> "GND"
      effectiveAltitudeFt == null -> "—"
      effectiveAltitudeFt!! >= 18_000 -> "FL${(effectiveAltitudeFt!! / 100.0).roundToInt().toString().padStart(3, '0')}"
      else -> "${"%,d".format(effectiveAltitudeFt)} ft"
    }

  val formattedSpeed: String
    get() = groundSpeedKt?.let { "${it.roundToInt()} kt" } ?: "—"

  val formattedVerticalRate: String
    get() = when {
      baroRateFpm == null -> "—"
      baroRateFpm > 64 -> "+${baroRateFpm} fpm"
      baroRateFpm < -64 -> "$baroRateFpm fpm"
      else -> "LEVEL"
    }
}

data class AirportInfo(
  val iataCode: String?,
  val icaoCode: String?,
  val name: String,
  val municipality: String?,
  val countryName: String?,
  val countryIso: String?,
  val latitude: Double?,
  val longitude: Double?,
) {
  val codeLabel: String
    get() = iataCode?.takeIf { it.isNotBlank() }
      ?: icaoCode?.takeIf { it.isNotBlank() }
      ?: "—"

  val geoPoint: GeoPoint?
    get() = if (latitude != null && longitude != null) GeoPoint(latitude, longitude) else null
}

data class AirlineInfo(
  val name: String,
  val iata: String?,
  val icao: String?,
  val country: String?,
  val countryIso: String?,
)

data class FlightRoute(
  val callsign: String,
  val callsignIata: String?,
  val airline: AirlineInfo?,
  val origin: AirportInfo?,
  val destination: AirportInfo?,
)

data class AircraftMetadata(
  val modeS: String,
  val registration: String?,
  val manufacturer: String?,
  val typeDescription: String?,
  val icaoType: String?,
  val registeredOwner: String?,
  val countryName: String?,
  val countryIso: String?,
  val photoUrl: String?,
  val thumbnailUrl: String?,
)

data class FlightEnrichment(
  val hex: String,
  val route: FlightRoute? = null,
  val aircraftMetadata: AircraftMetadata? = null,
  val isLoading: Boolean = false,
  val errorMessage: String? = null,
)

enum class FeedSource(val displayName: String, val badgeText: String) {
  ADSB_FI("opendata.adsb.fi", "adsb.fi"),
  ADSB_LOL_FAILOVER("api.adsb.lol (failover)", "adsb.lol failover"),
  ADSB_LOL_MIL("api.adsb.lol/v2/mil", "adsb.lol MIL"),
  ADSB_LOL_WIDEBODY("api.adsb.lol (widebody)", "adsb.lol HEAVY"),
  ADSB_LOL_SEARCH("api.adsb.lol (global)", "adsb.lol global"),
}

data class FlightFeedResult(
  val aircraft: List<Aircraft>,
  val source: FeedSource,
  val latencyMs: Long,
  val timestampMs: Long,
  val failoverActive: Boolean = false,
  val note: String? = null,
)

enum class AircraftFilter(val label: String) {
  ALL("All"),
  WIDEBODY("Widebody"),
  MILITARY("Military"),
  AIRBORNE("Airborne"),
  EMERGENCY("Alert"),
}

enum class FlightSortOption(val label: String) {
  ALTITUDE_DESC("Alt"),
  SPEED_DESC("Speed"),
  CALLSIGN_ASC("Callsign"),
  DISTANCE_ASC("Dist"),
}

enum class MapTileLayer(val id: String, val label: String, val attribution: String) {
  ESRI_DARK_GRAY(
    id = "esri_dark",
    label = "Esri Dark Gray",
    attribution = "Tiles © Esri — World Dark Gray Base + Reference",
  ),
  OPEN_STREET_MAP(
    id = "osm",
    label = "OpenStreetMap",
    attribution = "© OpenStreetMap contributors",
  ),
  ;

  companion object {
    fun fromId(id: String?): MapTileLayer =
      entries.firstOrNull { it.id == id } ?: ESRI_DARK_GRAY
  }
}
