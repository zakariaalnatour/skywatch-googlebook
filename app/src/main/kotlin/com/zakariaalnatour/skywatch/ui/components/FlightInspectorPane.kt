package com.zakariaalnatour.skywatch.ui.components

import android.content.ClipData
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.zakariaalnatour.skywatch.geo.GreatCircleCalculator
import com.zakariaalnatour.skywatch.model.Aircraft
import com.zakariaalnatour.skywatch.model.FeedSource
import com.zakariaalnatour.skywatch.model.FlightEnrichment
import com.zakariaalnatour.skywatch.model.FlightHub
import com.zakariaalnatour.skywatch.model.GeoPoint
import com.zakariaalnatour.skywatch.ui.theme.MonoTelemetryLarge
import com.zakariaalnatour.skywatch.ui.theme.MonoTelemetryValue
import com.zakariaalnatour.skywatch.ui.theme.SkyWatchColors
import com.zakariaalnatour.skywatch.ui.theme.SkyWatchIcons
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun FlightInspectorPane(
  activeHub: FlightHub,
  selectedAircraft: Aircraft?,
  enrichment: FlightEnrichment?,
  allAircraft: List<Aircraft>,
  feedSource: FeedSource,
  latencyMs: Long?,
  followSelected: Boolean,
  onToggleFollow: () -> Unit,
  onPopOutWindow: () -> Unit,
  onRefreshEnrichment: () -> Unit,
  onClearSelection: () -> Unit,
  onSelectAircraft: (Aircraft) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxHeight()
      .background(SkyWatchColors.SurfacePanel)
      .verticalScroll(rememberScrollState())
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    if (selectedAircraft == null) {
      AirspaceOverviewSection(
        activeHub = activeHub,
        allAircraft = allAircraft,
        feedSource = feedSource,
        latencyMs = latencyMs,
        onSelectAircraft = onSelectAircraft,
      )
    } else {
      SelectedFlightInspector(
        aircraft = selectedAircraft,
        enrichment = enrichment,
        followSelected = followSelected,
        onToggleFollow = onToggleFollow,
        onPopOutWindow = onPopOutWindow,
        onRefreshEnrichment = onRefreshEnrichment,
        onClearSelection = onClearSelection,
      )
    }
  }
}

