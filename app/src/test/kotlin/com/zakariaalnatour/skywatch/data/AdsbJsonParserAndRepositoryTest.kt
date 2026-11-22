package com.zakariaalnatour.skywatch.data

import com.zakariaalnatour.skywatch.model.FeedSource
import com.zakariaalnatour.skywatch.model.FlightHubs
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AdsbJsonParserAndRepositoryTest {

  private lateinit var fiServer: MockWebServer
  private lateinit var lolServer: MockWebServer
  private lateinit var dbServer: MockWebServer
  private lateinit var repository: LiveFlightRepository

  @Before
  fun setUp() {
    fiServer = MockWebServer().apply { start() }
    lolServer = MockWebServer().apply { start() }
    dbServer = MockWebServer().apply { start() }
    repository = LiveFlightRepository(
      httpClient = OkHttpClient(),
      adsbFiBaseUrl = fiServer.url("/api").toString().trimEnd('/'),
      adsbLolBaseUrl = lolServer.url("").toString().trimEnd('/'),
      adsbdbBaseUrl = dbServer.url("").toString().trimEnd('/'),
    )
  }

  @After
  fun tearDown() {
    fiServer.shutdown()
    lolServer.shutdown()
    dbServer.shutdown()
  }

  @Test
  fun parserHandlesAdsbFiAircraftArrayAndGroundAltitude() {
    val payload = """
      {
        "now": 1795378790.001,
        "aircraft": [
          {
            "hex": "8964c0",
            "flight": "ETD6JG  ",
            "r": "A6-BLP",
            "t": "B789",
            "desc": "BOEING 787-9 Dreamliner",
            "alt_baro": 19700,
            "gs": 388.8,
            "track": 126.63,
            "baro_rate": -1216,
            "squawk": "1045",
            "category": "A5",
            "lat": 24.878540,
            "lon": 53.588867
          },
          {
            "hex": "8960ee",
            "flight": "UAE2LT ",
            "r": "A6-EDM",
            "t": "A388",
            "alt_baro": "ground",
            "gs": 14.2,
            "track": 103.0,
            "squawk": "7700",
            "lat": 25.2532,
            "lon": 55.3657
          }
        ]
      }
    """.trimIndent()

    val list = AdsbJsonParser.parseAircraftList(payload)
    assertEquals(2, list.size)
    assertEquals("ETD6JG", list[0].callsign)
    assertEquals(19700, list[0].baroAltitudeFt)
    assertTrue(list[0].isWidebody)
    assertFalse(list[0].onGround)

    assertEquals("UAE2LT", list[1].callsign)
    assertTrue(list[1].onGround)
    assertEquals("GND", list[1].formattedAltitude)
    assertTrue(list[1].isEmergency)
    assertTrue(list[1].isWidebody)
  }

  @Test
  fun parserHandlesAdsbdbSuccessAndUnknownStringResponse() {
    val routeJson = """
      {
        "response": {
          "flightroute": {
            "callsign": "UAE1",
            "callsign_iata": "EK1",
            "airline": {"name": "Emirates", "icao": "UAE", "iata": "EK", "country": "United Arab Emirates", "country_iso": "AE"},
            "origin": {"iata_code": "DXB", "icao_code": "OMDB", "name": "Dubai International Airport", "municipality": "Dubai", "latitude": 25.2528, "longitude": 55.3644},
            "destination": {"iata_code": "LHR", "icao_code": "EGLL", "name": "London Heathrow Airport", "municipality": "London", "latitude": 51.4706, "longitude": -0.46194}
          }
        }
      }
    """.trimIndent()

    val route = AdsbJsonParser.parseAdsbdbRoute(routeJson)
    assertNotNull(route)
    assertEquals("Emirates", route!!.airline?.name)
    assertEquals("DXB", route.origin?.iataCode)
    assertEquals("LHR", route.destination?.iataCode)

    assertNull(AdsbJsonParser.parseAdsbdbRoute("""{"response":"unknown callsign"}"""))
    assertNull(AdsbJsonParser.parseAdsbdbAircraft("""{"response":"unknown aircraft"}"""))
  }

  @Test
  fun regionalHubUsesAdsbFiPrimaryWhenHealthy() = runTest {
    fiServer.enqueue(
      MockResponse().setBody(
        """{"aircraft":[{"hex":"8964c0","flight":"UAE1 ","t":"A388","alt_baro":38000,"gs":490,"track":305,"lat":25.3,"lon":55.2}]}""",
      ),
    )

    val result = repository.fetchHubFlights(FlightHubs.DXB)
    assertEquals(FeedSource.ADSB_FI, result.source)
    assertFalse(result.failoverActive)
    assertEquals(1, result.aircraft.size)
    assertEquals(1, fiServer.requestCount)
    assertEquals(0, lolServer.requestCount)
  }

  @Test
  fun regionalHubAutomaticallyFailsOverToAdsbLolWhenAdsbFiErrors() = runTest {
    fiServer.enqueue(MockResponse().setResponseCode(503))
    lolServer.enqueue(
      MockResponse().setBody(
        """{"ac":[{"hex":"06a123","flight":"QTR101 ","t":"A35K","alt_baro":39000,"gs":502,"track":295,"lat":25.4,"lon":51.5}]}""",
      ),
    )

    val result = repository.fetchHubFlights(FlightHubs.DOH)
    assertEquals(FeedSource.ADSB_LOL_FAILOVER, result.source)
    assertTrue(result.failoverActive)
    assertEquals(1, result.aircraft.size)
    assertEquals("QTR101", result.aircraft.first().callsign)
  }

  @Test
  fun militaryFeedQueriesAdsbLolV2Mil() = runTest {
    lolServer.enqueue(
      MockResponse().setBody(
        """{"ac":[{"hex":"ae2126","flight":"C2702   ","r":"07-27011","t":"C27J","dbFlags":1,"alt_baro":14000,"gs":240,"track":143,"lat":31.5,"lon":36.1}]}""",
      ),
    )

    val result = repository.fetchHubFlights(FlightHubs.MIL)
    assertEquals(FeedSource.ADSB_LOL_MIL, result.source)
    assertEquals(1, result.aircraft.size)
    assertTrue(result.aircraft.first().isMilitary)
    assertEquals("/v2/mil", lolServer.takeRequest().path)
  }

  @Test
  fun enrichmentFetchesAndCachesRouteAndPhotoFromAdsbdb() = runTest {
    dbServer.dispatcher = object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
        "/v0/callsign/UAE1" -> MockResponse().setBody(
          """{"response":{"flightroute":{"callsign":"UAE1","callsign_iata":"EK1","airline":{"name":"Emirates"},"origin":{"iata_code":"DXB","name":"Dubai Intl","latitude":25.25,"longitude":55.36},"destination":{"iata_code":"LHR","name":"Heathrow","latitude":51.47,"longitude":-0.46}}}}""",
        )
        "/v0/aircraft/8964c0" -> MockResponse().setBody(
          """{"response":{"aircraft":{"mode_s":"8964C0","registration":"A6-BLP","manufacturer":"Boeing","type":"787-9","icao_type":"B789","registered_owner":"Etihad Airways","url_photo":"https://image.airport-data.com/aircraft/001686729.jpg"}}}""",
        )
        else -> MockResponse().setResponseCode(404)
      }
    }

    val first = repository.fetchEnrichment(hex = "8964c0", callsign = "UAE1")
    val second = repository.fetchEnrichment(hex = "8964c0", callsign = "UAE1")

    assertEquals(2, dbServer.requestCount)
    assertEquals("DXB", first.route?.origin?.iataCode)
    assertEquals(first.route?.origin?.iataCode, second.route?.origin?.iataCode)
    assertEquals("https://image.airport-data.com/aircraft/001686729.jpg", first.aircraftMetadata?.photoUrl)
  }

  @Test
  fun widebodyFeedAggregatesAndDeduplicatesHeavyTypes() = runTest {
    repeat(FlightHubs.WIDEBODY_QUERY_TYPES.size) { idx ->
      val hex = if (idx == 0 || idx == 1) "8964c0" else "8964c$idx"
      lolServer.enqueue(
        MockResponse().setBody(
          """{"ac":[{"hex":"$hex","flight":"UAE$idx ","t":"A388","alt_baro":38000,"gs":490,"track":300,"lat":25.3,"lon":55.2}]}""",
        ),
      )
    }

    val result = repository.fetchHubFlights(FlightHubs.WIDEBODY)
    assertEquals(FeedSource.ADSB_LOL_WIDEBODY, result.source)
    assertEquals(FlightHubs.WIDEBODY_QUERY_TYPES.size - 1, result.aircraft.size)
  }

  @Test
  fun globalSearchRoutesHexSquawkRegistrationAndCallsign() = runTest {
    val hexUrls = repository.buildGlobalSearchUrls("8964C0", "8964C0")
    assertTrue(hexUrls.first().endsWith("/v2/hex/8964c0"))

    val squawkUrls = repository.buildGlobalSearchUrls("7700", "7700")
    assertTrue(squawkUrls.first().endsWith("/v2/squawk/7700"))

    val regUrls = repository.buildGlobalSearchUrls("A6-BLP", "A6-BLP")
    assertTrue(regUrls.first().endsWith("/v2/reg/A6-BLP"))

    lolServer.enqueue(
      MockResponse().setBody(
        """{"ac":[{"hex":"8964c0","flight":"UAE1 ","r":"A6-EUL","t":"A388","alt_baro":38000,"gs":490,"track":305,"lat":25.3,"lon":55.2}]}""",
      ),
    )
    val found = repository.searchGlobal("UAE1")
    assertEquals(FeedSource.ADSB_LOL_SEARCH, found.source)
    assertEquals(1, found.aircraft.size)
    assertEquals("/v2/callsign/UAE1", lolServer.takeRequest().path)
  }
}
