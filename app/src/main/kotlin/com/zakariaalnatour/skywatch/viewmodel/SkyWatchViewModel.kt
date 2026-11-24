package com.zakariaalnatour.skywatch.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zakariaalnatour.skywatch.data.FlightRepository
import com.zakariaalnatour.skywatch.data.LiveFlightRepository
import com.zakariaalnatour.skywatch.geo.GeoProjection
import com.zakariaalnatour.skywatch.geo.GreatCircleCalculator
import com.zakariaalnatour.skywatch.geo.TrailStore
import com.zakariaalnatour.skywatch.geo.ViewportState
import com.zakariaalnatour.skywatch.model.Aircraft
import com.zakariaalnatour.skywatch.model.AircraftFilter
import com.zakariaalnatour.skywatch.model.FeedSource
import com.zakariaalnatour.skywatch.model.FlightEnrichment
import com.zakariaalnatour.skywatch.model.FlightFeedResult
import com.zakariaalnatour.skywatch.model.FlightHub
import com.zakariaalnatour.skywatch.model.FlightHubs
import com.zakariaalnatour.skywatch.model.FlightSortOption
import com.zakariaalnatour.skywatch.model.GeoPoint
import com.zakariaalnatour.skywatch.model.MapTileLayer
import com.zakariaalnatour.skywatch.model.TrackPoint
import com.zakariaalnatour.skywatch.platform.SkyWatchHandoffState
import com.zakariaalnatour.skywatch.platform.SkyWatchShortcutAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SkyWatchUiState(
  val activeHub: FlightHub = FlightHubs.DEFAULT,
  val allAircraft: List<Aircraft> = emptyList(),
  val filteredAircraft: List<Aircraft> = emptyList(),
  val trails: Map<String, List<TrackPoint>> = emptyMap(),
  val selectedHex: String? = null,
  val hoveredHex: String? = null,
  val selectedEnrichment: FlightEnrichment? = null,
  val viewport: ViewportState = ViewportState(
    centerLat = FlightHubs.DEFAULT.latitude,
    centerLon = FlightHubs.DEFAULT.longitude,
    zoom = FlightHubs.DEFAULT.defaultZoom,
  ),
  val searchQuery: String = "",
  val isGlobalSearchMode: Boolean = false,
  val activeFilter: AircraftFilter = AircraftFilter.ALL,
  val sortOption: FlightSortOption = FlightSortOption.ALTITUDE_DESC,
  val mapTileLayer: MapTileLayer = MapTileLayer.ESRI_DARK_GRAY,
  val showTrails: Boolean = true,
  val showGreatCircle: Boolean = true,
  val followSelected: Boolean = false,
  val autoRefreshEnabled: Boolean = true,
  val continueOnEnabled: Boolean = true,
  val showShortcutsDialog: Boolean = false,
  val isLoading: Boolean = false,
  val activeFeedSource: FeedSource = FeedSource.ADSB_FI,
  val failoverActive: Boolean = false,
  val feedLatencyMs: Long? = null,
  val lastUpdatedMs: Long? = null,
  val statusBanner: String? = null,
  val errorMessage: String? = null,
) {
  val selectedAircraft: Aircraft?
    get() = selectedHex?.let { hex -> allAircraft.firstOrNull { it.hex == hex } }

  val widebodyCount: Int
    get() = allAircraft.count { it.isWidebody }

  val militaryCount: Int
    get() = allAircraft.count { it.isMilitary }

  val airborneCount: Int
    get() = allAircraft.count { !it.onGround }

  fun toHandoffState(): SkyWatchHandoffState = SkyWatchHandoffState(
    hubId = activeHub.id,
    selectedHex = selectedHex,
    centerLat = viewport.centerLat,
    centerLon = viewport.centerLon,
    zoom = viewport.zoom,
    searchQuery = searchQuery,
    filter = activeFilter,
    tileLayer = mapTileLayer,
    showTrails = showTrails,
    showGreatCircle = showGreatCircle,
    followSelected = followSelected,
  )
}