@Composable
private fun AirspaceOverviewSection(
  activeHub: FlightHub,
  allAircraft: List<Aircraft>,
  feedSource: FeedSource,
  latencyMs: Long?,
  onSelectAircraft: (Aircraft) -> Unit,
) {
  Text(
    text = "AIRSPACE TELEMETRY",
    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
    color = SkyWatchColors.RadarCyan,
  )
  Text(
    text = "${activeHub.name} (${activeHub.icaoCode})",
    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 18.sp),
    color = SkyWatchColors.TextPrimary,
  )
  Text(
    text = "Select any aircraft on the geographic radar or flight list to inspect its live telemetry, aircraft photo, and great-circle route arc.",
    style = MaterialTheme.typography.bodySmall,
    color = SkyWatchColors.TextSecondary,
  )

  val airborne = allAircraft.count { !it.onGround }
  val widebodies = allAircraft.count { it.isWidebody }
  val military = allAircraft.count { it.isMilitary }
  val alerts = allAircraft.count { it.isEmergency }

  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    TelemetryMetricTile("TRACKED", allAircraft.size.toString(), SkyWatchColors.RadarCyan, Modifier.weight(1f))
    TelemetryMetricTile("AIRBORNE", airborne.toString(), SkyWatchColors.EmeraldLive, Modifier.weight(1f))
  }
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    TelemetryMetricTile("WIDEBODY", widebodies.toString(), SkyWatchColors.VioletHigh, Modifier.weight(1f))
    TelemetryMetricTile("MILITARY", military.toString(), SkyWatchColors.MilitaryGold, Modifier.weight(1f))
  }
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    TelemetryMetricTile("ACTIVE FEED", feedSource.badgeText, SkyWatchColors.TextPrimary, Modifier.weight(1f))
    TelemetryMetricTile(
      label = if (alerts > 0) "ALERTS" else "LATENCY",
      value = if (alerts > 0) alerts.toString() else (latencyMs?.let { "${it} ms" } ?: "—"),
      accent = if (alerts > 0) SkyWatchColors.AlertCrimson else SkyWatchColors.TextSecondary,
      modifier = Modifier.weight(1f),
    )
  }

  HorizontalDivider(color = SkyWatchColors.BorderSubtle)

  // Highlighted Flights in Airspace
  val highlights = allAircraft
    .sortedWith(
      compareByDescending<Aircraft> { it.isEmergency }
        .thenByDescending { it.isWidebody || it.isMilitary }
        .thenByDescending { it.effectiveAltitudeFt ?: -1 },
    )
    .take(5)

  if (highlights.isNotEmpty()) {
    Text(
      text = "HIGHLIGHTED TARGETS",
      style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
      color = SkyWatchColors.TextMuted,
    )
    for (ac in highlights) {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .clickable { onSelectAircraft(ac) },
        shape = RoundedCornerShape(8.dp),
        color = SkyWatchColors.SurfaceCard,
        border = BorderStroke(1.dp, SkyWatchColors.BorderSubtle),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "${ac.displayCallsign}  ${ac.aircraftType.orEmpty()}",
              style = MaterialTheme.typography.labelMedium,
              color = SkyWatchColors.TextPrimary,
            )
            Text(
              text = ac.description ?: ac.registration ?: ac.hex.uppercase(),
              style = MaterialTheme.typography.bodySmall,
              color = SkyWatchColors.TextSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Text(
            text = "${ac.formattedAltitude} · ${ac.formattedSpeed}",
            style = MaterialTheme.typography.labelSmall,
            color = SkyWatchColors.RadarCyanBright,
          )
        }
      }
    }
  }

  Surface(
    shape = RoundedCornerShape(10.dp),
    color = SkyWatchColors.SurfaceElevated,
    border = BorderStroke(1.dp, SkyWatchColors.BorderSubtle),
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = "GOOGLEBOOK DESKTOP READY",
        style = MaterialTheme.typography.labelSmall,
        color = SkyWatchColors.EmeraldLive,
      )
      Text(
        text = "Android 17 Continue On syncs your active hub, map viewport, and tracked flight automatically. Press Ctrl+N to launch a side-by-side radar window, or ? for all keyboard shortcuts.",
        style = MaterialTheme.typography.bodySmall,
        color = SkyWatchColors.TextSecondary,
      )
    }
  }
}

