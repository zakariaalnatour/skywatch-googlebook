package com.zakariaalnatour.skywatch.ui

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.zakariaalnatour.skywatch.platform.KeyboardShortcutHandler
import com.zakariaalnatour.skywatch.platform.SkyWatchHandoffState
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

@Composable
fun SkyWatchApp(
  viewModel: SkyWatchViewModel,
  onOpenNewWindow: (SkyWatchHandoffState) -> Unit,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsState()
  val searchFocusRequester = remember { FocusRequester() }
  val focusManager = LocalFocusManager.current
  var isSearchFocused by remember { mutableStateOf(false) }
  var mediumTab by remember { mutableStateOf(SecondaryPaneTab.LIST) }
  var compactShowDrawer by remember { mutableStateOf(false) }

  Column(
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
        ) ?: return@onPreviewKeyEvent false

        if (action == com.zakariaalnatour.skywatch.platform.SkyWatchShortcutAction.ClearOrDismiss && isSearchFocused) {
          focusManager.clearFocus()
        }

        viewModel.handleShortcut(
          action = action,
          onFocusSearch = {
            runCatching { searchFocusRequester.requestFocus() }
          },
          onOpenNewWindow = {
            onOpenNewWindow(uiState.toHandoffState())
          },
        )
        true
      },
  ) {
    TopCommandBar(
      activeHub = uiState.activeHub,
      trackedCount = uiState.allAircraft.size,
      feedSource = uiState.activeFeedSource,
      failoverActive = uiState.failoverActive,
      latencyMs = uiState.feedLatencyMs,
      isLoading = uiState.isLoading,
      continueOnEnabled = uiState.continueOnEnabled,
      onSelectHub = { viewModel.selectHub(it) },
      onToggleContinueOn = { viewModel.toggleContinueOn() },
      onOpenNewWindow = { onOpenNewWindow(uiState.toHandoffState()) },
      onShowShortcuts = { viewModel.setShortcutsDialogVisible(true) },
    )

    // Adaptive 3-Pane (Googlebook Wide) / 2-Pane (Split Window) / Compact (Narrow Window)
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
    ) {
      val widthDp = maxWidth

      when {
        // EXPANDED DESKTOP: Full 3-Pane Aviation Command Center
        widthDp >= 1020.dp -> {
          Row(modifier = Modifier.fillMaxSize()) {
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
              modifier = Modifier.width(300.dp),
            )
            VerticalDivider(color = SkyWatchColors.BorderSubtle, thickness = 1.dp)

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
              onZoomIn = { viewModel.zoomBy(0.8f) },
              onZoomOut = { viewModel.zoomBy(-0.8f) },
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

            VerticalDivider(color = SkyWatchColors.BorderSubtle, thickness = 1.dp)

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
              modifier = Modifier.width(340.dp),
            )
          }
        }

        // MEDIUM SPLIT WINDOW: 2-Pane (Geographic Radar + Tabbed Flights/Inspector Sidebar)
        widthDp >= 680.dp -> {
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
                if (hex != null) mediumTab = SecondaryPaneTab.INSPECTOR
              },
              onHoverAircraft = { hex -> viewModel.setHoveredAircraft(hex) },
              onZoomIn = { viewModel.zoomBy(0.8f) },
              onZoomOut = { viewModel.zoomBy(-0.8f) },
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
            VerticalDivider(color = SkyWatchColors.BorderSubtle, thickness = 1.dp)
            Column(modifier = Modifier.width(310.dp).fillMaxHeight()) {
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

        // COMPACT FREEFORM WINDOW: Stacked Radar + Collapsible Drawer
        else -> {
          Column(modifier = Modifier.fillMaxSize()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .weight(if (compactShowDrawer) 0.52f else 1f),
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
                onZoomIn = { viewModel.zoomBy(0.8f) },
                onZoomOut = { viewModel.zoomBy(-0.8f) },
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
              HorizontalDivider(color = SkyWatchColors.BorderSubtle, thickness = 1.dp)
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .weight(0.48f),
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

  if (uiState.showShortcutsDialog) {
    ShortcutsDialog(onDismiss = { viewModel.setShortcutsDialogVisible(false) })
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
