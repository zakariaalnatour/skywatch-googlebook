package com.zakariaalnatour.skywatch.ui.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.zakariaalnatour.skywatch.data.MapTileLoader
import com.zakariaalnatour.skywatch.geo.GeoProjection
import com.zakariaalnatour.skywatch.geo.GreatCircleCalculator
import com.zakariaalnatour.skywatch.geo.ViewportState
import com.zakariaalnatour.skywatch.model.Aircraft
import com.zakariaalnatour.skywatch.model.AircraftCategoryClass
import com.zakariaalnatour.skywatch.model.FlightEnrichment
import com.zakariaalnatour.skywatch.model.FlightHub
import com.zakariaalnatour.skywatch.model.FlightHubs
import com.zakariaalnatour.skywatch.model.GeoPoint
import com.zakariaalnatour.skywatch.model.MapTileLayer
import com.zakariaalnatour.skywatch.model.TrackPoint
import com.zakariaalnatour.skywatch.ui.theme.SkyWatchColors
import com.zakariaalnatour.skywatch.ui.theme.SkyWatchIcons
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.roundToInt

@Composable
fun FlightRadarMap(
  activeHub: FlightHub,
  viewport: ViewportState,
  aircraft: List<Aircraft>,
  trails: Map<String, List<TrackPoint>>,
  selectedHex: String?,
  hoveredHex: String?,
  selectedEnrichment: FlightEnrichment?,
  tileLayer: MapTileLayer,
  showTrails: Boolean,
  showGreatCircle: Boolean,
  autoRefreshEnabled: Boolean,
  statusBanner: String?,
  errorMessage: String?,
  onViewportChanged: (ViewportState, Boolean) -> Unit,
  onSelectAircraft: (String?) -> Unit,
  onHoverAircraft: (String?) -> Unit,
  onZoomIn: () -> Unit,
  onZoomOut: () -> Unit,
  onRecenterHub: () -> Unit,
  onToggleMapLayer: () -> Unit,
  onToggleTrails: () -> Unit,
  onToggleGreatCircle: () -> Unit,
  onToggleAutoRefresh: () -> Unit,
  onRefreshNow: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val tileLoader = remember(context) { MapTileLoader.getInstance(context) }

  var canvasSize by remember { mutableStateOf(IntSize.Zero) }
  var tileRevision by remember { mutableIntStateOf(0) }
  var hoverPointer by remember { mutableStateOf<Offset?>(null) }
  var callsignBadgeSize by remember { mutableStateOf(IntSize.Zero) }
  var hoverTooltipSize by remember { mutableStateOf(IntSize.Zero) }

  val latestViewport by rememberUpdatedState(viewport)
  val latestAircraft by rememberUpdatedState(aircraft)
  val latestSize by rememberUpdatedState(canvasSize)
  val latestOnViewportChanged by rememberUpdatedState(onViewportChanged)
  val latestOnSelectAircraft by rememberUpdatedState(onSelectAircraft)
  val latestOnHoverAircraft by rememberUpdatedState(onHoverAircraft)

  val visibleTiles = remember(viewport, canvasSize) {
    GeoProjection.visibleTiles(
      viewport = viewport,
      viewportWidthPx = canvasSize.width.toFloat(),
      viewportHeightPx = canvasSize.height.toFloat(),
    )
  }

  // Trigger asynchronous tile fetches for any tiles not yet in memory cache
  LaunchedEffect(visibleTiles, tileLayer) {
    for (tile in visibleTiles) {
      if (tileLoader.getCached(tileLayer, tile.z, tile.x, tile.y) == null) {
        launch {
          val fetched = tileLoader.loadTile(tileLayer, tile.z, tile.x, tile.y)
          if (fetched != null) {
            tileRevision++
          }
        }
      }
    }
  }

  // Precompute great-circle arcs for the selected aircraft's route (if resolved via adsbdb)
  val selectedAircraft = remember(aircraft, selectedHex) {
    selectedHex?.let { hex -> aircraft.firstOrNull { it.hex == hex } }
  }
  val routeArcSegments = remember(selectedAircraft, selectedEnrichment, showGreatCircle) {
    if (!showGreatCircle || selectedAircraft == null) {
      null
    } else {
      val origin = selectedEnrichment?.route?.origin?.geoPoint
      val dest = selectedEnrichment?.route?.destination?.geoPoint
      if (origin != null && dest != null) {
        val current = GeoPoint(selectedAircraft.latitude, selectedAircraft.longitude)
        val completed = GreatCircleCalculator.splitSegmentsAtAntimeridian(
          GreatCircleCalculator.greatCircleArc(origin, current, segments = 40),
        )
        val remaining = GreatCircleCalculator.splitSegmentsAtAntimeridian(
          GreatCircleCalculator.greatCircleArc(current, dest, segments = 48),
        )
        Triple(completed, remaining, Pair(selectedEnrichment.route.origin, selectedEnrichment.route.destination))
      } else {
        null
      }
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .clipToBounds()
      .background(SkyWatchColors.ObsidianBg)
      .onSizeChanged { canvasSize = it },
  ) {
    // Core Geographic Radar Canvas (owns all map gestures so HUD overlays never conflict)
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .clipToBounds()
        // Pan + pinch-to-zoom on map surface only
        .pointerInput(Unit) {
          detectTransformGestures { centroid, pan, zoomChange, _ ->
            var next = latestViewport
            var changed = false
            if (pan != Offset.Zero) {
              next = GeoProjection.panByScreenDelta(
                viewport = next,
                dxPx = pan.x,
                dyPx = pan.y,
              )
              changed = true
            }
            if (
              zoomChange != 1f &&
              zoomChange.isFinite() &&
              zoomChange > 0f &&
              latestSize.width > 0 &&
              latestSize.height > 0
            ) {
              val zoomDelta = (ln(zoomChange.toDouble()) / ln(2.0)).toFloat()
              next = GeoProjection.zoomAroundScreenPoint(
                viewport = next,
                newZoomRaw = next.zoom + zoomDelta,
                anchorScreenX = centroid.x,
                anchorScreenY = centroid.y,
                viewportWidthPx = latestSize.width.toFloat(),
                viewportHeightPx = latestSize.height.toFloat(),
              )
              changed = true
            }
            if (changed) {
              latestOnViewportChanged(next, true)
            }
          }
        }
        // Click to select aircraft; double-click to zoom in around cursor
        .pointerInput(Unit) {
          detectTapGestures(
            onTap = { tapOffset ->
              val hit = findNearestAircraft(
                pointer = tapOffset,
                aircraft = latestAircraft,
                viewport = latestViewport,
                widthPx = latestSize.width.toFloat(),
                heightPx = latestSize.height.toFloat(),
                hitRadiusPx = 32f,
              )
              latestOnSelectAircraft(hit?.hex)
            },
            onDoubleTap = { tapOffset ->
              val next = GeoProjection.zoomAroundScreenPoint(
                viewport = latestViewport,
                newZoomRaw = latestViewport.zoom + 1.0f,
                anchorScreenX = tapOffset.x,
                anchorScreenY = tapOffset.y,
                viewportWidthPx = latestSize.width.toFloat(),
                viewportHeightPx = latestSize.height.toFloat(),
              )
              latestOnViewportChanged(next, true)
            },
          )
        }
        // Trackpad scroll-wheel zoom + pointer hover inspection
        .pointerInput(Unit) {
          awaitPointerEventScope {
            while (true) {
              val event = awaitPointerEvent()
              when (event.type) {
                PointerEventType.Scroll -> {
                  val change = event.changes.firstOrNull() ?: continue
                  val scrollY = change.scrollDelta.y
                  if (scrollY != 0f) {
                    val zoomDelta = (-scrollY * 0.35f).coerceIn(-0.9f, 0.9f)
                    val next = GeoProjection.zoomAroundScreenPoint(
                      viewport = latestViewport,
                      newZoomRaw = latestViewport.zoom + zoomDelta,
                      anchorScreenX = change.position.x,
                      anchorScreenY = change.position.y,
                      viewportWidthPx = latestSize.width.toFloat(),
                      viewportHeightPx = latestSize.height.toFloat(),
                    )
                    latestOnViewportChanged(next, false)
                    change.consume()
                  }
                }
                PointerEventType.Move -> {
                  val pos = event.changes.firstOrNull()?.position
                  hoverPointer = pos
                  if (pos != null) {
                    val hit = findNearestAircraft(
                      pointer = pos,
                      aircraft = latestAircraft,
                      viewport = latestViewport,
                      widthPx = latestSize.width.toFloat(),
                      heightPx = latestSize.height.toFloat(),
                      hitRadiusPx = 28f,
                    )
                    latestOnHoverAircraft(hit?.hex)
                  }
                }
                PointerEventType.Exit -> {
                  hoverPointer = null
                  latestOnHoverAircraft(null)
                }
                else -> Unit
              }
            }
          }
        },
    ) {
      // Read tileRevision so Compose redraws whenever a tile completes loading
      @Suppress("UNUSED_EXPRESSION")
      tileRevision

      val widthPx = size.width
      val heightPx = size.height

      clipRect(left = 0f, top = 0f, right = widthPx, bottom = heightPx) {
        // 1. Background radar graticule (visible while tiles stream in)
        drawRadarGraticule(viewport, widthPx, heightPx)

        // 2. Keyless Map Tiles: Esri World Dark Gray Base + Reference overlay (or OSM fallback)
        for (tile in visibleTiles) {
          val cached = tileLoader.getCached(tileLayer, tile.z, tile.x, tile.y)
          if (cached != null) {
            val dstOffset = IntOffset(floor(tile.screenLeft).toInt(), floor(tile.screenTop).toInt())
            val drawSizeInt = ceil(tile.tileDrawSizePx).toInt() + 1
            val dstSize = IntSize(drawSizeInt, drawSizeInt)
            drawImage(
              image = cached.base.asImageBitmap(),
              dstOffset = dstOffset,
              dstSize = dstSize,
            )
            cached.referenceOverlay?.let { refBitmap ->
              drawImage(
                image = refBitmap.asImageBitmap(),
                dstOffset = dstOffset,
                dstSize = dstSize,
              )
            }
          }
        }

        // Subtle atmospheric dark tint over OSM when OSM layer is chosen so radar vectors pop
        if (tileLayer == MapTileLayer.OPEN_STREET_MAP) {
          drawRect(color = Color(0x55070C14))
        }

        // 3. Hub airport beacons
        for (hub in FlightHubs.ALL) {
          if (hub.feedMode != com.zakariaalnatour.skywatch.model.HubFeedMode.REGIONAL) continue
          val pt = GeoProjection.latLonToScreen(hub.latitude, hub.longitude, viewport, widthPx, heightPx)
          if (pt.x in -40f..(widthPx + 40f) && pt.y in -40f..(heightPx + 40f)) {
            val isActive = hub.id == activeHub.id
            val center = Offset(pt.x, pt.y)
            drawCircle(
              color = if (isActive) SkyWatchColors.RadarCyan.copy(alpha = 0.22f) else SkyWatchColors.TextMuted.copy(alpha = 0.12f),
              radius = if (isActive) 18f else 9f,
              center = center,
            )
            drawCircle(
              color = if (isActive) SkyWatchColors.RadarCyan else SkyWatchColors.TextSecondary.copy(alpha = 0.7f),
              radius = if (isActive) 4.5f else 3f,
              center = center,
            )
            if (isActive) {
              drawCircle(
                color = SkyWatchColors.RadarCyan.copy(alpha = 0.55f),
                radius = 12f,
                center = center,
                style = Stroke(width = 1.4f),
              )
            }
          }
        }

        // 4. Great-circle route arc for selected flight
        if (routeArcSegments != null) {
          val (completedSegments, remainingSegments, airports) = routeArcSegments
          for (segment in completedSegments) {
            drawGeoPolyline(
              points = segment,
              viewport = viewport,
              widthPx = widthPx,
              heightPx = heightPx,
              color = SkyWatchColors.RadarCyan,
              strokeWidth = 3.2f,
              dashed = false,
            )
          }
          for (segment in remainingSegments) {
            drawGeoPolyline(
              points = segment,
              viewport = viewport,
              widthPx = widthPx,
              heightPx = heightPx,
              color = SkyWatchColors.AmberLow.copy(alpha = 0.85f),
              strokeWidth = 2.4f,
              dashed = true,
            )
          }
          airports.first.geoPoint?.let { orig ->
            val p = GeoProjection.latLonToScreen(orig.latitude, orig.longitude, viewport, widthPx, heightPx)
            drawCircle(color = SkyWatchColors.EmeraldLive, radius = 7f, center = Offset(p.x, p.y))
            drawCircle(color = Color.White, radius = 3f, center = Offset(p.x, p.y))
          }
          airports.second.geoPoint?.let { dst ->
            val p = GeoProjection.latLonToScreen(dst.latitude, dst.longitude, viewport, widthPx, heightPx)
            drawCircle(color = SkyWatchColors.AmberLow, radius = 7f, center = Offset(p.x, p.y))
            drawCircle(color = Color.White, radius = 3f, center = Offset(p.x, p.y))
          }
        }

        // 5. Live trails
        if (showTrails) {
          for (ac in aircraft) {
            val track = trails[ac.hex] ?: continue
            if (track.size < 2) continue
            val isSelected = ac.hex == selectedHex
            val color = SkyWatchColors.altitudeBandColor(
              altitudeFt = ac.effectiveAltitudeFt,
              onGround = ac.onGround,
              isMilitary = ac.isMilitary,
              isEmergency = ac.isEmergency,
            )
            val geoPoints = track.map { GeoPoint(it.latitude, it.longitude) }
            val split = GreatCircleCalculator.splitSegmentsAtAntimeridian(geoPoints)
            for (segment in split) {
              drawGeoPolyline(
                points = segment,
                viewport = viewport,
                widthPx = widthPx,
                heightPx = heightPx,
                color = color.copy(alpha = if (isSelected) 0.92f else 0.42f),
                strokeWidth = if (isSelected) 3.0f else 1.5f,
                dashed = false,
              )
            }
          }
        }

        // 6. Aircraft icons (draw unselected first, hovered & selected on top)
        for (ac in aircraft) {
          if (ac.hex == selectedHex || ac.hex == hoveredHex) continue
          drawAircraftMarker(ac, viewport, widthPx, heightPx, isSelected = false, isHovered = false)
        }
        hoveredHex?.let { hex ->
          if (hex != selectedHex) {
            aircraft.firstOrNull { it.hex == hex }?.let { ac ->
              drawAircraftMarker(ac, viewport, widthPx, heightPx, isSelected = false, isHovered = true)
            }
          }
        }
        selectedAircraft?.let { ac ->
          drawAircraftMarker(ac, viewport, widthPx, heightPx, isSelected = true, isHovered = false)
        }
      }
    }

    // Selected aircraft crisp floating callsign badge clamped inside map viewport bounds
    if (selectedAircraft != null && canvasSize.width > 0 && canvasSize.height > 0) {
      val pt = GeoProjection.latLonToScreen(
        selectedAircraft.latitude,
        selectedAircraft.longitude,
        viewport,
        canvasSize.width.toFloat(),
        canvasSize.height.toFloat(),
      )
      if (pt.x in 0f..canvasSize.width.toFloat() && pt.y in 0f..canvasSize.height.toFloat()) {
        val marginPx = 8
        val badgeW = if (callsignBadgeSize.width > 0) callsignBadgeSize.width else 180
        val badgeH = if (callsignBadgeSize.height > 0) callsignBadgeSize.height else 28
        val maxX = (canvasSize.width - badgeW - marginPx).coerceAtLeast(marginPx)
        val maxY = (canvasSize.height - badgeH - marginPx).coerceAtLeast(marginPx)
        val clampedX = (pt.x + 18f).roundToInt().coerceIn(marginPx, maxX)
        val clampedY = (pt.y - 26f).roundToInt().coerceIn(marginPx, maxY)

        Surface(
          modifier = Modifier
            .zIndex(1f)
            .offset { IntOffset(clampedX, clampedY) }
            .onSizeChanged { callsignBadgeSize = it },
          shape = RoundedCornerShape(6.dp),
          color = SkyWatchColors.SurfaceElevated.copy(alpha = 0.94f),
          border = BorderStroke(1.dp, SkyWatchColors.RadarCyan),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = selectedAircraft.displayCallsign,
              style = MaterialTheme.typography.labelMedium,
              color = SkyWatchColors.RadarCyanBright,
            )
            Text(
              text = selectedAircraft.formattedAltitude,
              style = MaterialTheme.typography.labelSmall,
              color = SkyWatchColors.TextPrimary,
            )
            Text(
              text = selectedAircraft.formattedSpeed,
              style = MaterialTheme.typography.labelSmall,
              color = SkyWatchColors.TextSecondary,
            )
          }
        }
      }
    }

    // Desktop Hover Tooltip clamped strictly inside map viewport bounds
    val hoveredAc = remember(aircraft, hoveredHex, selectedHex) {
      if (hoveredHex == null || hoveredHex == selectedHex) null
      else aircraft.firstOrNull { it.hex == hoveredHex }
    }
    if (hoveredAc != null && hoverPointer != null && canvasSize.width > 0 && canvasSize.height > 0) {
      val pos = hoverPointer!!
      val marginPx = 8
      val tooltipW = if (hoverTooltipSize.width > 0) hoverTooltipSize.width else 220
      val tooltipH = if (hoverTooltipSize.height > 0) hoverTooltipSize.height else 28
      val maxX = (canvasSize.width - tooltipW - marginPx).coerceAtLeast(marginPx)
      val maxY = (canvasSize.height - tooltipH - marginPx).coerceAtLeast(marginPx)
      val clampedX = (pos.x + 16f).roundToInt().coerceIn(marginPx, maxX)
      val clampedY = (pos.y - 34f).roundToInt().coerceIn(marginPx, maxY)

      Surface(
        modifier = Modifier
          .zIndex(1f)
          .offset { IntOffset(clampedX, clampedY) }
          .onSizeChanged { hoverTooltipSize = it },
        shape = RoundedCornerShape(6.dp),
        color = SkyWatchColors.ObsidianBg.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, SkyWatchColors.BorderSubtle),
      ) {
        Text(
          text = buildString {
            append(hoveredAc.displayCallsign)
            hoveredAc.aircraftType?.let { append(" • $it") }
            append(" • ${hoveredAc.formattedAltitude}")
            append(" • ${hoveredAc.formattedSpeed}")
          },
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          style = MaterialTheme.typography.labelSmall,
          color = SkyWatchColors.TextPrimary,
        )
      }
    }

    // Floating Top-Left HUD (Hub coordinates + zoom + banner)
    Column(
      modifier = Modifier
        .align(Alignment.TopStart)
        .zIndex(1f)
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = SkyWatchColors.SurfacePanel.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, SkyWatchColors.BorderSubtle),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "${activeHub.id} · ${activeHub.name}",
            style = MaterialTheme.typography.titleSmall,
            color = SkyWatchColors.TextPrimary,
          )
          Text(
            text = String.format(
              Locale.US,
              "%.2f°, %.2f°  Z%.1f",
              viewport.centerLat,
              viewport.centerLon,
              viewport.zoom,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = SkyWatchColors.TextSecondary,
          )
          Text(
            text = tileLayer.label,
            style = MaterialTheme.typography.labelSmall,
            color = SkyWatchColors.RadarCyan,
          )
        }
      }

      if (!statusBanner.isNullOrBlank()) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = SkyWatchColors.AmberLow.copy(alpha = 0.18f),
          border = BorderStroke(1.dp, SkyWatchColors.AmberLow.copy(alpha = 0.65f)),
        ) {
          Text(
            text = statusBanner,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = SkyWatchColors.AmberLow,
          )
        }
      }

      if (!errorMessage.isNullOrBlank()) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = SkyWatchColors.AlertCrimson.copy(alpha = 0.22f),
          border = BorderStroke(1.dp, SkyWatchColors.AlertCrimson.copy(alpha = 0.75f)),
        ) {
          Text(
            text = errorMessage,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = SkyWatchColors.TextPrimary,
          )
        }
      }
    }

    // Floating Top-Right Desktop Radar Controls (42dp Zoom Pill + 40dp Tool Buttons)
    Column(
      modifier = Modifier
        .align(Alignment.TopEnd)
        .zIndex(1f)
        .padding(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      ZoomControlPill(
        zoom = viewport.zoom,
        onZoomIn = onZoomIn,
        onRecenterHub = onRecenterHub,
        onZoomOut = onZoomOut,
      )
      Spacer(modifier = Modifier.height(2.dp))
      MapControlButton(
        icon = SkyWatchIcons.MyLocation,
        contentDescription = "Recenter Hub",
        onClick = onRecenterHub,
      )
      MapControlButton(
        icon = SkyWatchIcons.Layers,
        contentDescription = "Toggle Map Layer (M)",
        active = tileLayer == MapTileLayer.OPEN_STREET_MAP,
        onClick = onToggleMapLayer,
      )
      MapControlButton(
        icon = SkyWatchIcons.Timeline,
        contentDescription = "Toggle Trails (T)",
        active = showTrails,
        onClick = onToggleTrails,
      )
      MapControlButton(
        icon = SkyWatchIcons.Route,
        contentDescription = "Toggle Great-Circle Arc (G)",
        active = showGreatCircle,
        onClick = onToggleGreatCircle,
      )
      MapControlButton(
        icon = if (autoRefreshEnabled) SkyWatchIcons.Pause else SkyWatchIcons.PlayArrow,
        contentDescription = "Pause/Resume Polling (Space)",
        active = autoRefreshEnabled,
        onClick = onToggleAutoRefresh,
      )
      MapControlButton(
        icon = SkyWatchIcons.Refresh,
        contentDescription = "Refresh Feed (R)",
        onClick = onRefreshNow,
      )
    }

    // Floating Bottom-Left Altitude Legend + Map Attribution
    Surface(
      modifier = Modifier
        .align(Alignment.BottomStart)
        .zIndex(1f)
        .padding(12.dp),
      shape = RoundedCornerShape(8.dp),
      color = SkyWatchColors.SurfacePanel.copy(alpha = 0.86f),
      border = BorderStroke(1.dp, SkyWatchColors.BorderSubtle),
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          LegendDot("GND", SkyWatchColors.GroundSlate)
          LegendDot("<10k", SkyWatchColors.AmberLow)
          LegendDot("10–25k", SkyWatchColors.EmeraldLive)
          LegendDot("25–38k", SkyWatchColors.RadarCyan)
          LegendDot("38k+", SkyWatchColors.VioletHigh)
          LegendDot("MIL", SkyWatchColors.MilitaryGold)
          LegendDot("7700", SkyWatchColors.AlertCrimson)
        }
        Text(
          text = tileLayer.attribution,
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
          color = SkyWatchColors.TextMuted,
        )
      }
    }
  }
}