@Composable
private fun SelectedFlightInspector(
  aircraft: Aircraft,
  enrichment: FlightEnrichment?,
  followSelected: Boolean,
  onToggleFollow: () -> Unit,
  onPopOutWindow: () -> Unit,
  onRefreshEnrichment: () -> Unit,
  onClearSelection: () -> Unit,
) {
  val clipboard = LocalClipboard.current
  val coroutineScope = rememberCoroutineScope()
  val metadata = enrichment?.aircraftMetadata
  val route = enrichment?.route

  // Header
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = aircraft.displayCallsign,
          style = MonoTelemetryLarge,
          color = SkyWatchColors.RadarCyanBright,
        )
        route?.callsignIata?.takeIf { it.isNotBlank() && it != aircraft.displayCallsign }?.let { iata ->
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = SkyWatchColors.SurfaceCard,
            border = BorderStroke(1.dp, SkyWatchColors.BorderSubtle),
          ) {
            Text(
              text = iata,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
              style = MaterialTheme.typography.labelSmall,
              color = SkyWatchColors.TextSecondary,
            )
          }
        }
      }
      Text(
        text = route?.airline?.name
          ?: metadata?.registeredOwner
          ?: aircraft.description
          ?: "Mode-S ${aircraft.hex.uppercase()}",
        style = MaterialTheme.typography.bodySmall,
        color = SkyWatchColors.TextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    Icon(
      imageVector = SkyWatchIcons.Close,
      contentDescription = "Deselect aircraft",
      tint = SkyWatchColors.TextSecondary,
      modifier = Modifier
        .size(22.dp)
        .clickable(onClick = onClearSelection),
    )
  }

  // Aircraft Photo from api.adsbdb.com
  val photoUrl = metadata?.photoUrl ?: metadata?.thumbnailUrl
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .height(148.dp),
    shape = RoundedCornerShape(10.dp),
    color = SkyWatchColors.SurfaceCard,
    border = BorderStroke(1.dp, SkyWatchColors.BorderSubtle),
  ) {
    if (photoUrl != null) {
      SubcomposeAsyncImage(
        model = photoUrl,
        contentDescription = "Aircraft photo ${aircraft.displayCallsign}",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxWidth(),
        loading = {
          Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
              modifier = Modifier.size(22.dp),
              strokeWidth = 2.dp,
              color = SkyWatchColors.RadarCyan,
            )
          }
        },
        error = {
          AircraftPhotoPlaceholder(aircraft, metadata?.manufacturer, metadata?.typeDescription)
        },
      )
    } else {
      AircraftPhotoPlaceholder(
        aircraft = aircraft,
        manufacturer = metadata?.manufacturer,
        modelDesc = metadata?.typeDescription,
        isLoading = enrichment?.isLoading == true,
      )
    }
  }

  // Action Bar
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    InspectorActionButton(
      icon = SkyWatchIcons.MyLocation,
      label = if (followSelected) "Following" else "Follow",
      active = followSelected,
      onClick = onToggleFollow,
      modifier = Modifier.weight(1f),
    )
    InspectorActionButton(
      icon = SkyWatchIcons.OpenInNew,
      label = "Pop Out",
      active = false,
      onClick = onPopOutWindow,
      modifier = Modifier.weight(1f),
    )
    InspectorActionButton(
      icon = SkyWatchIcons.ContentCopy,
      label = "Copy",
      active = false,
      onClick = {
        val summary = buildString {
          append("${aircraft.displayCallsign} (${aircraft.hex.uppercase()})")
          aircraft.registration?.let { append(" reg=$it") }
          aircraft.aircraftType?.let { append(" type=$it") }
          append(" alt=${aircraft.formattedAltitude} gs=${aircraft.formattedSpeed}")
          append(String.format(Locale.US, " pos=%.4f,%.4f", aircraft.latitude, aircraft.longitude))
          route?.let { r ->
            if (r.origin != null && r.destination != null) {
              append(" route=${r.origin.codeLabel}->${r.destination.codeLabel}")
            }
          }
        }
        coroutineScope.launch {
          clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("SkyWatch Telemetry", summary)))
        }
      },
      modifier = Modifier.weight(0.85f),
    )
    InspectorActionButton(
      icon = SkyWatchIcons.Refresh,
      label = "",
      active = false,
      onClick = onRefreshEnrichment,
    )
  }

  // Great-Circle Route Card (from api.adsbdb.com)
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = SkyWatchColors.SurfaceCard,
    border = BorderStroke(1.dp, SkyWatchColors.BorderSubtle),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "ROUTE · ADSBDB",
          style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
          color = SkyWatchColors.RadarCyan,
        )
        if (enrichment?.isLoading == true) {
          CircularProgressIndicator(
            modifier = Modifier.size(12.dp),
            strokeWidth = 1.5.dp,
            color = SkyWatchColors.RadarCyan,
          )
        }
      }

      val origin = route?.origin
      val dest = route?.destination
      if (origin != null && dest != null) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = origin.codeLabel,
              style = MonoTelemetryLarge,
              color = SkyWatchColors.EmeraldLive,
            )
            Text(
              text = origin.municipality ?: origin.name,
              style = MaterialTheme.typography.bodySmall,
              color = SkyWatchColors.TextSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Icon(
            imageVector = SkyWatchIcons.Flight,
            contentDescription = null,
            tint = SkyWatchColors.RadarCyan,
            modifier = Modifier.size(20.dp),
          )
          Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
          ) {
            Text(
              text = dest.codeLabel,
              style = MonoTelemetryLarge,
              color = SkyWatchColors.AmberLow,
            )
            Text(
              text = dest.municipality ?: dest.name,
              style = MaterialTheme.typography.bodySmall,
              color = SkyWatchColors.TextSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        val origGeo = origin.geoPoint
        val destGeo = dest.geoPoint
        if (origGeo != null && destGeo != null) {
          val metrics = GreatCircleCalculator.computeRouteMetrics(
            origin = origGeo,
            destination = destGeo,
            currentPosition = GeoPoint(aircraft.latitude, aircraft.longitude),
          )
          val progress = metrics.progressFraction ?: 0f
          LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
              .fillMaxWidth()
              .height(5.dp)
              .clip(RoundedCornerShape(3.dp)),
            color = SkyWatchColors.RadarCyan,
            trackColor = SkyWatchColors.SurfaceElevated,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "GC ${metrics.totalDistanceNm.roundToInt()} nm",
              style = MaterialTheme.typography.labelSmall,
              color = SkyWatchColors.TextSecondary,
            )
            val remNm = metrics.remainingDistanceNm
            val gs = aircraft.groundSpeedKt
            val etaStr = if (remNm != null && gs != null && gs > 60.0) {
              val totalMinutes = ((remNm / gs) * 60.0).roundToInt()
              val hrs = totalMinutes / 60
              val mins = totalMinutes % 60
              "${remNm.roundToInt()} nm · ETA ${hrs}h ${mins}m"
            } else if (remNm != null) {
              "${remNm.roundToInt()} nm left"
            } else {
              "${(progress * 100).roundToInt()}%"
            }
            Text(
              text = etaStr,
              style = MaterialTheme.typography.labelSmall,
              color = SkyWatchColors.RadarCyanBright,
            )
          }
        }
      } else {
        Text(
          text = if (enrichment?.isLoading == true) {
            "Resolving callsign route via api.adsbdb.com…"
          } else {
            "No published route in ADSBDB for ${aircraft.displayCallsign}"
          },
          style = MaterialTheme.typography.bodySmall,
          color = SkyWatchColors.TextMuted,
        )
      }
    }
  }

  // Telemetry Grid
  Text(
    text = "LIVE ADS-B TELEMETRY",
    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
    color = SkyWatchColors.TextMuted,
  )

  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    TelemetryMetricTile(
      label = "BARO ALT",
      value = aircraft.formattedAltitude,
      accent = SkyWatchColors.altitudeBandColor(
        aircraft.effectiveAltitudeFt,
        aircraft.onGround,
        aircraft.isMilitary,
        aircraft.isEmergency,
      ),
      modifier = Modifier.weight(1f),
    )
    TelemetryMetricTile(
      label = "GROUND SPEED",
      value = aircraft.formattedSpeed,
      accent = SkyWatchColors.RadarCyanBright,
      modifier = Modifier.weight(1f),
    )
  }

  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    TelemetryMetricTile(
      label = "VERTICAL RATE",
      value = aircraft.formattedVerticalRate,
      accent = when {
        (aircraft.baroRateFpm ?: 0) > 128 -> SkyWatchColors.EmeraldLive
        (aircraft.baroRateFpm ?: 0) < -128 -> SkyWatchColors.AmberLow
        else -> SkyWatchColors.TextSecondary
      },
      modifier = Modifier.weight(1f),
    )
    TelemetryMetricTile(
      label = "TRACK / HDG",
      value = "${aircraft.effectiveHeadingDeg.roundToInt().toString().padStart(3, '0')}°",
      accent = SkyWatchColors.TextPrimary,
      modifier = Modifier.weight(1f),
    )
  }

  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    val speedExtra = buildString {
      aircraft.mach?.let { append(String.format(Locale.US, "M%.2f", it)) }
      aircraft.iasKt?.let {
        if (isNotEmpty()) append(" · ")
        append("IAS $it")
      }
      if (isEmpty()) append("—")
    }
    TelemetryMetricTile(
      label = "MACH / IAS",
      value = speedExtra,
      accent = SkyWatchColors.TextPrimary,
      modifier = Modifier.weight(1f),
    )
    TelemetryMetricTile(
      label = "SQUAWK",
      value = aircraft.squawk ?: "—",
      accent = if (aircraft.isEmergency) SkyWatchColors.AlertCrimson else SkyWatchColors.TextPrimary,
      modifier = Modifier.weight(1f),
    )
  }

  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    TelemetryMetricTile(
      label = "AIRFRAME",
      value = listOfNotNull(aircraft.aircraftType ?: metadata?.icaoType, aircraft.registration ?: metadata?.registration)
        .joinToString(" · ")
        .ifBlank { "—" },
      accent = SkyWatchColors.TextPrimary,
      modifier = Modifier.weight(1f),
    )
    TelemetryMetricTile(
      label = "MODE-S / CAT",
      value = "${aircraft.hex.uppercase()}${aircraft.category?.let { " · $it" }.orEmpty()}",
      accent = SkyWatchColors.TextSecondary,
      modifier = Modifier.weight(1f),
    )
  }

  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    TelemetryMetricTile(
      label = "POSITION",
      value = String.format(Locale.US, "%.3f, %.3f", aircraft.latitude, aircraft.longitude),
      accent = SkyWatchColors.TextSecondary,
      modifier = Modifier.weight(1f),
    )
    TelemetryMetricTile(
      label = "SIGNAL / SEEN",
      value = buildString {
        aircraft.rssiDb?.let { append(String.format(Locale.US, "%.1f dB", it)) } ?: append("—")
        aircraft.seenSec?.let { append(String.format(Locale.US, " · %.1fs", it)) }
      },
      accent = SkyWatchColors.TextSecondary,
      modifier = Modifier.weight(1f),
    )
  }

  if (metadata?.registeredOwner != null || aircraft.navAltitudeMcpFt != null) {
    Surface(
      shape = RoundedCornerShape(8.dp),
      color = SkyWatchColors.SurfaceElevated,
      border = BorderStroke(1.dp, SkyWatchColors.BorderSubtle),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        metadata?.let { m ->
          val airframeLine = listOfNotNull(m.manufacturer, m.typeDescription).joinToString(" ")
          if (airframeLine.isNotBlank()) {
            Text(
              text = airframeLine,
              style = MaterialTheme.typography.titleSmall,
              color = SkyWatchColors.TextPrimary,
            )
          }
          m.registeredOwner?.let { owner ->
            Text(
              text = "Operator: $owner${m.countryName?.let { " ($it)" }.orEmpty()}",
              style = MaterialTheme.typography.bodySmall,
              color = SkyWatchColors.TextSecondary,
            )
          }
        }
        aircraft.navAltitudeMcpFt?.let { mcp ->
          Text(
            text = "MCP Target: ${"%,d".format(mcp)} ft${aircraft.navQnhHpa?.let { " · QNH ${it.roundToInt()} hPa" }.orEmpty()}",
            style = MaterialTheme.typography.labelSmall,
            color = SkyWatchColors.TextMuted,
          )
        }
      }
    }
  }
}

