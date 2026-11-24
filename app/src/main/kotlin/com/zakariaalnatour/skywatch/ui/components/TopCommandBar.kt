package com.zakariaalnatour.skywatch.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.zakariaalnatour.skywatch.model.FeedSource
import com.zakariaalnatour.skywatch.model.FlightHub
import com.zakariaalnatour.skywatch.model.FlightHubs
import com.zakariaalnatour.skywatch.model.HubFeedMode
import com.zakariaalnatour.skywatch.ui.theme.SkyWatchColors
import com.zakariaalnatour.skywatch.ui.theme.SkyWatchIcons

@Composable
fun TopCommandBar(
  activeHub: FlightHub,
  trackedCount: Int,
  feedSource: FeedSource,
  failoverActive: Boolean,
  latencyMs: Long?,
  isLoading: Boolean,
  continueOnEnabled: Boolean,
  flightsPaneVisible: Boolean,
  inspectorPaneVisible: Boolean,
  onSelectHub: (FlightHub) -> Unit,
  onToggleFlightsPane: () -> Unit,
  onToggleInspectorPane: () -> Unit,
  onToggleContinueOn: () -> Unit,
  onOpenNewWindow: () -> Unit,
  onShowShortcuts: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .zIndex(2f)
      .clipToBounds()
      .background(SkyWatchColors.SurfacePanel),
  ) {
    // Primary Desktop Header Strip
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // Brand Badge
        Box(
          modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SkyWatchColors.SurfaceCardSelected),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = SkyWatchIcons.Radar,
            contentDescription = "SkyWatch",
            tint = SkyWatchColors.RadarCyan,
            modifier = Modifier.size(19.dp),
          )
        }
        Column {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = "SkyWatch",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = SkyWatchColors.TextPrimary,
            )
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = SkyWatchColors.RadarCyan.copy(alpha = 0.14f),
              border = BorderStroke(1.dp, SkyWatchColors.RadarCyan.copy(alpha = 0.4f)),
            ) {
              Text(
                text = "GOOGLEBOOK OS",
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = SkyWatchColors.RadarCyanBright,
              )
            }
          }
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (failoverActive) SkyWatchColors.AmberLow else SkyWatchColors.EmeraldLive),
            )
            Text(
              text = buildString {
                append(feedSource.badgeText)
                append(" · $trackedCount live")
                latencyMs?.let { append(" · ${it}ms") }
              },
              style = MaterialTheme.typography.labelSmall,
              color = if (failoverActive) SkyWatchColors.AmberLow else SkyWatchColors.TextSecondary,
            )
            if (isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(11.dp),
                strokeWidth = 1.5.dp,
                color = SkyWatchColors.RadarCyan,
              )
            }
          }
        }
      }

      // Desktop Actions: Panel Toggles, Continue On, Multi-Instance Window, Keyboard Shortcuts
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
      ) {
        CommandActionChip(
          icon = SkyWatchIcons.SidebarList,
          label = "Flights",
          active = flightsPaneVisible,
          accentColor = SkyWatchColors.RadarCyan,
          onClick = onToggleFlightsPane,
        )
        CommandActionChip(
          icon = SkyWatchIcons.InspectorPanel,
          label = "Inspector",
          active = inspectorPaneVisible,
          accentColor = SkyWatchColors.RadarCyan,
          onClick = onToggleInspectorPane,
        )
        Box(
          modifier = Modifier
            .width(1.dp)
            .height(18.dp)
            .background(SkyWatchColors.BorderSubtle),
        )
        CommandActionChip(
          icon = SkyWatchIcons.Devices,
          label = if (continueOnEnabled) "Continue On" else "Handoff Off",
          active = continueOnEnabled,
          accentColor = SkyWatchColors.EmeraldLive,
          onClick = onToggleContinueOn,
        )
        CommandActionChip(
          icon = SkyWatchIcons.OpenInNew,
          label = "New Window",
          active = false,
          accentColor = SkyWatchColors.RadarCyan,
          onClick = onOpenNewWindow,
        )
        CommandActionChip(
          icon = SkyWatchIcons.Keyboard,
          label = "?",
          active = false,
          accentColor = SkyWatchColors.RadarCyan,
          onClick = onShowShortcuts,
        )
      }
    }

    HorizontalDivider(color = SkyWatchColors.BorderSubtle, thickness = 1.dp)

    // One-Click Hubs Ribbon: Middle East + Global + Widebody / Military
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 14.dp, vertical = 7.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      HubSectionTag("MIDDLE EAST")
      for (hub in FlightHubs.MIDDLE_EAST_HUBS) {
        HubChip(
          hub = hub,
          selected = activeHub.id == hub.id,
          onClick = { onSelectHub(hub) },
        )
      }

      Spacer(modifier = Modifier.width(6.dp))
      Box(
        modifier = Modifier
          .width(1.dp)
          .height(18.dp)
          .background(SkyWatchColors.BorderSubtle),
      )
      Spacer(modifier = Modifier.width(4.dp))

      HubSectionTag("GLOBAL")
      for (hub in FlightHubs.GLOBAL_HUBS) {
        HubChip(
          hub = hub,
          selected = activeHub.id == hub.id,
          onClick = { onSelectHub(hub) },
        )
      }

      Spacer(modifier = Modifier.width(6.dp))
      Box(
        modifier = Modifier
          .width(1.dp)
          .height(18.dp)
          .background(SkyWatchColors.BorderSubtle),
      )
      Spacer(modifier = Modifier.width(4.dp))

      HubSectionTag("FEEDS")
      for (hub in FlightHubs.SPECIAL_FEEDS) {
        HubChip(
          hub = hub,
          selected = activeHub.id == hub.id,
          onClick = { onSelectHub(hub) },
        )
      }
    }

    HorizontalDivider(color = SkyWatchColors.BorderSubtle, thickness = 1.dp)
  }
}

