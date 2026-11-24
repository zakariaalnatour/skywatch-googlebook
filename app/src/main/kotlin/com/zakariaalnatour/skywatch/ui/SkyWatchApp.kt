package com.zakariaalnatour.skywatch.ui

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.zakariaalnatour.skywatch.platform.KeyboardShortcutHandler
import com.zakariaalnatour.skywatch.platform.SkyWatchHandoffState
import com.zakariaalnatour.skywatch.platform.SkyWatchShortcutAction
import com.zakariaalnatour.skywatch.ui.components.FlightInspectorPane
import com.zakariaalnatour.skywatch.ui.components.FlightListPane
import com.zakariaalnatour.skywatch.ui.components.ShortcutsDialog
import com.zakariaalnatour.skywatch.ui.components.TopCommandBar
import com.zakariaalnatour.skywatch.ui.map.FlightRadarMap
import com.zakariaalnatour.skywatch.ui.theme.SkyWatchColors
import com.zakariaalnatour.skywatch.viewmodel.SkyWatchViewModel

private enum class SecondaryPaneTab(val label: String) {
  LIST("Flights"),
  INSPECTOR("Inspector"),
}

private enum class LayoutTier {
  WIDE,
  MEDIUM,
  COMPACT,
}

@Composable
fun SkyWatchApp(
  viewModel: SkyWatchViewModel,
  onOpenNewWindow: (SkyWatchHandoffState) -> Unit,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsState()
  val searchFocusRequester = remember { FocusRequester() }
  val focusManager = LocalFocusManager.current
  val density = LocalDensity.current

  var isSearchFocused by remember { mutableStateOf(false) }

  // Collapsible & draggable split-pane state across Wide, Medium, and Compact windows
  var showFlightsPane by remember { mutableStateOf(true) }
  var showInspectorPane by remember { mutableStateOf(true) }
  var flightsPaneWidthDp by remember { mutableStateOf(300.dp) }
  var inspectorPaneWidthDp by remember { mutableStateOf(340.dp) }

  var mediumSideVisible by remember { mutableStateOf(true) }
  var mediumSideWidthDp by remember { mutableStateOf(310.dp) }
  var mediumTab by remember { mutableStateOf(SecondaryPaneTab.LIST) }

  var compactShowDrawer by remember { mutableStateOf(false) }
  var compactDrawerHeightDp by remember { mutableStateOf(280.dp) }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(SkyWatchColors.ObsidianBg)
      .onPreviewKeyEvent { keyEvent ->
        val native = keyEvent.nativeKeyEvent
        if (native.action != AndroidKeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
        val action = KeyboardShortcutHandler.mapKeyCode(
          keyCode = native.keyCode,
          isCtrlPressed = native.isCtrlPressed,
          isShiftPressed = native.isShiftPressed,
          isAltPressed = native.isAltPressed,
          isSearchFieldFocused = isSearchFocused,
          unicodeChar = native.unicodeChar,
        ) ?: return@onPreviewKeyEvent false

        if (action == SkyWatchShortcutAction.ClearOrDismiss && isSearchFocused) {
          focusManager.clearFocus()
        }

        viewModel.handleShortcut(
          action = action,
          onFocusSearch = {
            showFlightsPane = true
            mediumSideVisible = true
            compactShowDrawer = true
            mediumTab = SecondaryPaneTab.LIST
            runCatching { searchFocusRequester.requestFocus() }
          },
          onOpenNewWindow = {
            onOpenNewWindow(uiState.toHandoffState())
          },
        )
        true
      },
  ) {
    val tier = when {
      maxWidth >= 1020.dp -> LayoutTier.WIDE
      maxWidth >= 680.dp -> LayoutTier.MEDIUM
      else -> LayoutTier.COMPACT
    }

    val flightsChipActive = when (tier) {
      LayoutTier.WIDE -> showFlightsPane
      LayoutTier.MEDIUM -> mediumSideVisible && mediumTab == SecondaryPaneTab.LIST
      LayoutTier.COMPACT -> compactShowDrawer && mediumTab == SecondaryPaneTab.LIST
    }
    val inspectorChipActive = when (tier) {
      LayoutTier.WIDE -> showInspectorPane
      LayoutTier.MEDIUM -> mediumSideVisible && mediumTab == SecondaryPaneTab.INSPECTOR
      LayoutTier.COMPACT -> compactShowDrawer && mediumTab == SecondaryPaneTab.INSPECTOR
    }

    Column(modifier = Modifier.fillMaxSize()) {
      TopCommandBar(
        activeHub = uiState.activeHub,
        trackedCount = uiState.allAircraft.size,
        feedSource = uiState.activeFeedSource,
        failoverActive = uiState.failoverActive,
        latencyMs = uiState.feedLatencyMs,
        isLoading = uiState.isLoading,
        continueOnEnabled = uiState.continueOnEnabled,
        flightsPaneVisible = flightsChipActive,
        inspectorPaneVisible = inspectorChipActive,
        onSelectHub = { viewModel.selectHub(it) },
        onToggleFlightsPane = {
          when (tier) {
            LayoutTier.WIDE -> showFlightsPane = !showFlightsPane
            LayoutTier.MEDIUM -> {
              if (mediumSideVisible && mediumTab == SecondaryPaneTab.LIST) {
                mediumSideVisible = false
              } else {
                mediumTab = SecondaryPaneTab.LIST
                mediumSideVisible = true
              }
            }
            LayoutTier.COMPACT -> {
              if (compactShowDrawer && mediumTab == SecondaryPaneTab.LIST) {
                compactShowDrawer = false
              } else {
                mediumTab = SecondaryPaneTab.LIST
                compactShowDrawer = true
              }
            }
          }
        },
        onToggleInspectorPane = {
          when (tier) {
            LayoutTier.WIDE -> showInspectorPane = !showInspectorPane
            LayoutTier.MEDIUM -> {
              if (mediumSideVisible && mediumTab == SecondaryPaneTab.INSPECTOR) {
                mediumSideVisible = false
              } else {
                mediumTab = SecondaryPaneTab.INSPECTOR
                mediumSideVisible = true
              }
            }
            LayoutTier.COMPACT -> {
              if (compactShowDrawer && mediumTab == SecondaryPaneTab.INSPECTOR) {
                compactShowDrawer = false
              } else {
                mediumTab = SecondaryPaneTab.INSPECTOR
                compactShowDrawer = true
              }
            }
          }
        },
        onToggleContinueOn = { viewModel.toggleContinueOn() },
        onOpenNewWindow = { onOpenNewWindow(uiState.toHandoffState()) },
        onShowShortcuts = { viewModel.setShortcutsDialogVisible(true) },
      )

      // Adaptive 3-Pane (Googlebook Wide) / 2-Pane (Split Window) / Compact (Narrow Window)
      BoxWithConstraints(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .clipToBounds(),
      ) {
        val contentWidthDp = maxWidth
        val contentHeightDp = maxHeight

        when (tier) {
          // EXPANDED DESKTOP: Collapsible & Resizable 3-Pane Aviation Command Center
          LayoutTier.WIDE -> {
            val dividerWidth = 10.dp
            val minPaneWidth = 200.dp
            val minMapWidth = 280.dp
            val dividerCount = (if (showFlightsPane) 1 else 0) + (if (showInspectorPane) 1 else 0)
            val availableForPanes = (contentWidthDp - minMapWidth - dividerWidth * dividerCount)
              .coerceAtLeast(minPaneWidth)

            val maxFlightsW = if (showInspectorPane) {
              (availableForPanes - minPaneWidth).coerceAtLeast(minPaneWidth)
            } else {
              minOf(560.dp, availableForPanes)
            }
            val clampedFlightsW = flightsPaneWidthDp.coerceIn(minPaneWidth, maxFlightsW)

            val maxInspectorW = if (showFlightsPane) {
              (availableForPanes - clampedFlightsW).coerceAtLeast(minPaneWidth)
            } else {
              minOf(560.dp, availableForPanes)
            }
            val clampedInspectorW = inspectorPaneWidthDp.coerceIn(minPaneWidth, maxInspectorW)

            Row(modifier = Modifier.fillMaxSize()) {
              if (showFlightsPane) {
                FlightListPane(
                  aircraft = uiState.filteredAircraft,
                  totalAircraftCount = uiState.allAircraft.size,
                  selectedHex = uiState.selectedHex,
                  searchQuery = uiState.searchQuery,
                  isGlobalSearchMode = uiState.isGlobalSearchMode,
                  activeFilter = uiState.activeFilter,
                  sortOption = uiState.sortOption,
                  searchFocusRequester = searchFocusRequester,
                  onSearchFocusChanged = { isSearchFocused = it },
                  onSearchQueryChanged = { viewModel.updateSearchQuery(it) },
                  onSubmitGlobalSearch = { viewModel.submitGlobalSearch() },
                  onExitGlobalSearch = { viewModel.exitGlobalSearch() },
                  onFilterSelected = { viewModel.setFilter(it) },
                  onSortSelected = { viewModel.setSortOption(it) },
                  onSelectAircraft = { viewModel.selectAircraft(it.hex, centerOnAircraft = true) },
                  modifier = Modifier
                    .width(clampedFlightsW)
                    .zIndex(1f),
                )
                ResizableVerticalDivider(
                  onDragDeltaPx = { deltaPx ->
                    val deltaDp = with(density) { deltaPx.toDp() }
                    flightsPaneWidthDp = (clampedFlightsW + deltaDp).coerceIn(minPaneWidth, maxFlightsW)
                  },
                )
              }

              FlightRadarMap(
                activeHub = uiState.activeHub,
                viewport = uiState.viewport,
                aircraft = uiState.filteredAircraft,
                trails = uiState.trails,
                selectedHex = uiState.selectedHex,
                hoveredHex = uiState.hoveredHex,
                selectedEnrichment = uiState.selectedEnrichment,
                tileLayer = uiState.mapTileLayer,
                showTrails = uiState.showTrails,
                showGreatCircle = uiState.showGreatCircle,
                autoRefreshEnabled = uiState.autoRefreshEnabled,
                statusBanner = uiState.statusBanner,
                errorMessage = uiState.errorMessage,
                onViewportChanged = { vp, disableFollow -> viewModel.updateViewport(vp, disableFollow) },
                onSelectAircraft = { hex -> viewModel.selectAircraft(hex, centerOnAircraft = false) },
                onHoverAircraft = { hex -> viewModel.setHoveredAircraft(hex) },
                onZoomIn = { viewModel.zoomBy(1.0f) },
                onZoomOut = { viewModel.zoomBy(-1.0f) },
                onRecenterHub = { viewModel.selectHub(uiState.activeHub) },
                onToggleMapLayer = { viewModel.toggleMapLayer() },
                onToggleTrails = { viewModel.toggleTrails() },
                onToggleGreatCircle = { viewModel.toggleGreatCircle() },
                onToggleAutoRefresh = { viewModel.toggleAutoRefresh() },
                onRefreshNow = { viewModel.refreshNow(silent = false) },
                modifier = Modifier
                  .weight(1f)
                  .fillMaxHeight(),
              )

              if (showInspectorPane) {
                ResizableVerticalDivider(
                  onDragDeltaPx = { deltaPx ->
                    val deltaDp = with(density) { deltaPx.toDp() }
                    inspectorPaneWidthDp = (clampedInspectorW - deltaDp).coerceIn(minPaneWidth, maxInspectorW)
                  },
                )
                FlightInspectorPane(
                  activeHub = uiState.activeHub,
                  selectedAircraft = uiState.selectedAircraft,
                  enrichment = uiState.selectedEnrichment,
                  allAircraft = uiState.allAircraft,
                  feedSource = uiState.activeFeedSource,
                  latencyMs = uiState.feedLatencyMs,
                  followSelected = uiState.followSelected,
                  onToggleFollow = { viewModel.toggleFollowSelected() },
                  onPopOutWindow = { onOpenNewWindow(uiState.toHandoffState()) },
                  onRefreshEnrichment = { viewModel.refreshSelectedEnrichment() },
                  onClearSelection = { viewModel.selectAircraft(null) },
                  onSelectAircraft = { viewModel.selectAircraft(it.hex, centerOnAircraft = true) },
                  modifier = Modifier
                    .width(clampedInspectorW)
                    .zIndex(1f),
                )
              }
            }
          }

          // MEDIUM SPLIT WINDOW: Resizable & Collapsible 2-Pane (Map + Tabbed Sidebar)
          LayoutTier.MEDIUM -> {
            val minSideW = 220.dp
            val maxSideW = (contentWidthDp - 260.dp - 10.dp).coerceAtLeast(minSideW)
            val clampedSideW = mediumSideWidthDp.coerceIn(minSideW, maxSideW)

            Row(modifier = Modifier.fillMaxSize()) {
              FlightRadarMap(
                activeHub = uiState.activeHub,
                viewport = uiState.viewport,
                aircraft = uiState.filteredAircraft,
                trails = uiState.trails,
                selectedHex = uiState.selectedHex,
                hoveredHex = uiState.hoveredHex,
                selectedEnrichment = uiState.selectedEnrichment,
                tileLayer = uiState.mapTileLayer,
                showTrails = uiState.showTrails,
                showGreatCircle = uiState.showGreatCircle,
                autoRefreshEnabled = uiState.autoRefreshEnabled,
                statusBanner = uiState.statusBanner,
                errorMessage = uiState.errorMessage,
                onViewportChanged = { vp, disableFollow -> viewModel.updateViewport(vp, disableFollow) },
                onSelectAircraft = { hex ->
                  viewModel.selectAircraft(hex, centerOnAircraft = false)
                  if (hex != null) {
                    mediumTab = SecondaryPaneTab.INSPECTOR
                    mediumSideVisible = true
                  }
                },
                onHoverAircraft = { hex -> viewModel.setHoveredAircraft(hex) },
                onZoomIn = { viewModel.zoomBy(1.0f) },
                onZoomOut = { viewModel.zoomBy(-1.0f) },
                onRecenterHub = { viewModel.selectHub(uiState.activeHub) },
                onToggleMapLayer = { viewModel.toggleMapLayer() },
                onToggleTrails = { viewModel.toggleTrails() },
                onToggleGreatCircle = { viewModel.toggleGreatCircle() },
                onToggleAutoRefresh = { viewModel.toggleAutoRefresh() },
                onRefreshNow = { viewModel.refreshNow(silent = false) },
                modifier = Modifier
                  .weight(1f)
                  .fillMaxHeight(),
              )

              if (mediumSideVisible) {
                ResizableVerticalDivider(
                  onDragDeltaPx = { deltaPx ->
                    val deltaDp = with(density) { deltaPx.toDp() }
                    mediumSideWidthDp = (clampedSideW - deltaDp).coerceIn(minSideW, maxSideW)
                  },
                )
                Column(
                  modifier = Modifier
                    .width(clampedSideW)
                    .fillMaxHeight()
                    .zIndex(1f)
                    .clipToBounds(),
                ) {
                  PaneSwitcherBar(
                    activeTab = mediumTab,
                    flightCount = uiState.filteredAircraft.size,
                    hasSelection = uiState.selectedAircraft != null,
                    onSelectTab = { mediumTab = it },
                  )
                  if (mediumTab == SecondaryPaneTab.LIST) {
                    FlightListPane(
                      aircraft = uiState.filteredAircraft,
                      totalAircraftCount = uiState.allAircraft.size,
                      selectedHex = uiState.selectedHex,
                      searchQuery = uiState.searchQuery,
                      isGlobalSearchMode = uiState.isGlobalSearchMode,
                      activeFilter = uiState.activeFilter,
                      sortOption = uiState.sortOption,
                      searchFocusRequester = searchFocusRequester,
                      onSearchFocusChanged = { isSearchFocused = it },
                      onSearchQueryChanged = { viewModel.updateSearchQuery(it) },
                      onSubmitGlobalSearch = { viewModel.submitGlobalSearch() },
                      onExitGlobalSearch = { viewModel.exitGlobalSearch() },
                      onFilterSelected = { viewModel.setFilter(it) },
                      onSortSelected = { viewModel.setSortOption(it) },
                      onSelectAircraft = {
                        viewModel.selectAircraft(it.hex, centerOnAircraft = true)
                        mediumTab = SecondaryPaneTab.INSPECTOR
                      },
                      modifier = Modifier.fillMaxSize(),
                    )
                  } else {
                    FlightInspectorPane(
                      activeHub = uiState.activeHub,
                      selectedAircraft = uiState.selectedAircraft,
                      enrichment = uiState.selectedEnrichment,
                      allAircraft = uiState.allAircraft,
                      feedSource = uiState.activeFeedSource,
                      latencyMs = uiState.feedLatencyMs,
                      followSelected = uiState.followSelected,
                      onToggleFollow = { viewModel.toggleFollowSelected() },
                      onPopOutWindow = { onOpenNewWindow(uiState.toHandoffState()) },
                      onRefreshEnrichment = { viewModel.refreshSelectedEnrichment() },
                      onClearSelection = { viewModel.selectAircraft(null) },
                      onSelectAircraft = { viewModel.selectAircraft(it.hex, centerOnAircraft = true) },
                      modifier = Modifier.fillMaxSize(),
                    )
                  }
                }
              }
            }
          }

          // COMPACT FREEFORM WINDOW: Stacked Radar + Collapsible & Resizable Bottom Drawer
          LayoutTier.COMPACT -> {
            val minDrawerH = 150.dp
            val maxDrawerH = (contentHeightDp - 180.dp - 10.dp).coerceAtLeast(minDrawerH)
            val clampedDrawerH = compactDrawerHeightDp.coerceIn(minDrawerH, maxDrawerH)

            Column(modifier = Modifier.fillMaxSize()) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .weight(1f),
              ) {
                FlightRadarMap(
                  activeHub = uiState.activeHub,
                  viewport = uiState.viewport,
                  aircraft = uiState.filteredAircraft,
                  trails = uiState.trails,
                  selectedHex = uiState.selectedHex,
                  hoveredHex = uiState.hoveredHex,
                  selectedEnrichment = uiState.selectedEnrichment,
                  tileLayer = uiState.mapTileLayer,
                  showTrails = uiState.showTrails,
                  showGreatCircle = uiState.showGreatCircle,
                  autoRefreshEnabled = uiState.autoRefreshEnabled,
                  statusBanner = uiState.statusBanner,
                  errorMessage = uiState.errorMessage,
                  onViewportChanged = { vp, disableFollow -> viewModel.updateViewport(vp, disableFollow) },
                  onSelectAircraft = { hex ->
                    viewModel.selectAircraft(hex, centerOnAircraft = false)
                    if (hex != null) {
                      mediumTab = SecondaryPaneTab.INSPECTOR
                      compactShowDrawer = true
                    }
                  },
                  onHoverAircraft = { hex -> viewModel.setHoveredAircraft(hex) },
                  onZoomIn = { viewModel.zoomBy(1.0f) },
                  onZoomOut = { viewModel.zoomBy(-1.0f) },
                  onRecenterHub = { viewModel.selectHub(uiState.activeHub) },
                  onToggleMapLayer = { viewModel.toggleMapLayer() },
                  onToggleTrails = { viewModel.toggleTrails() },
                  onToggleGreatCircle = { viewModel.toggleGreatCircle() },
                  onToggleAutoRefresh = { viewModel.toggleAutoRefresh() },
                  onRefreshNow = { viewModel.refreshNow(silent = false) },
                  modifier = Modifier.fillMaxSize(),
                )

                Surface(
                  modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .zIndex(1f)
                    .padding(12.dp)
                    .clickable { compactShowDrawer = !compactShowDrawer },
                  shape = RoundedCornerShape(8.dp),
                  color = SkyWatchColors.SurfaceCardSelected,
                  border = BorderStroke(1.dp, SkyWatchColors.RadarCyan),
                ) {
                  Text(
                    text = if (compactShowDrawer) "Hide Panel" else "Flights (${uiState.filteredAircraft.size})",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = SkyWatchColors.RadarCyanBright,
                  )
                }
              }

              if (compactShowDrawer) {
                ResizableHorizontalDivider(
                  onDragDeltaPx = { deltaPx ->
                    val deltaDp = with(density) { deltaPx.toDp() }
                    compactDrawerHeightDp = (clampedDrawerH - deltaDp).coerceIn(minDrawerH, maxDrawerH)
                  },
                )
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(clampedDrawerH)
                    .zIndex(1f)
                    .clipToBounds(),
                ) {
                  PaneSwitcherBar(
                    activeTab = mediumTab,
                    flightCount = uiState.filteredAircraft.size,
                    hasSelection = uiState.selectedAircraft != null,
                    onSelectTab = { mediumTab = it },
                  )
                  if (mediumTab == SecondaryPaneTab.LIST) {
                    FlightListPane(
                      aircraft = uiState.filteredAircraft,
                      totalAircraftCount = uiState.allAircraft.size,
                      selectedHex = uiState.selectedHex,
                      searchQuery = uiState.searchQuery,
                      isGlobalSearchMode = uiState.isGlobalSearchMode,
                      activeFilter = uiState.activeFilter,
                      sortOption = uiState.sortOption,
                      searchFocusRequester = searchFocusRequester,
                      onSearchFocusChanged = { isSearchFocused = it },
                      onSearchQueryChanged = { viewModel.updateSearchQuery(it) },
                      onSubmitGlobalSearch = { viewModel.submitGlobalSearch() },
                      onExitGlobalSearch = { viewModel.exitGlobalSearch() },
                      onFilterSelected = { viewModel.setFilter(it) },
                      onSortSelected = { viewModel.setSortOption(it) },
                      onSelectAircraft = {
                        viewModel.selectAircraft(it.hex, centerOnAircraft = true)
                        mediumTab = SecondaryPaneTab.INSPECTOR
                      },
                      modifier = Modifier.fillMaxSize(),
                    )
                  } else {
                    FlightInspectorPane(
                      activeHub = uiState.activeHub,
                      selectedAircraft = uiState.selectedAircraft,
                      enrichment = uiState.selectedEnrichment,
                      allAircraft = uiState.allAircraft,
                      feedSource = uiState.activeFeedSource,
                      latencyMs = uiState.feedLatencyMs,
                      followSelected = uiState.followSelected,
                      onToggleFollow = { viewModel.toggleFollowSelected() },
                      onPopOutWindow = { onOpenNewWindow(uiState.toHandoffState()) },
                      onRefreshEnrichment = { viewModel.refreshSelectedEnrichment() },
                      onClearSelection = { viewModel.selectAircraft(null) },
                      onSelectAircraft = { viewModel.selectAircraft(it.hex, centerOnAircraft = true) },
                      modifier = Modifier.fillMaxSize(),
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  if (uiState.showShortcutsDialog) {
    ShortcutsDialog(onDismiss = { viewModel.setShortcutsDialogVisible(false) })
  }
}

@Composable
private fun ResizableVerticalDivider(
  onDragDeltaPx: (Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  val latestOnDrag by rememberUpdatedState(onDragDeltaPx)
  var isDragging by remember { mutableStateOf(false) }

  Box(
    modifier = modifier
      .fillMaxHeight()
      .width(10.dp)
      .zIndex(1.5f)
      .background(if (isDragging) SkyWatchColors.SurfaceCardSelected else SkyWatchColors.SurfacePanel)
      .pointerInput(Unit) {
        detectHorizontalDragGestures(
          onDragStart = { isDragging = true },
          onDragEnd = { isDragging = false },
          onDragCancel = { isDragging = false },
          onHorizontalDrag = { change, dragAmount ->
            change.consume()
            latestOnDrag(dragAmount)
          },
        )
      },
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier = Modifier
        .fillMaxHeight()
        .width(1.dp)
        .background(if (isDragging) SkyWatchColors.RadarCyan else SkyWatchColors.BorderSubtle),
    )
    Box(
      modifier = Modifier
        .width(4.dp)
        .height(40.dp)
        .clip(RoundedCornerShape(2.dp))
        .background(if (isDragging) SkyWatchColors.RadarCyanBright else SkyWatchColors.TextMuted.copy(alpha = 0.65f)),
    )
  }
}

@Composable
private fun ResizableHorizontalDivider(
  onDragDeltaPx: (Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  val latestOnDrag by rememberUpdatedState(onDragDeltaPx)
  var isDragging by remember { mutableStateOf(false) }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(10.dp)
      .zIndex(1.5f)
      .background(if (isDragging) SkyWatchColors.SurfaceCardSelected else SkyWatchColors.SurfacePanel)
      .pointerInput(Unit) {
        detectVerticalDragGestures(
          onDragStart = { isDragging = true },
          onDragEnd = { isDragging = false },
          onDragCancel = { isDragging = false },
          onVerticalDrag = { change, dragAmount ->
            change.consume()
            latestOnDrag(dragAmount)
          },
        )
      },
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(1.dp)
        .background(if (isDragging) SkyWatchColors.RadarCyan else SkyWatchColors.BorderSubtle),
    )
    Box(
      modifier = Modifier
        .width(40.dp)
        .height(4.dp)
        .clip(RoundedCornerShape(2.dp))
        .background(if (isDragging) SkyWatchColors.RadarCyanBright else SkyWatchColors.TextMuted.copy(alpha = 0.65f)),
    )
  }
}

@Composable
private fun PaneSwitcherBar(
  activeTab: SecondaryPaneTab,
  flightCount: Int,
  hasSelection: Boolean,
  onSelectTab: (SecondaryPaneTab) -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(38.dp)
      .background(SkyWatchColors.SurfaceElevated)
      .padding(horizontal = 8.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    for (tab in SecondaryPaneTab.entries) {
      val selected = tab == activeTab
      val title = when (tab) {
        SecondaryPaneTab.LIST -> "Flights ($flightCount)"
        SecondaryPaneTab.INSPECTOR -> if (hasSelection) "Inspector ●" else "Inspector"
      }
      Surface(
        modifier = Modifier
          .weight(1f)
          .clickable { onSelectTab(tab) },
        shape = RoundedCornerShape(6.dp),
        color = if (selected) SkyWatchColors.SurfaceCardSelected else SkyWatchColors.SurfacePanel,
        border = BorderStroke(
          1.dp,
          if (selected) SkyWatchColors.RadarCyan else SkyWatchColors.BorderSubtle,
        ),
      ) {
        Box(
          modifier = Modifier.padding(vertical = 5.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) SkyWatchColors.RadarCyanBright else SkyWatchColors.TextSecondary,
          )
        }
      }
    }
  }
}
