package com.zakariaalnatour.skywatch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SkyWatchDarkScheme = darkColorScheme(
  primary = SkyWatchColors.RadarCyan,
  onPrimary = SkyWatchColors.ObsidianBg,
  primaryContainer = SkyWatchColors.SurfaceCardSelected,
  onPrimaryContainer = SkyWatchColors.RadarCyanBright,
  secondary = SkyWatchColors.MilitaryGold,
  onSecondary = SkyWatchColors.ObsidianBg,
  tertiary = SkyWatchColors.EmeraldLive,
  background = SkyWatchColors.ObsidianBg,
  onBackground = SkyWatchColors.TextPrimary,
  surface = SkyWatchColors.SurfacePanel,
  onSurface = SkyWatchColors.TextPrimary,
  surfaceVariant = SkyWatchColors.SurfaceCard,
  onSurfaceVariant = SkyWatchColors.TextSecondary,
  outline = SkyWatchColors.BorderSubtle,
  error = SkyWatchColors.AlertCrimson,
  onError = SkyWatchColors.TextPrimary,
)

@Composable
fun SkyWatchTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = SkyWatchDarkScheme,
    typography = SkyWatchTypography,
    content = content,
  )
}