@Composable
private fun ZoomControlPill(
  zoom: Float,
  onZoomIn: () -> Unit,
  onRecenterHub: () -> Unit,
  onZoomOut: () -> Unit,
) {
  Surface(
    modifier = Modifier.width(42.dp),
    shape = RoundedCornerShape(10.dp),
    color = SkyWatchColors.SurfacePanel.copy(alpha = 0.94f),
    border = BorderStroke(1.dp, SkyWatchColors.BorderSubtle),
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .clickable(onClick = onZoomIn),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = SkyWatchIcons.Add,
          contentDescription = "Zoom In (+)",
          tint = SkyWatchColors.TextPrimary,
          modifier = Modifier.size(22.dp),
        )
      }
      HorizontalDivider(color = SkyWatchColors.BorderSubtle, thickness = 1.dp)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(34.dp)
          .background(SkyWatchColors.SurfaceElevated.copy(alpha = 0.72f))
          .clickable(onClick = onRecenterHub),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = String.format(Locale.US, "Z%.1f", zoom),
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
          ),
          color = SkyWatchColors.RadarCyanBright,
          maxLines = 1,
        )
      }
      HorizontalDivider(color = SkyWatchColors.BorderSubtle, thickness = 1.dp)
      Box(
        modifier = Modifier
          .size(42.dp)
          .clickable(onClick = onZoomOut),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = SkyWatchIcons.Remove,
          contentDescription = "Zoom Out (-)",
          tint = SkyWatchColors.TextPrimary,
          modifier = Modifier.size(22.dp),
        )
      }
    }
  }
}

