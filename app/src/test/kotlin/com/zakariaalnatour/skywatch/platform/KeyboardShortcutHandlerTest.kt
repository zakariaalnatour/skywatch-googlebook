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
    assertNull(
      KeyboardShortcutHandler.mapKeyCode(
        keyCode = KeyEvent.KEYCODE_SLASH,
        isCtrlPressed = false,
        isShiftPressed = false,
        isAltPressed = false,
        isSearchFieldFocused = true,
        unicodeChar = '-'.code,
      ),
    )
  }

  @Test
  fun mapsGermanQwertzZoomAndSymbolKeysViaUnicodeCharBeforePhysicalKeycodes() {
    // German QWERTZ '+' sits on physical US KEYCODE_RIGHT_BRACKET
    assertEquals(
      SkyWatchShortcutAction.ZoomIn,
      KeyboardShortcutHandler.mapKeyCode(
        keyCode = KeyEvent.KEYCODE_RIGHT_BRACKET,
        isCtrlPressed = false,
        isShiftPressed = false,
        isAltPressed = false,
        isSearchFieldFocused = false,
        unicodeChar = '+'.code,
      ),
    )
    // German QWERTZ '-' sits on physical US KEYCODE_SLASH (must zoom out, NOT focus search)
    assertEquals(
      SkyWatchShortcutAction.ZoomOut,
      KeyboardShortcutHandler.mapKeyCode(
        keyCode = KeyEvent.KEYCODE_SLASH,
        isCtrlPressed = false,
        isShiftPressed = false,
        isAltPressed = false,
        isSearchFieldFocused = false,
        unicodeChar = '-'.code,
      ),
    )
    // German QWERTZ '=' sits on Shift + 0
    assertEquals(
      SkyWatchShortcutAction.ZoomIn,
      KeyboardShortcutHandler.mapKeyCode(
        keyCode = KeyEvent.KEYCODE_0,
        isCtrlPressed = false,
        isShiftPressed = true,
        isAltPressed = false,
        isSearchFieldFocused = false,
        unicodeChar = '='.code,
      ),
    )
    // German QWERTZ '?' sits on Shift + ß (physical KEYCODE_MINUS -> must open help, NOT zoom out)
    assertEquals(
      SkyWatchShortcutAction.ToggleShortcutsHelp,
      KeyboardShortcutHandler.mapKeyCode(
        keyCode = KeyEvent.KEYCODE_MINUS,
        isCtrlPressed = false,
        isShiftPressed = true,
        isAltPressed = false,
        isSearchFieldFocused = false,
        unicodeChar = '?'.code,
      ),
    )
    // German QWERTZ '/' sits on Shift + 7 (physical KEYCODE_7 -> must focus search, NOT jump hub 7)
    assertEquals(
      SkyWatchShortcutAction.FocusSearch,
      KeyboardShortcutHandler.mapKeyCode(
        keyCode = KeyEvent.KEYCODE_7,
        isCtrlPressed = false,
        isShiftPressed = true,
        isAltPressed = false,
        isSearchFieldFocused = false,
        unicodeChar = '/'.code,
      ),
    )
    // Ctrl + QWERTZ '+' / '-' also zoom cleanly
    assertEquals(
      SkyWatchShortcutAction.ZoomIn,
      KeyboardShortcutHandler.mapKeyCode(
        keyCode = KeyEvent.KEYCODE_RIGHT_BRACKET,
        isCtrlPressed = true,
        isShiftPressed = false,
        isAltPressed = false,
        isSearchFieldFocused = false,
        unicodeChar = '+'.code,
      ),
    )
    assertEquals(
      SkyWatchShortcutAction.ZoomOut,
      KeyboardShortcutHandler.mapKeyCode(
        keyCode = KeyEvent.KEYCODE_SLASH,
        isCtrlPressed = true,
        isShiftPressed = false,
        isAltPressed = false,
        isSearchFieldFocused = false,
        unicodeChar = '-'.code,
      ),
    )
  }
}
