package com.zakariaalnatour.skywatch.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.zakariaalnatour.skywatch.MainActivity
import com.zakariaalnatour.skywatch.R
import com.zakariaalnatour.skywatch.data.LiveFlightRepository
import com.zakariaalnatour.skywatch.data.WidgetSnapshot
import com.zakariaalnatour.skywatch.data.WidgetSnapshotStore
import com.zakariaalnatour.skywatch.model.FlightHubs
import com.zakariaalnatour.skywatch.platform.AndroidDeviceEnvironment
import com.zakariaalnatour.skywatch.platform.GooglebookRuntimeGuard
import com.zakariaalnatour.skywatch.platform.HandoffStateCodec
import com.zakariaalnatour.skywatch.platform.PlatformCheckResult
import com.zakariaalnatour.skywatch.platform.SkyWatchHandoffState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Googlebook OS Desktop App Widget for SkyWatch.
 * Displays live tracked flights, active hub, feed health, one-tap refresh, and quick-jump hub buttons.
 */
class SkyWatchWidgetProvider : AppWidgetProvider() {

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray,
  ) {
    val existing = WidgetSnapshotStore.load(context)
    for (widgetId in appWidgetIds) {
      appWidgetManager.updateAppWidget(widgetId, buildRemoteViews(context, existing))
    }

    if (existing == null) {
      refreshAsync(context)
    }
  }

  override fun onReceive(context: Context, intent: Intent) {
    super.onReceive(context, intent)
    if (intent.action == ACTION_REFRESH_WIDGET) {
      val requestedHubId = intent.getStringExtra(EXTRA_HUB_ID)
      refreshAsync(context, requestedHubId)
    }
  }

  private fun refreshAsync(context: Context, requestedHubId: String? = null) {
    val guard = GooglebookRuntimeGuard.evaluate(AndroidDeviceEnvironment(context))
    if (guard is PlatformCheckResult.Blocked) return

    val pendingResult = goAsync()
    val appContext = context.applicationContext
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      try {
        val hub = FlightHubs.findById(requestedHubId)
          ?: WidgetSnapshotStore.activeHubOrDefault(appContext)
        val repository = LiveFlightRepository()
        val feed = repository.fetchHubFlights(hub)
        val snapshot = WidgetSnapshotStore.buildSnapshot(
          hub = hub,
          aircraft = feed.aircraft,
          source = feed.source,
          updatedAtMs = feed.timestampMs,
        )
        WidgetSnapshotStore.save(appContext, snapshot)
        notifyDataUpdated(appContext)
      } catch (_: Throwable) {
        // Keep last good snapshot on transient network outage
      } finally {
        pendingResult.finish()
      }
    }
  }

  companion object {
    const val ACTION_REFRESH_WIDGET = "com.zakariaalnatour.skywatch.action.REFRESH_WIDGET"
    const val EXTRA_HUB_ID = "extra_hub_id"

    fun notifyDataUpdated(context: Context) {
      val appContext = context.applicationContext
      val manager = AppWidgetManager.getInstance(appContext)
      val component = ComponentName(appContext, SkyWatchWidgetProvider::class.java)
      val ids = manager.getAppWidgetIds(component)
      if (ids.isEmpty()) return
      val snapshot = WidgetSnapshotStore.load(appContext)
      for (id in ids) {
        manager.updateAppWidget(id, buildRemoteViews(appContext, snapshot))
      }
    }

    private fun buildRemoteViews(context: Context, snapshot: WidgetSnapshot?): RemoteViews {
      val views = RemoteViews(context.packageName, R.layout.widget_skywatch)

      val hub = FlightHubs.findById(snapshot?.hubId) ?: FlightHubs.DEFAULT
      views.setTextViewText(R.id.widget_hub_badge, "${hub.id} · ${hub.name}")

      if (snapshot != null) {
        val utcFmt = SimpleDateFormat("HH:mm 'UTC'", Locale.US).apply {
          timeZone = TimeZone.getTimeZone("UTC")
        }
        views.setTextViewText(
          R.id.widget_status_line,
          "${snapshot.totalTracked} live · ${snapshot.feedBadge} · ${utcFmt.format(Date(snapshot.updatedAtMs))}",
        )
      } else {
        views.setTextViewText(
          R.id.widget_status_line,
          "Tap Refresh or open SkyWatch to load live airspace",
        )
      }

      // Header opens SkyWatch on the active hub
      views.setOnClickPendingIntent(
        R.id.widget_header,
        launchHubPendingIntent(context, hub.id, requestCode = 100),
      )

      // Refresh button triggers live background fetch
      val refreshIntent = Intent(context, SkyWatchWidgetProvider::class.java).apply {
        action = ACTION_REFRESH_WIDGET
        putExtra(EXTRA_HUB_ID, hub.id)
      }
      val refreshPending = PendingIntent.getBroadcast(
        context,
        200,
        refreshIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
      views.setOnClickPendingIntent(R.id.widget_refresh_btn, refreshPending)

      // Quick-jump hub chips
      views.setOnClickPendingIntent(R.id.widget_chip_dxb, launchHubPendingIntent(context, "DXB", 301))
      views.setOnClickPendingIntent(R.id.widget_chip_doh, launchHubPendingIntent(context, "DOH", 302))
      views.setOnClickPendingIntent(R.id.widget_chip_ruh, launchHubPendingIntent(context, "RUH", 303))
      views.setOnClickPendingIntent(R.id.widget_chip_mil, launchHubPendingIntent(context, "MIL", 304))

      // Populate top 3 live flights
      val rowIds = listOf(
        Triple(R.id.widget_row_1, R.id.widget_row_1_left, R.id.widget_row_1_right),
        Triple(R.id.widget_row_2, R.id.widget_row_2_left, R.id.widget_row_2_right),
        Triple(R.id.widget_row_3, R.id.widget_row_3_left, R.id.widget_row_3_right),
      )
      val flights = snapshot?.topFlights.orEmpty()
      for (idx in rowIds.indices) {
        val (rowId, leftId, rightId) = rowIds[idx]
        val entry = flights.getOrNull(idx)
        if (entry != null) {
          views.setViewVisibility(rowId, View.VISIBLE)
          val tag = when {
            entry.isMilitary -> " [MIL]"
            entry.isWidebody -> " [HVY]"
            else -> ""
          }
          views.setTextViewText(leftId, "${entry.callsign}  ${entry.aircraftType}$tag")
          views.setTextViewText(rightId, "${entry.altitude} · ${entry.speed}")
          views.setOnClickPendingIntent(
            rowId,
            launchFlightPendingIntent(context, hub.id, entry.hex, requestCode = 400 + idx),
          )
        } else {
          views.setViewVisibility(rowId, View.GONE)
        }
      }

      views.setViewVisibility(
        R.id.widget_empty_hint,
        if (flights.isEmpty()) View.VISIBLE else View.GONE,
      )

      return views
    }

    private fun launchHubPendingIntent(
      context: Context,
      hubId: String,
      requestCode: Int,
    ): PendingIntent {
      val hub = FlightHubs.findById(hubId) ?: FlightHubs.DEFAULT
      val state = SkyWatchHandoffState(
        hubId = hub.id,
        centerLat = hub.latitude,
        centerLon = hub.longitude,
        zoom = hub.defaultZoom,
      )
      val intent = Intent(context, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        putExtras(HandoffStateCodec.toBundle(state))
      }
      return PendingIntent.getActivity(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    }

    private fun launchFlightPendingIntent(
      context: Context,
      hubId: String,
      hex: String,
      requestCode: Int,
    ): PendingIntent {
      val hub = FlightHubs.findById(hubId) ?: FlightHubs.DEFAULT
      val state = SkyWatchHandoffState(
        hubId = hub.id,
        selectedHex = hex,
        centerLat = hub.latitude,
        centerLon = hub.longitude,
        zoom = hub.defaultZoom,
      )
      val intent = Intent(context, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        putExtras(HandoffStateCodec.toBundle(state))
      }
      return PendingIntent.getActivity(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    }
  }
}
