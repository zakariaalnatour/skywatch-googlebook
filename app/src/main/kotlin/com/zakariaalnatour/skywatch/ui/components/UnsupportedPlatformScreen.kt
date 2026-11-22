package com.zakariaalnatour.skywatch.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zakariaalnatour.skywatch.platform.PlatformCheckResult
import com.zakariaalnatour.skywatch.ui.theme.SkyWatchColors
import com.zakariaalnatour.skywatch.ui.theme.SkyWatchIcons

@Composable
fun UnsupportedPlatformScreen(
  blocked: PlatformCheckResult.Blocked,
  onExitApp: () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(SkyWatchColors.ObsidianBg)
      .padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier = Modifier.widthIn(max = 520.dp),
      shape = RoundedCornerShape(16.dp),
      color = SkyWatchColors.SurfacePanel,
      border = BorderStroke(1.5.dp, SkyWatchColors.AlertCrimson.copy(alpha = 0.8f)),
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Icon(
          imageVector = SkyWatchIcons.Block,
          contentDescription = "Unsupported platform",
          tint = SkyWatchColors.AlertCrimson,
          modifier = Modifier.size(48.dp),
        )
        Text(
          text = blocked.title,
          style = MaterialTheme.typography.headlineSmall,
          color = SkyWatchColors.TextPrimary,
        )
        Text(
          text = blocked.reason,
          style = MaterialTheme.typography.bodyMedium,
          color = SkyWatchColors.TextSecondary,
        )
        if (blocked.detectedMarkers.isNotEmpty()) {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = SkyWatchColors.SurfaceCard,
            border = BorderStroke(1.dp, SkyWatchColors.BorderSubtle),
          ) {
            Column(
              modifier = Modifier.padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(
                text = "DETECTED RUNTIME MARKERS",
                style = MaterialTheme.typography.labelSmall,
                color = SkyWatchColors.AlertCrimson,
              )
              Text(
                text = blocked.detectedMarkers.joinToString("\n"),
                style = MaterialTheme.typography.labelSmall,
                color = SkyWatchColors.TextSecondary,
              )
            }
          }
        }
        Button(
          onClick = onExitApp,
          colors = ButtonDefaults.buttonColors(containerColor = SkyWatchColors.AlertCrimson),
        ) {
          Text("Exit SkyWatch", color = SkyWatchColors.TextPrimary)
        }
      }
    }
  }
}
