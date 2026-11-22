package com.zakariaalnatour.skywatch.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GooglebookRuntimeGuardTest {

  private data class FakeDeviceEnvironment(
    override val sdkInt: Int = 37,
    override val board: String = "googlebook_board",
    override val brand: String = "google",
    override val device: String = "googlebook",
    override val hardware: String = "googlebook",
    override val model: String = "Googlebook Pro",
    override val product: String = "googlebook",
    override val fingerprint: String = "google/googlebook/googlebook:17/BP3A/123:user/release-keys",
    override val manufacturer: String = "Google",
    override val host: String = "abfarm-release",
    override val systemFeatures: Set<String> = setOf("android.software.app_widgets"),
  ) : DeviceEnvironment

  @Test
  fun allowsLegitimateGooglebookOsApi37() {
    val result = GooglebookRuntimeGuard.evaluate(FakeDeviceEnvironment())
    assertEquals(PlatformCheckResult.SupportedGooglebook, result)
  }

  @Test
  fun blocksOrgChromiumArcFeature() {
    val env = FakeDeviceEnvironment(
      systemFeatures = setOf("android.software.app_widgets", "org.chromium.arc"),
    )
    val result = GooglebookRuntimeGuard.evaluate(env)
    assertTrue(result is PlatformCheckResult.Blocked)
    val blocked = result as PlatformCheckResult.Blocked
    assertTrue(blocked.detectedMarkers.any { it.contains("org.chromium.arc") })
  }

  @Test
  fun blocksOrgChromiumArcSubfeaturePrefix() {
    val env = FakeDeviceEnvironment(
      systemFeatures = setOf("org.chromium.arc.device_management", "org.chromium.arc.custom_flag"),
    )
    val result = GooglebookRuntimeGuard.evaluate(env)
    assertTrue(result is PlatformCheckResult.Blocked)
    val blocked = result as PlatformCheckResult.Blocked
    assertTrue(blocked.detectedMarkers.contains("feature:org.chromium.arc.custom_flag"))
  }

  @Test
  fun blocksCheetsBuildMarker() {
    val env = FakeDeviceEnvironment(
      device = "cheets_x86_64",
      product = "cheets",
    )
    val result = GooglebookRuntimeGuard.evaluate(env)
    assertTrue(result is PlatformCheckResult.Blocked)
    val blocked = result as PlatformCheckResult.Blocked
    assertTrue(blocked.detectedMarkers.any { it.contains("cheets") })
  }

  @Test
  fun blocksBerthaBuildMarker() {
    val env = FakeDeviceEnvironment(
      hardware = "bertha",
      board = "bertha_arm64",
    )
    val result = GooglebookRuntimeGuard.evaluate(env)
    assertTrue(result is PlatformCheckResult.Blocked)
    val blocked = result as PlatformCheckResult.Blocked
    assertTrue(blocked.detectedMarkers.any { it.contains("bertha") })
  }

  @Test
  fun blocksArcvmBuildMarker() {
    val env = FakeDeviceEnvironment(
      fingerprint = "google/arcvm_x86_64/arcvm:15/R130/1:user/release-keys",
    )
    val result = GooglebookRuntimeGuard.evaluate(env)
    assertTrue(result is PlatformCheckResult.Blocked)
    val blocked = result as PlatformCheckResult.Blocked
    assertTrue(blocked.detectedMarkers.any { it.contains("arcvm") })
  }

  @Test
  fun blocksPreAndroid17SdkLevel() {
    val env = FakeDeviceEnvironment(sdkInt = 36)
    val result = GooglebookRuntimeGuard.evaluate(env)
    assertTrue(result is PlatformCheckResult.Blocked)
    val blocked = result as PlatformCheckResult.Blocked
    assertTrue(blocked.detectedMarkers.contains("sdk:36"))
  }
}