@Composable
private fun MapControlButton(
  icon: ImageVector,
  contentDescription: String,
  active: Boolean = false,
  onClick: () -> Unit,
) {
  Box(
    modifier = Modifier
      .size(40.dp)
      .clip(RoundedCornerShape(9.dp))
      .background(
        if (active) SkyWatchColors.SurfaceCardSelected.copy(alpha = 0.95f)
        else SkyWatchColors.SurfacePanel.copy(alpha = 0.92f),
      )
      .border(
        width = 1.dp,
        color = if (active) SkyWatchColors.RadarCyan else SkyWatchColors.BorderSubtle,
        shape = RoundedCornerShape(9.dp),
      )
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint = if (active) SkyWatchColors.RadarCyanBright else SkyWatchColors.TextPrimary,
      modifier = Modifier.size(20.dp),
    )
  }
}

@Composable
private fun LegendDot(label: String, color: Color) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Box(
      modifier = Modifier
        .size(7.dp)
        .clip(CircleShape)
        .background(color),
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Medium),
      color = SkyWatchColors.TextSecondary,
    )
  }
}

private fun DrawScope.drawRadarGraticule(
  viewport: ViewportState,
  widthPx: Float,
  heightPx: Float,
) {
  val stepDeg = when {
    viewport.zoom < 4f -> 20
    viewport.zoom < 6.5f -> 10
    viewport.zoom < 9f -> 5
    else -> 2
  }
  val gridColor = Color(0xFF132033)
  for (lon in -180..180 step stepDeg) {
    val pTop = GeoProjection.latLonToScreen(80.0, lon.toDouble(), viewport, widthPx, heightPx)
    val pBot = GeoProjection.latLonToScreen(-80.0, lon.toDouble(), viewport, widthPx, heightPx)
    if (pTop.x in -10f..(widthPx + 10f)) {
      drawLine(gridColor, Offset(pTop.x, 0f), Offset(pBot.x, heightPx), strokeWidth = 1f)
    }
  }
  for (lat in -80..80 step stepDeg) {
    val p = GeoProjection.latLonToScreen(lat.toDouble(), viewport.centerLon, viewport, widthPx, heightPx)
    if (p.y in -10f..(heightPx + 10f)) {
      drawLine(gridColor, Offset(0f, p.y), Offset(widthPx, p.y), strokeWidth = 1f)
    }
  }
}

