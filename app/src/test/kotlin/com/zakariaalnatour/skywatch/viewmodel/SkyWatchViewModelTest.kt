package com.zakariaalnatour.skywatch.viewmodel

import com.zakariaalnatour.skywatch.data.FlightRepository
import com.zakariaalnatour.skywatch.model.Aircraft
import com.zakariaalnatour.skywatch.model.AircraftFilter
import com.zakariaalnatour.skywatch.model.AircraftMetadata
import com.zakariaalnatour.skywatch.model.AirlineInfo
import com.zakariaalnatour.skywatch.model.AirportInfo
import com.zakariaalnatour.skywatch.model.FeedSource
import com.zakariaalnatour.skywatch.model.FlightEnrichment
import com.zakariaalnatour.skywatch.model.FlightFeedResult
import com.zakariaalnatour.skywatch.model.FlightHub
import com.zakariaalnatour.skywatch.model.FlightHubs
import com.zakariaalnatour.skywatch.model.FlightRoute
import com.zakariaalnatour.skywatch.model.MapTileLayer
import com.zakariaalnatour.skywatch.platform.SkyWatchHandoffState
import com.zakariaalnatour.skywatch.platform.SkyWatchShortcutAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SkyWatchViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  private val sampleDxbFlight = Aircraft(
    hex = "8964c0", callsign = "UAE1", registration = "A6-EUL", aircraftType = "A388",
    description = "AIRBUS A380-800", latitude = 25.35, longitude = 55.25,
    baroAltitudeFt = 38000, geomAltitudeFt = 38400, onGround = false,
    groundSpeedKt = 492.0, iasKt = 285, tasKt = 485, mach = 0.85,
    trackDeg = 312.0, magHeadingDeg = 310.0, baroRateFpm = 0, squawk = "2201",
    emergency = "none", category = "A5", dbFlags = 0, seenSec = 0.2, rssiDb = -18.5,
    navAltitudeMcpFt = 38000, navQnhHpa = 1013.2, distanceNm = 12.0,
  )

  private val sampleMilFlight = Aircraft(
    hex = "ae2126", callsign = "RCH401", registration = "07-7171", aircraftType = "C17",
    description = "BOEING C-17 Globemaster III", latitude = 25.10, longitude = 51.55,
    baroAltitudeFt = 29000, geomAltitudeFt = 29300, onGround = false,
    groundSpeedKt = 435.0, iasKt = 270, tasKt = 430, mach = 0.76,
    trackDeg = 280.0, magHeadingDeg = 279.0, baroRateFpm = 512, squawk = "4412",
    emergency = "none", category = "A5", dbFlags = 1, seenSec = 0.4, rssiDb = -21.0,
    navAltitudeMcpFt = 32000, navQnhHpa = 1013.2, distanceNm = 18.0,
  )

  private class FakeFlightRepository(
    var hubAircraft: List<Aircraft>,
    var globalAircraft: List<Aircraft> = hubAircraft,
  ) : FlightRepository {
    var lastHubRequested: FlightHub? = null
    var lastGlobalQuery: String? = null

    override suspend fun fetchHubFlights(hub: FlightHub): FlightFeedResult {
      lastHubRequested = hub
      return FlightFeedResult(
        aircraft = hubAircraft,
        source = FeedSource.ADSB_FI,
        latencyMs = 42L,
        timestampMs = 1_795_378_800_000L,
      )
    }

    override suspend fun searchGlobal(query: String): FlightFeedResult {
      lastGlobalQuery = query
      return FlightFeedResult(
        aircraft = globalAircraft,
        source = FeedSource.ADSB_LOL_SEARCH,
        latencyMs = 55L,
        timestampMs = 1_795_378_805_000L,
      )
    }

    override suspend fun fetchEnrichment(hex: String, callsign: String?): FlightEnrichment {
      return FlightEnrichment(
        hex = hex,
        route = FlightRoute(
          callsign = callsign ?: "UAE1",
          callsignIata = "EK1",
          airline = AirlineInfo("Emirates", "EK", "UAE", "United Arab Emirates", "AE"),
          origin = AirportInfo("DXB", "OMDB", "Dubai Intl", "Dubai", "UAE", "AE", 25.2528, 55.3644),
          destination = AirportInfo("LHR", "EGLL", "London Heathrow", "London", "UK", "GB", 51.4706, -0.46194),
        ),
        aircraftMetadata = AircraftMetadata(
          modeS = hex, registration = "A6-EUL", manufacturer = "Airbus",
          typeDescription = "A380-861", icaoType = "A388", registeredOwner = "Emirates",
          countryName = "UAE", countryIso = "AE",
          photoUrl = "https://image.airport-data.com/aircraft/example.jpg",
          thumbnailUrl = null,
        ),
      )
    }
  }

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun allFourteenRequiredHubsArePresent() {
    val ids = FlightHubs.ALL.map { it.id }.toSet()
    val required = setOf(
      "DXB", "DOH", "RUH", "JED", "AMM", "CAI", "IST",
      "SFO", "JFK", "LAX", "LHR", "HND",
      "WIDEBODY", "MIL",
    )
    assertEquals(required, ids)
  }

  @Test
  fun selectHubLoadsFlightsFiltersAndEnrichesSelection() = runTest {
    val repo = FakeFlightRepository(listOf(sampleDxbFlight, sampleMilFlight))
    val vm = SkyWatchViewModel(repository = repo, autoStartPolling = false)

    vm.selectHub(FlightHubs.DXB)
    advanceUntilIdle()

    assertEquals("DXB", repo.lastHubRequested?.id)
    assertEquals(2, vm.uiState.value.allAircraft.size)

    // Filter to military only
    vm.setFilter(AircraftFilter.MILITARY)
    assertEquals(1, vm.uiState.value.filteredAircraft.size)
    assertEquals("ae2126", vm.uiState.value.filteredAircraft.first().hex)

    // Reset filter and select UAE1 -> triggers adsbdb enrichment
    vm.setFilter(AircraftFilter.ALL)
    vm.selectAircraft("8964c0", centerOnAircraft = true)
    advanceUntilIdle()

    val state = vm.uiState.value
    assertEquals("8964c0", state.selectedHex)
    assertNotNull(state.selectedEnrichment?.route)
    assertEquals("LHR", state.selectedEnrichment?.route?.destination?.iataCode)
    assertEquals(25.35, state.viewport.centerLat, 1e-4)
  }

  @Test
  fun applyHandoffStateRestoresHubLayerSelectionAndExportMatches() = runTest {
    val repo = FakeFlightRepository(listOf(sampleDxbFlight))
    val vm = SkyWatchViewModel(repository = repo, autoStartPolling = false)

    val incoming = SkyWatchHandoffState(
      hubId = "IST",
      selectedHex = "8964c0",
      centerLat = 41.2753,
      centerLon = 28.7519,
      zoom = 8.0f,
      tileLayer = MapTileLayer.OPEN_STREET_MAP,
      showTrails = false,
      showGreatCircle = true,
      followSelected = false,
    )

    vm.applyHandoffState(incoming)
    advanceUntilIdle()

    val exported = vm.uiState.value.toHandoffState()
    assertEquals("IST", exported.hubId)
    assertEquals("8964c0", exported.selectedHex)
    assertEquals(MapTileLayer.OPEN_STREET_MAP, exported.tileLayer)
    assertEquals(false, exported.showTrails)
    assertEquals(8.0f, exported.zoom, 0.01f)
  }

  @Test
  fun shortcutsToggleMapLayerAndJumpHubs() = runTest {
    val repo = FakeFlightRepository(listOf(sampleDxbFlight))
    val vm = SkyWatchViewModel(repository = repo, autoStartPolling = false)

    vm.handleShortcut(SkyWatchShortcutAction.ToggleMapLayer)
    assertEquals(MapTileLayer.OPEN_STREET_MAP, vm.uiState.value.mapTileLayer)

    vm.handleShortcut(SkyWatchShortcutAction.SelectHubByDigit(2))
    advanceUntilIdle()
    assertEquals("DOH", vm.uiState.value.activeHub.id)
    assertTrue(vm.uiState.value.allAircraft.isNotEmpty())
  }

  @Test
  fun followSelectedTracksAircraftMovementAcrossRefreshes() = runTest {
    val moved = sampleDxbFlight.copy(latitude = 26.15, longitude = 54.40)
    val repo = FakeFlightRepository(listOf(sampleDxbFlight))
    val vm = SkyWatchViewModel(repository = repo, autoStartPolling = false)

    vm.selectHub(FlightHubs.DXB)
    advanceUntilIdle()
    vm.selectAircraft(sampleDxbFlight.hex, centerOnAircraft = true)
    advanceUntilIdle()
    vm.toggleFollowSelected()
    assertTrue(vm.uiState.value.followSelected)

    repo.hubAircraft = listOf(moved)
    vm.refreshNow(silent = true)
    advanceUntilIdle()

    assertEquals(26.15, vm.uiState.value.viewport.centerLat, 1e-4)
    assertEquals(54.40, vm.uiState.value.viewport.centerLon, 1e-4)
    assertTrue((vm.uiState.value.trails[sampleDxbFlight.hex]?.size ?: 0) >= 2)
  }
}