class SkyWatchViewModel(
  private val repository: FlightRepository = LiveFlightRepository(),
  private val trailStore: TrailStore = TrailStore(),
  private val pollIntervalMs: Long = 10_000L,
  private val autoStartPolling: Boolean = true,
  private val onFeedUpdated: ((FlightHub, FlightFeedResult) -> Unit)? = null,
  private val onContinueOnChanged: ((Boolean) -> Unit)? = null,
) : ViewModel() {

  private val _uiState = MutableStateFlow(SkyWatchUiState())
  val uiState: StateFlow<SkyWatchUiState> = _uiState.asStateFlow()

  private var pollingJob: Job? = null
  private var refreshJob: Job? = null
  private var enrichmentJob: Job? = null

  init {
    if (autoStartPolling) {
      startPollingLoop()
    }
  }

  fun startPollingLoop() {
    pollingJob?.cancel()
    pollingJob = viewModelScope.launch {
      refreshNow()
      while (isActive) {
        delay(pollIntervalMs)
        val state = _uiState.value
        if (state.autoRefreshEnabled && !state.isGlobalSearchMode) {
          refreshNow(silent = true)
        }
      }
    }
  }

  fun applyHandoffState(handoff: SkyWatchHandoffState) {
    val hub = FlightHubs.findById(handoff.hubId) ?: FlightHubs.DEFAULT
    _uiState.update { state ->
      val updated = state.copy(
        activeHub = hub,
        selectedHex = handoff.selectedHex,
        viewport = ViewportState(
          centerLat = handoff.centerLat,
          centerLon = handoff.centerLon,
          zoom = GeoProjection.clampZoom(handoff.zoom),
        ),
        searchQuery = handoff.searchQuery,
        isGlobalSearchMode = false,
        activeFilter = handoff.filter,
        mapTileLayer = handoff.tileLayer,
        showTrails = handoff.showTrails,
        showGreatCircle = handoff.showGreatCircle,
        followSelected = handoff.followSelected,
        errorMessage = null,
      )
      updated.copy(filteredAircraft = computeFilteredSorted(updated))
    }
    refreshNow(silent = false)
  }

  fun selectHub(hub: FlightHub) {
    val hubChanged = _uiState.value.activeHub.id != hub.id
    if (hubChanged) {
      trailStore.clear()
    }
    _uiState.update { state ->
      state.copy(
        activeHub = hub,
        isGlobalSearchMode = false,
        selectedHex = if (hubChanged) null else state.selectedHex,
        selectedEnrichment = if (hubChanged) null else state.selectedEnrichment,
        followSelected = false,
        viewport = ViewportState(
          centerLat = hub.latitude,
          centerLon = hub.longitude,
          zoom = hub.defaultZoom,
        ),
        trails = if (hubChanged) emptyMap() else state.trails,
        statusBanner = null,
        errorMessage = null,
      )
    }
    refreshNow(silent = false)
  }

  fun refreshNow(silent: Boolean = false) {
    val current = _uiState.value
    if (current.isGlobalSearchMode && current.searchQuery.isNotBlank()) {
      submitGlobalSearch(current.searchQuery)
      return
    }
    refreshJob?.cancel()
    refreshJob = viewModelScope.launch {
      val targetHub = _uiState.value.activeHub
      if (!silent) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      }
      val result = runCatching { repository.fetchHubFlights(targetHub) }
      result.onSuccess { feed ->
        applyFeedResult(targetHub, feed, isGlobalSearch = false)
      }.onFailure { err ->
        _uiState.update {
          it.copy(
            isLoading = false,
            errorMessage = "Live feed unavailable: ${err.message ?: "network error"}",
          )
        }
      }
    }
  }

  fun updateSearchQuery(query: String) {
    _uiState.update { state ->
      val exitingGlobal = state.isGlobalSearchMode && query.isBlank()
      val next = state.copy(
        searchQuery = query,
        isGlobalSearchMode = if (exitingGlobal) false else state.isGlobalSearchMode,
        statusBanner = if (exitingGlobal) null else state.statusBanner,
      )
      next.copy(filteredAircraft = computeFilteredSorted(next))
    }
    if (query.isBlank() && _uiState.value.activeFeedSource == FeedSource.ADSB_LOL_SEARCH) {
      refreshNow(silent = false)
    }
  }

  fun submitGlobalSearch(rawQuery: String = _uiState.value.searchQuery) {
    val query = rawQuery.trim()
    if (query.isBlank()) {
      exitGlobalSearch()
      return
    }
    refreshJob?.cancel()
    refreshJob = viewModelScope.launch {
      _uiState.update {
        it.copy(
          searchQuery = query,
          isGlobalSearchMode = true,
          isLoading = true,
          errorMessage = null,
          statusBanner = "Searching global ADS-B via api.adsb.lol for \"$query\"…",
        )
      }
      val result = runCatching { repository.searchGlobal(query) }
      result.onSuccess { feed ->
        applyFeedResult(_uiState.value.activeHub, feed, isGlobalSearch = true)
        val firstMatch = feed.aircraft.firstOrNull()
        if (firstMatch != null) {
          selectAircraft(firstMatch.hex, centerOnAircraft = true)
        }
      }.onFailure { err ->
        _uiState.update {
          it.copy(
            isLoading = false,
            errorMessage = "Global search failed: ${err.message ?: "network error"}",
          )
        }
      }
    }
  }

  fun exitGlobalSearch() {
    _uiState.update {
      it.copy(
        searchQuery = "",
        isGlobalSearchMode = false,
        statusBanner = null,
        errorMessage = null,
      )
    }
    refreshNow(silent = false)
  }

  fun setFilter(filter: AircraftFilter) {
    _uiState.update { state ->
      val next = state.copy(activeFilter = filter)
      next.copy(filteredAircraft = computeFilteredSorted(next))
    }
  }

  fun setSortOption(option: FlightSortOption) {
    _uiState.update { state ->
      val next = state.copy(sortOption = option)
      next.copy(filteredAircraft = computeFilteredSorted(next))
    }
  }

  fun selectAircraft(hex: String?, centerOnAircraft: Boolean = false) {
    val cleanHex = hex?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
    val aircraft = cleanHex?.let { h -> _uiState.value.allAircraft.firstOrNull { it.hex == h } }

    _uiState.update { state ->
      val nextViewport = if (centerOnAircraft && aircraft != null) {
        state.viewport.copy(
          centerLat = aircraft.latitude,
          centerLon = aircraft.longitude,
          zoom = state.viewport.zoom.coerceAtLeast(6.5f),
        )
      } else {
        state.viewport
      }
      state.copy(
        selectedHex = cleanHex,
        viewport = nextViewport,
        followSelected = if (cleanHex == null) false else state.followSelected,
        selectedEnrichment = if (cleanHex == null) {
          null
        } else if (state.selectedEnrichment?.hex == cleanHex) {
          state.selectedEnrichment
        } else {
          FlightEnrichment(hex = cleanHex, isLoading = true)
        },
      )
    }

    if (cleanHex != null && aircraft != null) {
      loadEnrichment(aircraft)
    }
  }

  fun setHoveredAircraft(hex: String?) {
    if (_uiState.value.hoveredHex != hex) {
      _uiState.update { it.copy(hoveredHex = hex) }
    }
  }

  fun refreshSelectedEnrichment() {
    val ac = _uiState.value.selectedAircraft ?: return
    loadEnrichment(ac)
  }

  private fun loadEnrichment(aircraft: Aircraft) {
    enrichmentJob?.cancel()
    enrichmentJob = viewModelScope.launch {
      _uiState.update { it.copy(selectedEnrichment = FlightEnrichment(hex = aircraft.hex, isLoading = true)) }
      val result = runCatching {
        repository.fetchEnrichment(aircraft.hex, aircraft.callsign)
      }
      result.onSuccess { enrichment ->
        if (_uiState.value.selectedHex == aircraft.hex) {
          _uiState.update { it.copy(selectedEnrichment = enrichment) }
        }
      }.onFailure { err ->
        if (_uiState.value.selectedHex == aircraft.hex) {
          _uiState.update {
            it.copy(
              selectedEnrichment = FlightEnrichment(
                hex = aircraft.hex,
                isLoading = false,
                errorMessage = err.message,
              ),
            )
          }
        }
      }
    }
  }

  fun updateViewport(viewport: ViewportState, disableFollow: Boolean = false) {
    _uiState.update {
      it.copy(
        viewport = viewport.copy(
          centerLat = GeoProjection.clampLat(viewport.centerLat),
          centerLon = GeoProjection.wrapLon(viewport.centerLon),
          zoom = GeoProjection.clampZoom(viewport.zoom),
        ),
        followSelected = if (disableFollow) false else it.followSelected,
      )
    }
  }

  fun zoomBy(delta: Float) {
    val current = _uiState.value.viewport
    updateViewport(current.copy(zoom = current.zoom + delta))
  }

  fun panByFraction(dxFraction: Float, dyFraction: Float) {
    val current = _uiState.value.viewport
    // Pan relative to a ~900px reference desktop radar pane width/height
    val next = GeoProjection.panByScreenDelta(
      viewport = current,
      dxPx = -dxFraction * 900f,
      dyPx = -dyFraction * 650f,
    )
    updateViewport(next, disableFollow = true)
  }

  fun toggleMapLayer() {
    _uiState.update { state ->
      val next = when (state.mapTileLayer) {
        MapTileLayer.ESRI_DARK_GRAY -> MapTileLayer.OPEN_STREET_MAP
        MapTileLayer.OPEN_STREET_MAP -> MapTileLayer.ESRI_DARK_GRAY
      }
      state.copy(mapTileLayer = next)
    }
  }

  fun toggleTrails() {
    _uiState.update { it.copy(showTrails = !it.showTrails) }
  }

  fun toggleGreatCircle() {
    _uiState.update { it.copy(showGreatCircle = !it.showGreatCircle) }
  }

  fun toggleFollowSelected() {
    _uiState.update { state ->
      val nextFollow = !state.followSelected && state.selectedAircraft != null
      val selected = state.selectedAircraft
      val nextViewport = if (nextFollow && selected != null) {
        state.viewport.copy(centerLat = selected.latitude, centerLon = selected.longitude)
      } else {
        state.viewport
      }
      state.copy(followSelected = nextFollow, viewport = nextViewport)
    }
  }

  fun toggleAutoRefresh() {
    _uiState.update { it.copy(autoRefreshEnabled = !it.autoRefreshEnabled) }
  }

  fun toggleContinueOn() {
    val next = !_uiState.value.continueOnEnabled
    _uiState.update { it.copy(continueOnEnabled = next) }
    onContinueOnChanged?.invoke(next)
  }

  fun setShortcutsDialogVisible(visible: Boolean) {
    _uiState.update { it.copy(showShortcutsDialog = visible) }
  }

  fun selectAdjacentFlight(step: Int) {
    val list = _uiState.value.filteredAircraft
    if (list.isEmpty()) return
    val currentIdx = list.indexOfFirst { it.hex == _uiState.value.selectedHex }
    val nextIdx = when {
      currentIdx < 0 -> if (step >= 0) 0 else list.lastIndex
      else -> (currentIdx + step).coerceIn(0, list.lastIndex)
    }
    selectAircraft(list[nextIdx].hex, centerOnAircraft = true)
  }

  fun handleShortcut(
    action: SkyWatchShortcutAction,
    onFocusSearch: () -> Unit = {},
    onOpenNewWindow: () -> Unit = {},
  ) {
    when (action) {
      SkyWatchShortcutAction.FocusSearch -> onFocusSearch()
      SkyWatchShortcutAction.RefreshFeed -> refreshNow(silent = false)
      SkyWatchShortcutAction.ToggleAutoRefresh -> toggleAutoRefresh()
      SkyWatchShortcutAction.ZoomIn -> zoomBy(1.0f)
      SkyWatchShortcutAction.ZoomOut -> zoomBy(-1.0f)
      is SkyWatchShortcutAction.PanMap -> panByFraction(action.dxFraction, action.dyFraction)
      SkyWatchShortcutAction.SelectNextFlight -> selectAdjacentFlight(1)
      SkyWatchShortcutAction.SelectPreviousFlight -> selectAdjacentFlight(-1)
      SkyWatchShortcutAction.ToggleFollowAircraft -> toggleFollowSelected()
      SkyWatchShortcutAction.ToggleMapLayer -> toggleMapLayer()
      SkyWatchShortcutAction.ToggleTrails -> toggleTrails()
      SkyWatchShortcutAction.ToggleGreatCircle -> toggleGreatCircle()
      is SkyWatchShortcutAction.SelectHubByDigit -> {
        FlightHubs.findByShortcutDigit(action.digit)?.let { selectHub(it) }
      }
      SkyWatchShortcutAction.OpenNewWindow -> onOpenNewWindow()
      SkyWatchShortcutAction.ToggleShortcutsHelp ->
        setShortcutsDialogVisible(!_uiState.value.showShortcutsDialog)
      SkyWatchShortcutAction.ClearOrDismiss -> {
        val state = _uiState.value
        when {
          state.showShortcutsDialog -> setShortcutsDialogVisible(false)
          state.selectedHex != null -> selectAircraft(null)
          state.isGlobalSearchMode -> exitGlobalSearch()
          state.searchQuery.isNotBlank() -> updateSearchQuery("")
        }
      }
    }
  }

  private fun applyFeedResult(
    hub: FlightHub,
    feed: FlightFeedResult,
    isGlobalSearch: Boolean,
  ) {
    val updatedTrails = trailStore.recordSnapshot(feed.aircraft, feed.timestampMs)
    var aircraftToEnrich: Aircraft? = null

    _uiState.update { state ->
      val selectedNow = state.selectedHex?.let { hex ->
        feed.aircraft.firstOrNull { it.hex == hex }
      }
      if (selectedNow != null && state.selectedEnrichment?.hex != selectedNow.hex) {
        aircraftToEnrich = selectedNow
      }

      val nextViewport = if (state.followSelected && selectedNow != null) {
        state.viewport.copy(
          centerLat = selectedNow.latitude,
          centerLon = selectedNow.longitude,
        )
      } else {
        state.viewport
      }

      val banner = when {
        feed.failoverActive -> feed.note
        isGlobalSearch && feed.aircraft.isEmpty() -> "No live aircraft matched \"${state.searchQuery}\""
        isGlobalSearch -> "Global match: ${feed.aircraft.size} aircraft via api.adsb.lol"
        else -> null
      }

      val updated = state.copy(
        allAircraft = feed.aircraft,
        trails = updatedTrails,
        viewport = nextViewport,
        isLoading = false,
        activeFeedSource = feed.source,
        failoverActive = feed.failoverActive,
        feedLatencyMs = feed.latencyMs,
        lastUpdatedMs = feed.timestampMs,
        statusBanner = banner,
        errorMessage = null,
      )
      updated.copy(filteredAircraft = computeFilteredSorted(updated))
    }

    aircraftToEnrich?.let { loadEnrichment(it) }
    if (!isGlobalSearch) {
      onFeedUpdated?.invoke(hub, feed)
    }
  }

  private fun computeFilteredSorted(state: SkyWatchUiState): List<Aircraft> {
    val query = if (state.isGlobalSearchMode) "" else state.searchQuery.trim().lowercase()
    val hubPoint = GeoPoint(state.activeHub.latitude, state.activeHub.longitude)

    val filtered = state.allAircraft.filter { ac ->
      val filterOk = when (state.activeFilter) {
        AircraftFilter.ALL -> true
        AircraftFilter.WIDEBODY -> ac.isWidebody
        AircraftFilter.MILITARY -> ac.isMilitary
        AircraftFilter.AIRBORNE -> !ac.onGround
        AircraftFilter.EMERGENCY -> ac.isEmergency
      }
      if (!filterOk) return@filter false
      if (query.isBlank()) return@filter true

      ac.hex.contains(query) ||
        (ac.callsign?.lowercase()?.contains(query) == true) ||
        (ac.registration?.lowercase()?.contains(query) == true) ||
        (ac.aircraftType?.lowercase()?.contains(query) == true) ||
        (ac.description?.lowercase()?.contains(query) == true) ||
        (ac.squawk?.contains(query) == true)
    }

    return when (state.sortOption) {
      FlightSortOption.ALTITUDE_DESC -> filtered.sortedWith(
        compareByDescending<Aircraft> { it.isEmergency }
          .thenByDescending { it.effectiveAltitudeFt ?: -1 }
          .thenBy { it.displayCallsign },
      )
      FlightSortOption.SPEED_DESC -> filtered.sortedWith(
        compareByDescending<Aircraft> { it.groundSpeedKt ?: -1.0 }
          .thenBy { it.displayCallsign },
      )
      FlightSortOption.CALLSIGN_ASC -> filtered.sortedBy { it.displayCallsign }
      FlightSortOption.DISTANCE_ASC -> filtered.sortedBy { ac ->
        ac.distanceNm ?: GreatCircleCalculator.distanceNm(
          hubPoint,
          GeoPoint(ac.latitude, ac.longitude),
        )
      }
    }
  }

  class Factory(
    private val repository: FlightRepository = LiveFlightRepository(),
    private val onFeedUpdated: ((FlightHub, FlightFeedResult) -> Unit)? = null,
    private val onContinueOnChanged: ((Boolean) -> Unit)? = null,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return SkyWatchViewModel(
        repository = repository,
        onFeedUpdated = onFeedUpdated,
        onContinueOnChanged = onContinueOnChanged,
      ) as T
    }
  }
}
