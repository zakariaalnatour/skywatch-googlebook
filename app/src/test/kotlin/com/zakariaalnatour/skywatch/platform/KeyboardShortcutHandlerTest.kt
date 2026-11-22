package com.zakariaalnatour.skywatch.platform

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeyboardShortcutHandlerTest {

  @Test
  fun mapsDesktopShortcutsWhenSearchUnfocused() {
    assertEquals(
      SkyWatchShortcutAction.FocusSearch,
      KeyboardShortcutHandler.mapKeyCode(KeyEvent.KEYCODE_K, isCtrlPressed = true, isShiftPressed = false, isAltPressed = false, isSearchFieldFocused = false),
    )
    assertEquals(
      SkyWatchShortcutAction.FocusSearch,
      KeyboardShortcutHandler.mapKeyCode(KeyEvent.KEYCODE_SLASH, isCtrlPressed = false, isShiftPressed = false, isAltPressed = false, isSearchFieldFocused = false),
    )
    assertEquals(
      SkyWatchShortcutAction.ToggleShortcutsHelp,
      KeyboardShortcutHandler.mapKeyCode(KeyEvent.KEYCODE_SLASH, isCtrlPressed = false, isShiftPressed = true, isAltPressed = false, isSearchFieldFocused = false),
    )
    assertEquals(
      SkyWatchShortcutAction.OpenNewWindow,
      KeyboardShortcutHandler.mapKeyCode(KeyEvent.KEYCODE_N, isCtrlPressed = true, isShiftPressed = false, isAltPressed = false, isSearchFieldFocused = false),
    )
    assertEquals(
      SkyWatchShortcutAction.RefreshFeed,
      KeyboardShortcutHandler.mapKeyCode(KeyEvent.KEYCODE_R, isCtrlPressed = false, isShiftPressed = false, isAltPressed = false, isSearchFieldFocused = false),
    )
    assertEquals(
      SkyWatchShortcutAction.ToggleMapLayer,
      KeyboardShortcutHandler.mapKeyCode(KeyEvent.KEYCODE_M, isCtrlPressed = false, isShiftPressed = false, isAltPressed = false, isSearchFieldFocused = false),
    )
    assertEquals(
      SkyWatchShortcutAction.SelectHubByDigit(1),
      KeyboardShortcutHandler.mapKeyCode(KeyEvent.KEYCODE_1, isCtrlPressed = false, isShiftPressed = false, isAltPressed = false, isSearchFieldFocused = false),
    )
    assertEquals(
      SkyWatchShortcutAction.SelectHubByDigit(9),
      KeyboardShortcutHandler.mapKeyCode(KeyEvent.KEYCODE_9, isCtrlPressed = false, isShiftPressed = false, isAltPressed = false, isSearchFieldFocused = false),
    )
  }

  @Test
  fun ignoresPlainKeysWhileTypingInSearchButAllowsEscapeAndCtrlShortcuts() {
    assertNull(
      KeyboardShortcutHandler.mapKeyCode(KeyEvent.KEYCODE_R, isCtrlPressed = false, isShiftPressed = false, isAltPressed = false, isSearchFieldFocused = true),
    )
    assertNull(
      KeyboardShortcutHandler.mapKeyCode(KeyEvent.KEYCODE_1, isCtrlPressed = false, isShiftPressed = false, isAltPressed = false, isSearchFieldFocused = true),
    )
    assertEquals(
      SkyWatchShortcutAction.ClearOrDismiss,
      KeyboardShortcutHandler.mapKeyCode(KeyEvent.KEYCODE_ESCAPE, isCtrlPressed = false, isShiftPressed = false, isAltPressed = false, isSearchFieldFocused = true),
    )
    assertEquals(
      SkyWatchShortcutAction.OpenNewWindow,
      KeyboardShortcutHandler.mapKeyCode(KeyEvent.KEYCODE_N, isCtrlPressed = true, isShiftPressed = false, isAltPressed = false, isSearchFieldFocused = true),
    )
  }
}