private fun DrawScope.drawGeoPolyline(
  points: List<GeoPoint>,
  viewport: ViewportState,
  widthPx: Float,
  heightPx: Float,
  color: Color,
  strokeWidth: Float,
  dashed: Boolean,
) {
  if (points.size < 2) return
  val path = Path()
  var prevX: Float? = null
  for (idx in points.indices) {
    val sp = GeoProjection.latLonToScreen(points[idx].latitude, points[idx].longitude, viewport, widthPx, heightPx)
    if (idx == 0 || (prevX != null && kotlin.math.abs(sp.x - prevX) > widthPx * 0.65f)) {
      path.moveTo(sp.x, sp.y)
    } else {
      path.lineTo(sp.x, sp.y)
    }
    prevX = sp.x
  }
  drawPath(
    path = path,
    color = color,
    style = Stroke(
      width = strokeWidth,
      cap = StrokeCap.Round,
      pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f) else null,
    ),
  )
}

private fun DrawScope.drawAircraftMarker(
  aircraft: Aircraft,
  viewport: ViewportState,
  widthPx: Float,
  heightPx: Float,
  isSelected: Boolean,
  isHovered: Boolean,
) {
  val pt = GeoProjection.latLonToScreen(aircraft.latitude, aircraft.longitude, viewport, widthPx, heightPx)
  if (pt.x < -30f || pt.x > widthPx + 30f || pt.y < -30f || pt.y > heightPx + 30f) return

  val center = Offset(pt.x, pt.y)
  val fillColor = SkyWatchColors.altitudeBandColor(
    altitudeFt = aircraft.effectiveAltitudeFt,
    onGround = aircraft.onGround,
    isMilitary = aircraft.isMilitary,
    isEmergency = aircraft.isEmergency,
  )

  if (isSelected) {
    drawCircle(
      color = SkyWatchColors.RadarCyan.copy(alpha = 0.22f),
      radius = 22f,
      center = center,
    )
    drawCircle(
      color = SkyWatchColors.RadarCyanBright,
      radius = 17f,
      center = center,
      style = Stroke(width = 2.0f),
    )
  } else if (isHovered) {
    drawCircle(
      color = Color.White.copy(alpha = 0.16f),
      radius = 16f,
      center = center,
    )
  }

  val scale = when {
    isSelected -> 1.38f
    isHovered -> 1.22f
    aircraft.visualClass == AircraftCategoryClass.WIDEBODY -> 1.15f
    aircraft.visualClass == AircraftCategoryClass.MILITARY -> 1.10f
    else -> 0.95f
  }

  rotate(degrees = aircraft.effectiveHeadingDeg.toFloat(), pivot = center) {
    val path = buildAircraftSilhouettePath(center, scale, aircraft.visualClass)
    drawPath(path = path, color = Color(0xCC04070D), style = Stroke(width = 2.4f))
    drawPath(path = path, color = fillColor)
  }
}

