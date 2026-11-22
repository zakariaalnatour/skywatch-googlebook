package com.zakariaalnatour.skywatch.data

import android.content.Context
import com.zakariaalnatour.skywatch.model.Aircraft
import com.zakariaalnatour.skywatch.model.FeedSource
import com.zakariaalnatour.skywatch.model.FlightHub
import com.zakariaalnatour.skywatch.model.FlightHubs
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class WidgetFlightEntry(
  val hex: String,
  val callsign: String,
  val aircraftType: String,
  val altitude: String,
  val speed: String,
  val isMilitary: Boolean,
  val isWidebody: Boolean,
)

@Serializable
data class WidgetSnapshot(
  val hubId: String,
  val hubName: String,
  val totalTracked: Int,
  val feedBadge: String,
  val updatedAtMs: Long,
  val topFlights: List<WidgetFlightEntry>,
)

/**
 * Persists a lightweight snapshot of live flights in SharedPreferences so the
 * Googlebook Desktop App Widget displays fresh telemetry even when the main window is closed.
 */
object WidgetSnapshotStore {
  private const val PREFS_NAME = "skywatch_widget_snapshot"
  private const val KEY_SNAPSHOT_JSON = "snapshot_json"

  private val json = Json { ignoreUnknownKeys = true }

  fun buildSnapshot(
    hub: FlightHub,
    aircraft: List<Aircraft>,
    source: FeedSource,
    updatedAtMs: Long,
  ): WidgetSnapshot {
    val highlighted = aircraft
      .sortedWith(
        compareByDescending<Aircraft> { it.isEmergency }
          .thenByDescending { it.isWidebody || it.isMilitary }
          .thenByDescending { it.effectiveAltitudeFt ?: -1 },
      )
      .take(4)
      .map { ac ->
        WidgetFlightEntry(
          hex = ac.hex,
          callsign = ac.displayCallsign,
          aircraftType = ac.aircraftType ?: "—",
          altitude = ac.formattedAltitude,
          speed = ac.formattedSpeed,
          isMilitary = ac.isMilitary,
          isWidebody = ac.isWidebody,
        )
      }

    return WidgetSnapshot(
      hubId = hub.id,
      hubName = hub.name,
      totalTracked = aircraft.size,
      feedBadge = source.badgeText,
      updatedAtMs = updatedAtMs,
      topFlights = highlighted,
    )
  }

  fun save(context: Context, snapshot: WidgetSnapshot) {
    val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_SNAPSHOT_JSON, json.encodeToString(snapshot)).apply()
  }

  fun load(context: Context): WidgetSnapshot? {
    val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_SNAPSHOT_JSON, null) ?: return null
    return runCatching { json.decodeFromString<WidgetSnapshot>(raw) }.getOrNull()
  }

  fun activeHubOrDefault(context: Context): FlightHub =
    FlightHubs.findById(load(context)?.hubId) ?: FlightHubs.DEFAULT
}
