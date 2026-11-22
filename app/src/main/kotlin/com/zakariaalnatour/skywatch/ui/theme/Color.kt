package com.zakariaalnatour.skywatch.ui.theme

import androidx.compose.ui.graphics.Color

object SkyWatchColors {
  val ObsidianBg = Color(0xFF070C14)
  val SurfacePanel = Color(0xFF0D1522)
  val SurfaceElevated = Color(0xFF131E30)
  val SurfaceCard = Color(0xFF162236)
  val SurfaceCardSelected = Color(0xFF1C3252)
  val BorderSubtle = Color(0xFF23344D)
  val BorderAccent = Color(0xFF38BDF8)

  val RadarCyan = Color(0xFF38BDF8)
  val RadarCyanBright = Color(0xFF7DD3FC)
  val EmeraldLive = Color(0xFF22C55E)
  val AmberLow = Color(0xFFF59E0B)
  val MilitaryGold = Color(0xFFFACC15)
  val VioletHigh = Color(0xFFA78BFA)
  val GroundSlate = Color(0xFF94A3B8)
  val AlertCrimson = Color(0xFFEF4444)

  val TextPrimary = Color(0xFFF1F5F9)
  val TextSecondary = Color(0xFF94A3B8)
  val TextMuted = Color(0xFF64748B)

  fun altitudeBandColor(
    altitudeFt: Int?,
    onGround: Boolean,
    isMilitary: Boolean,
    isEmergency: Boolean,
  ): Color = when {
    isEmergency -> AlertCrimson
    isMilitary -> MilitaryGold
    onGround -> GroundSlate
    altitudeFt == null -> RadarCyan
    altitudeFt < 10_000 -> AmberLow
    altitudeFt < 25_000 -> EmeraldLive
    altitudeFt < 38_000 -> RadarCyan
    else -> VioletHigh
  }
}