private fun buildAircraftSilhouettePath(
  c: Offset,
  s: Float,
  cls: AircraftCategoryClass,
): Path = Path().apply {
  when (cls) {
    AircraftCategoryClass.MILITARY -> {
      // Tactical delta wing silhouette pointing north (0 deg)
      moveTo(c.x, c.y - 12f * s)
      lineTo(c.x + 2.2f * s, c.y - 4f * s)
      lineTo(c.x + 10.5f * s, c.y + 4.5f * s)
      lineTo(c.x + 10.5f * s, c.y + 7f * s)
      lineTo(c.x + 2.5f * s, c.y + 5.5f * s)
      lineTo(c.x + 3.8f * s, c.y + 10.5f * s)
      lineTo(c.x, c.y + 8.5f * s)
      lineTo(c.x - 3.8f * s, c.y + 10.5f * s)
      lineTo(c.x - 2.5f * s, c.y + 5.5f * s)
      lineTo(c.x - 10.5f * s, c.y + 7f * s)
      lineTo(c.x - 10.5f * s, c.y + 4.5f * s)
      lineTo(c.x - 2.2f * s, c.y - 4f * s)
      close()
    }
    AircraftCategoryClass.HELICOPTER -> {
      // Rotor cross + fuselage
      moveTo(c.x, c.y - 9f * s)
      lineTo(c.x + 2.6f * s, c.y - 3f * s)
      lineTo(c.x + 8.5f * s, c.y - 3f * s)
      lineTo(c.x + 8.5f * s, c.y - 0.8f * s)
      lineTo(c.x + 2.4f * s, c.y - 0.8f * s)
      lineTo(c.x + 1.4f * s, c.y + 9.5f * s)
      lineTo(c.x + 4f * s, c.y + 10.5f * s)
      lineTo(c.x - 4f * s, c.y + 10.5f * s)
      lineTo(c.x - 1.4f * s, c.y + 9.5f * s)
      lineTo(c.x - 2.4f * s, c.y - 0.8f * s)
      lineTo(c.x - 8.5f * s, c.y - 0.8f * s)
      lineTo(c.x - 8.5f * s, c.y - 3f * s)
      lineTo(c.x - 2.6f * s, c.y - 3f * s)
      close()
    }
    AircraftCategoryClass.WIDEBODY -> {
      // Heavy swept-wing airliner
      moveTo(c.x, c.y - 13f * s)
      lineTo(c.x + 2.3f * s, c.y - 5f * s)
      lineTo(c.x + 12.5f * s, c.y + 2.5f * s)
      lineTo(c.x + 12.5f * s, c.y + 4.8f * s)
      lineTo(c.x + 2.4f * s, c.y + 1.5f * s)
      lineTo(c.x + 2.0f * s, c.y + 8.0f * s)
      lineTo(c.x + 6.0f * s, c.y + 11.2f * s)
      lineTo(c.x + 6.0f * s, c.y + 12.8f * s)
      lineTo(c.x, c.y + 10.6f * s)
      lineTo(c.x - 6.0f * s, c.y + 12.8f * s)
      lineTo(c.x - 6.0f * s, c.y + 11.2f * s)
      lineTo(c.x - 2.0f * s, c.y + 8.0f * s)
      lineTo(c.x - 2.4f * s, c.y + 1.5f * s)
      lineTo(c.x - 12.5f * s, c.y + 4.8f * s)
      lineTo(c.x - 12.5f * s, c.y + 2.5f * s)
      lineTo(c.x - 2.3f * s, c.y - 5f * s)
      close()
    }
    else -> {
      // Clean standard swept jet
      moveTo(c.x, c.y - 11f * s)
      lineTo(c.x + 1.9f * s, c.y - 3.5f * s)
      lineTo(c.x + 10f * s, c.y + 2f * s)
      lineTo(c.x + 10f * s, c.y + 4f * s)
      lineTo(c.x + 1.8f * s, c.y + 1.2f * s)
      lineTo(c.x + 1.5f * s, c.y + 7f * s)
      lineTo(c.x + 4.8f * s, c.y + 9.8f * s)
      lineTo(c.x + 4.8f * s, c.y + 11.2f * s)
      lineTo(c.x, c.y + 9.5f * s)
      lineTo(c.x - 4.8f * s, c.y + 11.2f * s)
      lineTo(c.x - 4.8f * s, c.y + 9.8f * s)
      lineTo(c.x - 1.5f * s, c.y + 7f * s)
      lineTo(c.x - 1.8f * s, c.y + 1.2f * s)
      lineTo(c.x - 10f * s, c.y + 4f * s)
      lineTo(c.x - 10f * s, c.y + 2f * s)
      lineTo(c.x - 1.9f * s, c.y - 3.5f * s)
      close()
    }
  }
}

private fun findNearestAircraft(
  pointer: Offset,
  aircraft: List<Aircraft>,
  viewport: ViewportState,
  widthPx: Float,
  heightPx: Float,
  hitRadiusPx: Float,
): Aircraft? {
  if (widthPx <= 0f || heightPx <= 0f) return null
  var best: Aircraft? = null
  var bestDist = hitRadiusPx
  for (ac in aircraft) {
    val sp = GeoProjection.latLonToScreen(ac.latitude, ac.longitude, viewport, widthPx, heightPx)
    val d = hypot((sp.x - pointer.x).toDouble(), (sp.y - pointer.y).toDouble()).toFloat()
    if (d < bestDist) {
      bestDist = d
      best = ac
    }
  }
  return best
}
