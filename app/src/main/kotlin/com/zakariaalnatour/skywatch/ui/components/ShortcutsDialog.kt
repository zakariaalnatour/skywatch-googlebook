package com.zakariaalnatour.skywatch.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.zakariaalnatour.skywatch.platform.KeyboardShortcutHandler
import com.zakariaalnatour.skywatch.ui.theme.SkyWatchColors

@Composable
fun ShortcutsDialog(onDismiss: () -> Unit) {
  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = SkyWatchColors.SurfacePanel,
      border = BorderStroke(1.dp, SkyWatchColors.BorderSubtle),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "Googlebook Keyboard & Trackpad Shortcuts",
          style = MaterialTheme.typography.titleMedium,
          color = SkyWatchColors.TextPrimary,
        )
        Text(
          text = "Trackpad: Two-finger scroll zooms around cursor • Click-drag pans • Hover previews flight • Double-click zooms in.",
          style = MaterialTheme.typography.bodySmall,
          color = SkyWatchColors.TextSecondary,
        )

        HorizontalDivider(color = SkyWatchColors.BorderSubtle)

        for (entry in KeyboardShortcutHandler.CHEAT_SHEET) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = entry.description,
              style = MaterialTheme.typography.bodySmall,
              color = SkyWatchColors.TextPrimary,
              modifier = Modifier.weight(1f),
            )
            Surface(
              shape = RoundedCornerShape(5.dp),
              color = SkyWatchColors.SurfaceCard,
              border = BorderStroke(1.dp, SkyWatchColors.BorderSubtle),
            ) {
              Text(
                text = entry.keys,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                color = SkyWatchColors.RadarCyanBright,
              )
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          TextButton(onClick = onDismiss) {
            Text("Close (Esc)", color = SkyWatchColors.RadarCyan)
          }
        }
      }
    }
  }
}
