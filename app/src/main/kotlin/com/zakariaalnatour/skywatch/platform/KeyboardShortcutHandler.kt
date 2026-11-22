package com.zakariaalnatour.skywatch.platform

import android.view.KeyEvent

sealed interface SkyWatchShortcutAction {
  data object FocusSearch : SkyWatchShortcutAction
  data object RefreshFeed : SkyWatchShortcutAction
  data object ToggleAutoRefresh : SkyWatchShortcutAction
  data object ZoomIn : SkyWatchShortcutAction
  data object ZoomOut : SkyWatchShortcutAction
  data class PanMap(val dxFraction: Float, val dyFraction: Float) : SkyWatchShortcutAction
  data object SelectNextFlight : SkyWatchShortcutAction
  data object SelectPreviousFlight : SkyWatchShortcutAction
  data object ToggleFollowAircraft : SkyWatchShortcutAction
  data object ToggleMapLayer : SkyWatchShortcutAction
  data object ToggleTrails : SkyWatchShortcutAction
  data object ToggleGreatCircle : SkyWatchShortcutAction
  data class SelectHubByDigit(val digit: Int) : SkyWatchShortcutAction
  data object OpenNewWindow : SkyWatchShortcutAction
  data object ToggleShortcutsHelp : SkyWatchShortcutAction
  data object ClearOrDismiss : SkyWatchShortcutAction
}

data class ShortcutDescriptor(
  val keys: String,
  val description: String,
)

/**
 * Pure JVM-testable desktop keyboard shortcut mapper for Googlebook hardware keyboards.
 */
object KeyboardShortcutHandler {
  val CHEAT_SHEET: List<ShortcutDescriptor> = listOf(
    ShortcutDescriptor("Ctrl + K  or  /", "Focus global flight search"),
    ShortcutDescriptor("R  or  F5", "Refresh live ADS-B feed now"),
    ShortcutDescriptor("Space", "Pause / resume live radar polling"),
    ShortcutDescriptor("+ / -", "Zoom map in / out (or trackpad scroll)"),
    ShortcutDescriptor("Arrow Keys", "Pan geographic radar viewport"),
    ShortcutDescriptor("J / K", "Select next / previous aircraft"),
    ShortcutDescriptor("F", "Toggle camera follow on selected aircraft"),
    ShortcutDescriptor("M", "Switch Esri Dark Gray ↔ OpenStreetMap"),
    ShortcutDescriptor("T / G", "Toggle live trails / great-circle route arc"),
    ShortcutDescriptor("1 – 7", "Jump to Middle East hubs (DXB..IST)"),
    ShortcutDescriptor("8 / 9", "Switch to Widebody / Military feed"),
    ShortcutDescriptor("Ctrl + N", "Open new multi-instance SkyWatch window"),
    ShortcutDescriptor("?  or  F1", "Show / hide keyboard shortcuts guide"),
    ShortcutDescriptor("Esc", "Clear selection / dismiss dialogs"),
  )

  fun mapKeyCode(
    keyCode: Int,
    isCtrlPressed: Boolean,
    isShiftPressed: Boolean,
    isAltPressed: Boolean,
    isSearchFieldFocused: Boolean,
  ): SkyWatchShortcutAction? {
    if (isAltPressed) return null

    if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
      return SkyWatchShortcutAction.ClearOrDismiss
    }

    if (isCtrlPressed) {
      return when (keyCode) {
        KeyEvent.KEYCODE_K -> SkyWatchShortcutAction.FocusSearch
        KeyEvent.KEYCODE_N -> SkyWatchShortcutAction.OpenNewWindow
        KeyEvent.KEYCODE_R -> SkyWatchShortcutAction.RefreshFeed
        KeyEvent.KEYCODE_EQUALS, KeyEvent.KEYCODE_PLUS -> SkyWatchShortcutAction.ZoomIn
        KeyEvent.KEYCODE_MINUS -> SkyWatchShortcutAction.ZoomOut
        else -> null
      }
    }

    // Do not hijack letter/digit typing while user is entering a search query
    if (isSearchFieldFocused) {
      return null
    }

    return when (keyCode) {
      KeyEvent.KEYCODE_SLASH -> {
        if (isShiftPressed) SkyWatchShortcutAction.ToggleShortcutsHelp
        else SkyWatchShortcutAction.FocusSearch
      }
      KeyEvent.KEYCODE_F1 -> SkyWatchShortcutAction.ToggleShortcutsHelp
      KeyEvent.KEYCODE_F5, KeyEvent.KEYCODE_R -> SkyWatchShortcutAction.RefreshFeed
      KeyEvent.KEYCODE_SPACE -> SkyWatchShortcutAction.ToggleAutoRefresh
      KeyEvent.KEYCODE_PLUS, KeyEvent.KEYCODE_EQUALS, KeyEvent.KEYCODE_NUMPAD_ADD ->
        SkyWatchShortcutAction.ZoomIn
      KeyEvent.KEYCODE_MINUS, KeyEvent.KEYCODE_NUMPAD_SUBTRACT ->
        SkyWatchShortcutAction.ZoomOut
      KeyEvent.KEYCODE_DPAD_UP -> SkyWatchShortcutAction.PanMap(0f, -0.18f)
      KeyEvent.KEYCODE_DPAD_DOWN -> SkyWatchShortcutAction.PanMap(0f, 0.18f)
      KeyEvent.KEYCODE_DPAD_LEFT -> SkyWatchShortcutAction.PanMap(-0.18f, 0f)
      KeyEvent.KEYCODE_DPAD_RIGHT -> SkyWatchShortcutAction.PanMap(0.18f, 0f)
      KeyEvent.KEYCODE_J -> SkyWatchShortcutAction.SelectNextFlight
      KeyEvent.KEYCODE_K -> SkyWatchShortcutAction.SelectPreviousFlight
      KeyEvent.KEYCODE_F -> SkyWatchShortcutAction.ToggleFollowAircraft
      KeyEvent.KEYCODE_M -> SkyWatchShortcutAction.ToggleMapLayer
      KeyEvent.KEYCODE_T -> SkyWatchShortcutAction.ToggleTrails
      KeyEvent.KEYCODE_G -> SkyWatchShortcutAction.ToggleGreatCircle
      KeyEvent.KEYCODE_1 -> SkyWatchShortcutAction.SelectHubByDigit(1)
      KeyEvent.KEYCODE_2 -> SkyWatchShortcutAction.SelectHubByDigit(2)
      KeyEvent.KEYCODE_3 -> SkyWatchShortcutAction.SelectHubByDigit(3)
      KeyEvent.KEYCODE_4 -> SkyWatchShortcutAction.SelectHubByDigit(4)
      KeyEvent.KEYCODE_5 -> SkyWatchShortcutAction.SelectHubByDigit(5)
      KeyEvent.KEYCODE_6 -> SkyWatchShortcutAction.SelectHubByDigit(6)
      KeyEvent.KEYCODE_7 -> SkyWatchShortcutAction.SelectHubByDigit(7)
      KeyEvent.KEYCODE_8 -> SkyWatchShortcutAction.SelectHubByDigit(8)
      KeyEvent.KEYCODE_9 -> SkyWatchShortcutAction.SelectHubByDigit(9)
      else -> null
    }
  }
}
