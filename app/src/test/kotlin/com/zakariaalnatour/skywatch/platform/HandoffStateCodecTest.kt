package com.zakariaalnatour.skywatch.platform

import com.zakariaalnatour.skywatch.model.AircraftFilter
import com.zakariaalnatour.skywatch.model.FlightHubs
import com.zakariaalnatour.skywatch.model.MapTileLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HandoffStateCodecTest {

  @Test
  fun roundTripsFullHandoffStateThroughStringMap() {
    val original = SkyWatchHandoffState(
      hubId = "DOH",
      selectedHex = "8964c0",
      centerLat = 25.2731,
      centerLon = 51.6081,
      zoom = 8.25f,
      searchQuery = "QTR",
      filter = AircraftFilter.WIDEBODY,
      tileLayer = MapTileLayer.OPEN_STREET_MAP,
      showTrails = false,
      showGreatCircle = true,
      followSelected = true,
    )

    val map = HandoffStateCodec.toStringMap(original)
    val restored = HandoffStateCodec.fromStringMap(map)

    assertNotNull(restored)
    assertEquals("DOH", restored!!.hubId)
    assertEquals("8964c0", restored.selectedHex)
    assertEquals(25.2731, restored.centerLat, 0.001)
    assertEquals(51.6081, restored.centerLon, 0.001)
    assertEquals(8.25f, restored.zoom, 0.01f)
    assertEquals("QTR", restored.searchQuery)
    assertEquals(AircraftFilter.WIDEBODY, restored.filter)
    assertEquals(MapTileLayer.OPEN_STREET_MAP, restored.tileLayer)
    assertEquals(false, restored.showTrails)
    assertEquals(true, restored.showGreatCircle)
    assertEquals(true, restored.followSelected)
  }

  @Test
  fun emptyMapReturnsNull() {
    assertNull(HandoffStateCodec.fromStringMap(emptyMap()))
  }

  @Test
  fun unknownHubFallsBackToDefaultDxbAndClampsZoom() {
    val restored = HandoffStateCodec.fromStringMap(
      mapOf(
        HandoffStateCodec.KEY_HUB_ID to "UNKNOWN_HUB",
        HandoffStateCodec.KEY_ZOOM to "99.0",
        HandoffStateCodec.KEY_CENTER_LAT to "120.0",
      ),
    )
    assertNotNull(restored)
    assertEquals(FlightHubs.DEFAULT.id, restored!!.hubId)
    assertEquals(14.0f, restored.zoom, 0.001f)
    assertEquals(85.0, restored.centerLat, 0.001)
  }

  @Test
  fun buildFallbackUriIncludesIcaoWhenAircraftSelected() {
    val state = SkyWatchHandoffState(
      hubId = "DXB",
      selectedHex = "8964C0",
      centerLat = 25.2532,
      centerLon = 55.3657,
      zoom = 7.2f,
    )
    val uri = HandoffStateCodec.buildFallbackUriString(state)
    assertTrue(uri.startsWith("https://globe.adsb.fi/?icao=8964c0"))
    assertTrue(uri.contains("lat=25.2532"))
    assertTrue(uri.contains("lon=55.3657"))
  }

  @Test
  fun buildFallbackUriUsesViewportWhenNoAircraftSelected() {
    val state = SkyWatchHandoffState(
      hubId = "RUH",
      selectedHex = null,
      centerLat = 24.9576,
      centerLon = 46.6988,
      zoom = 7.0f,
    )
    val uri = HandoffStateCodec.buildFallbackUriString(state)
    assertEquals("https://globe.adsb.fi/?lat=24.9576&lon=46.6988&zoom=7.0", uri)
  }
}
