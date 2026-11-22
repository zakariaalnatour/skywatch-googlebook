package com.zakariaalnatour.skywatch.platform

import android.content.Context
import android.os.Build

/**
 * Abstraction over Android Build and PackageManager metadata so the Googlebook OS
 * exclusive runtime guard is 100% JVM unit-testable without Robolectric or device mocks.
 */
interface DeviceEnvironment {
  val sdkInt: Int
  val board: String
  val brand: String
  val device: String
  val hardware: String
  val model: String
  val product: String
  val fingerprint: String
  val manufacturer: String
  val host: String
  val systemFeatures: Set<String>

  fun hasSystemFeature(featureName: String): Boolean = systemFeatures.contains(featureName)
}

class AndroidDeviceEnvironment(context: Context) : DeviceEnvironment {
  private val packageManager = context.packageManager

  override val sdkInt: Int = Build.VERSION.SDK_INT
  override val board: String = Build.BOARD.orEmpty()
  override val brand: String = Build.BRAND.orEmpty()
  override val device: String = Build.DEVICE.orEmpty()
  override val hardware: String = Build.HARDWARE.orEmpty()
  override val model: String = Build.MODEL.orEmpty()
  override val product: String = Build.PRODUCT.orEmpty()
  override val fingerprint: String = Build.FINGERPRINT.orEmpty()
  override val manufacturer: String = Build.MANUFACTURER.orEmpty()
  override val host: String = Build.HOST.orEmpty()

  override val systemFeatures: Set<String> by lazy {
    packageManager.systemAvailableFeatures
      .mapNotNull { it.name }
      .toSet()
  }

  override fun hasSystemFeature(featureName: String): Boolean =
    packageManager.hasSystemFeature(featureName) || systemFeatures.contains(featureName)
}

sealed interface PlatformCheckResult {
  data object SupportedGooglebook : PlatformCheckResult

  data class Blocked(
    val title: String,
    val reason: String,
    val detectedMarkers: List<String>,
  ) : PlatformCheckResult
}

/**
 * Enforces Googlebook OS (Android 17 / API 37) exclusivity at runtime and blocks
 * ChromeOS ARCVM / ARC++ containers via `org.chromium.arc*` system features and
 * ARC build markers (`cheets`, `bertha`, `arcvm`).
 */
object GooglebookRuntimeGuard {
  const val MIN_GOOGLEBOOK_SDK: Int = 37
  const val ARC_FEATURE_PREFIX: String = "org.chromium.arc"

  private val KNOWN_ARC_PROBE_FEATURES: List<String> = listOf(
    "org.chromium.arc",
    "org.chromium.arc.device_management",
    "org.chromium.arc.payment_app",
    "org.chromium.arc.nmf",
  )

  private val ARC_BUILD_MARKERS: List<String> = listOf(
    "cheets",
    "bertha",
    "arcvm",
  )

  fun evaluate(environment: DeviceEnvironment): PlatformCheckResult {
    val detected = mutableListOf<String>()

    // 1. Inspect declared system features for any org.chromium.arc* entry
    environment.systemFeatures
      .filter { it.startsWith(ARC_FEATURE_PREFIX, ignoreCase = true) }
      .sorted()
      .forEach { detected += "feature:$it" }

    // Also explicitly probe key ARC features in case a container hides them from enumeration
    for (feature in KNOWN_ARC_PROBE_FEATURES) {
      if (environment.hasSystemFeature(feature) && "feature:$feature" !in detected) {
        detected += "feature:$feature"
      }
    }

    // 2. Inspect Build fields for ChromeOS ARCVM / ARC++ markers (cheets, bertha, arcvm)
    val buildFields = listOf(
      "board" to environment.board,
      "brand" to environment.brand,
      "device" to environment.device,
      "hardware" to environment.hardware,
      "model" to environment.model,
      "product" to environment.product,
      "fingerprint" to environment.fingerprint,
      "manufacturer" to environment.manufacturer,
      "host" to environment.host,
    )

    for ((fieldName, fieldValue) in buildFields) {
      val lower = fieldValue.lowercase()
      for (marker in ARC_BUILD_MARKERS) {
        if (lower.contains(marker)) {
          detected += "$fieldName:$marker"
        }
      }
    }

    if (detected.isNotEmpty()) {
      return PlatformCheckResult.Blocked(
        title = "ChromeOS / ARCVM Blocked",
        reason = "SkyWatch is built exclusively for Googlebook OS (Android 17 / API 37). " +
          "ChromeOS ARC/ARCVM runtime markers were detected on this device.",
        detectedMarkers = detected.distinct(),
      )
    }

    if (environment.sdkInt < MIN_GOOGLEBOOK_SDK) {
      return PlatformCheckResult.Blocked(
        title = "Googlebook OS (Android 17) Required",
        reason = "SkyWatch requires Android 17 (API $MIN_GOOGLEBOOK_SDK) or higher, " +
          "but detected API ${environment.sdkInt}.",
        detectedMarkers = listOf("sdk:${environment.sdkInt}"),
      )
    }

    return PlatformCheckResult.SupportedGooglebook
  }
}
