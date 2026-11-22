package com.zakariaalnatour.skywatch

import android.app.HandoffActivityData
import android.app.HandoffActivityDataRequestInfo
import android.app.HandoffActivityParams
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import com.zakariaalnatour.skywatch.data.LiveFlightRepository
import com.zakariaalnatour.skywatch.data.WidgetSnapshotStore
import com.zakariaalnatour.skywatch.platform.AndroidDeviceEnvironment
import com.zakariaalnatour.skywatch.platform.GooglebookRuntimeGuard
import com.zakariaalnatour.skywatch.platform.HandoffStateCodec
import com.zakariaalnatour.skywatch.platform.MultiInstanceLauncher
import com.zakariaalnatour.skywatch.platform.PlatformCheckResult
import com.zakariaalnatour.skywatch.ui.SkyWatchApp
import com.zakariaalnatour.skywatch.ui.components.UnsupportedPlatformScreen
import com.zakariaalnatour.skywatch.ui.theme.SkyWatchTheme
import com.zakariaalnatour.skywatch.viewmodel.SkyWatchViewModel
import com.zakariaalnatour.skywatch.widget.SkyWatchWidgetProvider

class MainActivity : ComponentActivity() {

  private lateinit var platformCheck: PlatformCheckResult

  private val viewModel: SkyWatchViewModel by viewModels {
    SkyWatchViewModel.Factory(
      repository = LiveFlightRepository(),
      onFeedUpdated = { hub, feed ->
        val snapshot = WidgetSnapshotStore.buildSnapshot(
          hub = hub,
          aircraft = feed.aircraft,
          source = feed.source,
          updatedAtMs = feed.timestampMs,
        )
        WidgetSnapshotStore.save(applicationContext, snapshot)
        SkyWatchWidgetProvider.notifyDataUpdated(applicationContext)
      },
      onContinueOnChanged = { enabled ->
        applyHandoffRegistration(enabled)
      },
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
    )

    platformCheck = GooglebookRuntimeGuard.evaluate(AndroidDeviceEnvironment(this))

    if (platformCheck is PlatformCheckResult.Blocked) {
      setHandoffEnabled(false, null)
      val blocked = platformCheck as PlatformCheckResult.Blocked
      setContent {
        SkyWatchTheme {
          UnsupportedPlatformScreen(
            blocked = blocked,
            onExitApp = { finishAndRemoveTask() },
          )
        }
      }
      return
    }

    // Opt into Android 17 Continue On (cross-device continuity with web fallback)
    applyHandoffRegistration(enabled = true)

    // Restore workspace state from Android 17 Continue On, Multi-Instance window, or App Widget tap
    if (savedInstanceState == null) {
      HandoffStateCodec.fromBundle(intent?.extras)?.let { handoffState ->
        viewModel.applyHandoffState(handoffState)
      }
    }

    setContent {
      SkyWatchTheme {
        SkyWatchApp(
          viewModel = viewModel,
          onOpenNewWindow = { state ->
            MultiInstanceLauncher.openNewWindow(this, state)
          },
          modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    if (platformCheck is PlatformCheckResult.SupportedGooglebook) {
      HandoffStateCodec.fromBundle(intent.extras)?.let { handoffState ->
        viewModel.applyHandoffState(handoffState)
      }
    }
  }

  /**
   * Android 17 (API 37) Continue On callback: exports live SkyWatch hub, map viewport,
   * selected aircraft, and web fallback URL to nearby Googlebook / Android devices.
   */
  override fun onHandoffActivityDataRequested(
    requestInfo: HandoffActivityDataRequestInfo,
  ): HandoffActivityData {
    val handoffState = viewModel.uiState.value.toHandoffState()
    val fallbackUri = Uri.parse(HandoffStateCodec.buildFallbackUriString(handoffState))

    return HandoffActivityData.Builder(ComponentName(this, MainActivity::class.java))
      .setExtras(HandoffStateCodec.toPersistableBundle(handoffState))
      .setFallbackUri(fallbackUri)
      .build()
  }

  private fun applyHandoffRegistration(enabled: Boolean) {
    if (platformCheck !is PlatformCheckResult.SupportedGooglebook) return
    val params = HandoffActivityParams.Builder()
      .setAllowHandoffWithoutPackageInstalled(true)
      .build()
    setHandoffEnabled(enabled, params)
  }
}
