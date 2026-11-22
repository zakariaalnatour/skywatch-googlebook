package com.zakariaalnatour.skywatch.geo

import com.zakariaalnatour.skywatch.model.Aircraft
import com.zakariaalnatour.skywatch.model.FlightHubs
import com.zakariaalnatour.skywatch.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoProjectionAndGreatCircleTest {

  @Test
  fun esriUrlsUseZYXOrderAndOsmUsesZXYOrder() {
    val base = GeoProjection.esriDarkGrayBaseUrl(z = 6, x = 41, y = 27)
    val ref = GeoProjection.esriDarkGrayReferenceUrl(z = 6, x = 41, y = 27)
    val osm = GeoProjection.openStreetMapUrl(z = 6, x = 41, y = 27)

    assertEquals(
      "https://server.arcgisonline.com/arcgis/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/6/27/41",
      base,
    )
    assertEquals(
      "https://server.arcgisonline.com/arcgis/rest/services/Canvas/World_Dark_Gray_Reference/MapServer/tile/6/27/41",
      ref,
    )
    assertEquals(
      "https://tile.openstreetmap.org/6/41/27.png",
      osm,
    )
  }

  @Test
  fun webMercatorRoundTripIsAccurate() {
    val points = listOf(
      GeoPoint(FlightHubs.DXB.latitude, FlightHubs.DXB.longitude),
      GeoPoint(FlightHubs.SFO.latitude, FlightHubs.SFO.longitude),
      GeoPoint(FlightHubs.HND.latitude, FlightHubs.HND.longitude),
      GeoPoint(FlightHubs.LHR.latitude, FlightHubs.LHR.longitude),
    )

    for (pt in points) {
      val world = GeoProjection.latLonToWorld(pt.latitude, pt.longitude, zoom = 7.5f)
      val back = GeoProjection.worldToLatLon(world.x, world.y, zoom = 7.5f)
      assertEquals(pt.latitude, back.latitude, 1e-4)
      assertEquals(pt.longitude, back.longitude, 1e-4)
    }
  }

  @Test
  fun zoomAroundScreenPointPreservesGeoUnderPointer() {
    val vp = ViewportState(centerLat = 25.2532, centerLon = 55.3657, zoom = 7.0f)
    val anchorX = 640f
    val anchorY = 220f
    val beforeGeo = GeoProjection.screenToLatLon(anchorX, anchorY, vp, 1000f, 700f)

    val zoomed = GeoProjection.zoomAroundScreenPoint(vp, 8.5f, anchorX, anchorY, 1000f, 700f)
    val afterGeo = GeoProjection.screenToLatLon(anchorX, anchorY, zoomed, 1000f, 700f)

    assertEquals(8.5f, zoomed.zoom, 0.001f)
    assertEquals(beforeGeo.latitude, afterGeo.latitude, 1e-3)
    assertEquals(beforeGeo.longitude, afterGeo.longitude, 1e-3)
  }

  @Test
  fun greatCircleDistanceDxbToLhrMatchesRealAviationDistance() {
    val dxb = GeoPoint(25.2528, 55.3644)
    val lhr = GeoPoint(51.4706, -0.46194)
    val distNm = GreatCircleCalculator.distanceNm(dxb, lhr)
    // Actual DXB-LHR great-circle distance is ~2,968 NM
    assertTrue("Expected ~2968 NM but got $distNm", distNm in 2940.0..2995.0)
  }

  @Test
  fun greatCircleArcSplitsCleanlyAcrossAntimeridian() {
    val sfo = GeoPoint(37.6213, -122.3790)
    val hnd = GeoPoint(35.5494, 139.7798)
    val arc = GreatCircleCalculator.greatCircleArc(sfo, hnd, segments = 64)
    assertEquals(65, arc.size)
    assertEquals(sfo.latitude, arc.first().latitude, 0.01)
    assertEquals(hnd.latitude, arc.last().latitude, 0.01)

    val split = GreatCircleCalculator.splitSegmentsAtAntimeridian(arc)
    assertEquals("Trans-Pacific arc should split into 2 segments at dateline", 2, split.size)
  }

  @Test
  fun trailStoreAppendsMovementCapsLengthAndEvictsStale() {
    val store = TrailStore(maxPointsPerAircraft = 3, staleAfterMs = 10_000L, minMovementNm = 0.5)
    fun ac(lat: Double, lon: Double) = Aircraft(
      hex = "8964c0", callsign = "UAE1", registration = "A6-EUL", aircraftType = "A388",
      description = null, latitude = lat, longitude = lon, baroAltitudeFt = 36000,
      geomAltitudeFt = 36500, onGround = false, groundSpeedKt = 490.0, iasKt = 290, tasKt = 480,
      mach = 0.84, trackDeg = 310.0, magHeadingDeg = 308.0, baroRateFpm = 0, squawk = "2000",
      emergency = "none", category = "A5", dbFlags = 0, seenSec = 0.1, rssiDb = -20.0,
      navAltitudeMcpFt = 36000, navQnhHpa = 1013.2, distanceNm = 10.0,
    )

    store.recordSnapshot(listOf(ac(25.00, 55.00)), nowMs = 1_000L)
    store.recordSnapshot(listOf(ac(25.00, 55.00)), nowMs = 2_000L) // no movement -> ignored
    store.recordSnapshot(listOf(ac(25.10, 55.10)), nowMs = 3_000L)
    store.recordSnapshot(listOf(ac(25.20, 55.20)), nowMs = 4_000L)
    val capped = store.recordSnapshot(listOf(ac(25.30, 55.30)), nowMs = 5_000L)

    assertEquals(3, capped["8964c0"]?.size)
    assertEquals(25.10, capped["8964c0"]!!.first().latitude, 1e-4)
    assertEquals(25.30, capped["8964c0"]!!.last().latitude, 1e-4)

    // Evicted once stale
    val evicted = store.recordSnapshot(emptyList(), nowMs = 20_000L)
    assertTrue(evicted.isEmpty())
  }
}