@Composable
private fun AircraftPhotoPlaceholder(
  aircraft: Aircraft,
  manufacturer: String?,
  modelDesc: String?,
  isLoading: Boolean = false,
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(148.dp)
      .background(SkyWatchColors.SurfaceElevated),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      if (isLoading) {
        CircularProgressIndicator(
          modifier = Modifier.size(22.dp),
          strokeWidth = 2.dp,
          color = SkyWatchColors.RadarCyan,
        )
      } else {
        Icon(
          imageVector = SkyWatchIcons.Flight,
          contentDescription = null,
          tint = SkyWatchColors.RadarCyan.copy(alpha = 0.55f),
          modifier = Modifier.size(38.dp),
        )
      }
      Text(
        text = listOfNotNull(manufacturer, modelDesc).joinToString(" ")
          .ifBlank { aircraft.description ?: aircraft.aircraftType ?: "Live Aircraft" },
        style = MaterialTheme.typography.labelMedium,
        color = SkyWatchColors.TextSecondary,
      )
    }
  }
}

@Composable
private fun TelemetryMetricTile(
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(8.dp),
    color = SkyWatchColors.SurfaceCard,
    border = BorderStroke(1.dp, SkyWatchColors.BorderSubtle),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
      verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 0.6.sp),
        color = SkyWatchColors.TextMuted,
      )
      Text(
        text = value,
        style = MonoTelemetryValue,
        color = accent,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun InspectorActionButton(
  icon: ImageVector,
  label: String,
  active: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(8.dp),
    color = if (active) SkyWatchColors.RadarCyan.copy(alpha = 0.2f) else SkyWatchColors.SurfaceCard,
    border = BorderStroke(1.dp, if (active) SkyWatchColors.RadarCyan else SkyWatchColors.BorderSubtle),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label.ifBlank { "Refresh" },
        tint = if (active) SkyWatchColors.RadarCyanBright else SkyWatchColors.TextPrimary,
        modifier = Modifier.size(14.dp),
      )
      if (label.isNotBlank()) {
        Spacer(modifier = Modifier.size(4.dp))
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = if (active) SkyWatchColors.RadarCyanBright else SkyWatchColors.TextPrimary,
          maxLines = 1,
        )
      }
    }
  }
}