@Composable
private fun HubSectionTag(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 0.7.sp),
    color = SkyWatchColors.TextMuted,
    modifier = Modifier.padding(end = 2.dp),
  )
}

@Composable
private fun HubChip(
  hub: FlightHub,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val accent = when (hub.feedMode) {
    HubFeedMode.MILITARY -> SkyWatchColors.MilitaryGold
    HubFeedMode.WIDEBODY -> SkyWatchColors.VioletHigh
    HubFeedMode.REGIONAL -> SkyWatchColors.RadarCyan
  }
  val bg = if (selected) accent.copy(alpha = 0.20f) else SkyWatchColors.SurfaceCard
  val border = if (selected) accent else SkyWatchColors.BorderSubtle

  Surface(
    modifier = Modifier
      .clip(RoundedCornerShape(7.dp))
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(7.dp),
    color = bg,
    border = BorderStroke(1.dp, border),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      when (hub.feedMode) {
        HubFeedMode.MILITARY -> Icon(
          imageVector = SkyWatchIcons.MilitaryShield,
          contentDescription = null,
          tint = accent,
          modifier = Modifier.size(13.dp),
        )
        HubFeedMode.WIDEBODY -> Icon(
          imageVector = SkyWatchIcons.FlightTakeoff,
          contentDescription = null,
          tint = accent,
          modifier = Modifier.size(13.dp),
        )
        HubFeedMode.REGIONAL -> if (hub.group == com.zakariaalnatour.skywatch.model.HubGroup.GLOBAL) {
          Icon(
            imageVector = SkyWatchIcons.PublicGlobe,
            contentDescription = null,
            tint = if (selected) accent else SkyWatchColors.TextMuted,
            modifier = Modifier.size(12.dp),
          )
        }
      }
      Text(
        text = if (hub.feedMode == HubFeedMode.REGIONAL) hub.id else hub.name,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) SkyWatchColors.TextPrimary else SkyWatchColors.TextSecondary,
      )
    }
  }
}

@Composable
private fun CommandActionChip(
  icon: ImageVector,
  label: String,
  active: Boolean,
  accentColor: Color,
  onClick: () -> Unit,
) {
  Surface(
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(8.dp),
    color = if (active) accentColor.copy(alpha = 0.16f) else SkyWatchColors.SurfaceCard,
    border = BorderStroke(1.dp, if (active) accentColor.copy(alpha = 0.7f) else SkyWatchColors.BorderSubtle),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (active) accentColor else SkyWatchColors.TextSecondary,
        modifier = Modifier.size(15.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = if (active) SkyWatchColors.TextPrimary else SkyWatchColors.TextSecondary,
      )
    }
  }
}
