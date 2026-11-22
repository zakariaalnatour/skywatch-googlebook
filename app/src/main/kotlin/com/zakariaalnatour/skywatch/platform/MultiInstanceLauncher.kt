package com.zakariaalnatour.skywatch.platform

import android.content.Context
import android.content.Intent
import com.zakariaalnatour.skywatch.MainActivity

/**
 * Launches a new side-by-side multi-instance SkyWatch desktop window on Googlebook OS.
 */
object MultiInstanceLauncher {
  fun openNewWindow(
    context: Context,
    state: SkyWatchHandoffState,
  ) {
    val intent = Intent(context, MainActivity::class.java).apply {
      addFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK or
          Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
          Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT,
      )
      putExtras(HandoffStateCodec.toBundle(state))
    }
    context.startActivity(intent)
  }
}
