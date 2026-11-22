package com.zakariaalnatour.skywatch.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zakariaalnatour.skywatch.model.Aircraft
import com.zakariaalnatour.skywatch.model.AircraftFilter
import com.zakariaalnatour.skywatch.model.FlightSortOption
import com.zakariaalnatour.skywatch.ui.theme.SkyWatchColors
import com.zakariaalnatour.skywatch.ui.theme.SkyWatchIcons

@Composable
fun FlightListPane(
  aircraft: List<Aircraft>,
  totalAircraftCount: Int,
  selectedHex: String?,
  searchQuery: String,
  isGlobalSearchMode: Boolean,
  activeFilter: AircraftFilter,
  sortOption: FlightSortOption,
  searchFocusRequester: FocusRequester,
  onSearchFocusChanged: (Boolean) -> Unit,
  onSearchQueryChanged: (String) -> Unit,
  onSubmitGlobalSearch: () -> Unit,
  onExitGlobalSearch: () -> Unit,
  onFilterSelected: (AircraftFilter) -> Unit,
  onSortSelected: (FlightSortOption) -> Unit,
  onSelectAircraft: (Aircraft) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxHeight()
      .background(SkyWatchColors.SurfacePanel),
  ) {
    // Search Box (Instant Local Filter + Enter/Button for Global api.adsb.lol Search)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(9.dp),
        color = SkyWatchColors.SurfaceCard,
        border = BorderStroke(
          1.dp,
          if (isGlobalSearchMode) SkyWatchColors.RadarCyan else SkyWatchColors.BorderSubtle,
        ),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 9.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
          Icon(
            imageVector = SkyWatchIcons.Search,
            contentDescription = "Search flights",
            tint = SkyWatchColors.TextMuted,
            modifier = Modifier.size(16.dp),
          )
          Box(modifier = Modifier.weight(1f)) {
            if (searchQuery.isEmpty()) {
              Text(
                text = "Callsign, reg, hex, squawk (/)",
                style = MaterialTheme.typography.bodySmall,
                color = SkyWatchColors.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            BasicTextField(
              value = searchQuery,
              onValueChange = onSearchQueryChanged,
              singleLine = true,
              textStyle = MaterialTheme.typography.bodyMedium.copy(color = SkyWatchColors.TextPrimary),
              cursorBrush = SolidColor(SkyWatchColors.RadarCyan),
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
              keyboardActions = KeyboardActions(onSearch = { onSubmitGlobalSearch() }),
              modifier = Modifier
                .fillMaxWidth()
                .focusRequester(searchFocusRequester)
                .onFocusChanged { onSearchFocusChanged(it.isFocused) },
            )
          }
          if (searchQuery.isNotBlank()) {
            Icon(
              imageVector = SkyWatchIcons.Close,
              contentDescription = "Clear search",
              tint = SkyWatchColors.TextSecondary,
              modifier = Modifier
                .size(16.dp)
                .clickable {
                  if (isGlobalSearchMode) onExitGlobalSearch() else onSearchQueryChanged("")
                },
            )
            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .clickable(onClick = onSubmitGlobalSearch),
              shape = RoundedCornerShape(5.dp),
              color = SkyWatchColors.RadarCyan.copy(alpha = 0.2f),
              border = BorderStroke(1.dp, SkyWatchColors.RadarCyan.copy(alpha = 0.6f)),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
              ) {
                Icon(
                  imageVector = SkyWatchIcons.PublicGlobe,
                  contentDescription = "Global search",
                  tint = SkyWatchColors.RadarCyanBright,
                  modifier = Modifier.size(11.dp),
                )
                Text(
                  text = "Global",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                  color = SkyWatchColors.RadarCyanBright,
                )
              }
            }
          }
        }
      }

      // Filter Chips
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        for (filter in AircraftFilter.entries) {
          val selected = filter == activeFilter
          Surface(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .clickable { onFilterSelected(filter) },
            shape = RoundedCornerShape(6.dp),
            color = if (selected) SkyWatchColors.SurfaceCardSelected else SkyWatchColors.SurfaceElevated,
            border = BorderStroke(
              1.dp,
              if (selected) SkyWatchColors.RadarCyan else SkyWatchColors.BorderSubtle,
            ),
          ) {
            Text(
              text = filter.label,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              style = MaterialTheme.typography.labelSmall,
              color = if (selected) SkyWatchColors.RadarCyanBright else SkyWatchColors.TextSecondary,
            )
          }
        }
      }

      // Count + Sort Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = if (aircraft.size == totalAircraftCount) {
            "$totalAircraftCount AIRCRAFT"
          } else {
            "${aircraft.size} / $totalAircraftCount SHOWN"
          },
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
          color = SkyWatchColors.TextMuted,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          for (sort in FlightSortOption.entries) {
            val active = sort == sortOption
            Text(
              text = sort.label,
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
              ),
              color = if (active) SkyWatchColors.RadarCyan else SkyWatchColors.TextMuted,
              modifier = Modifier.clickable { onSortSelected(sort) },
            )
          }
        }
      }
    }

    HorizontalDivider(color = SkyWatchColors.BorderSubtle, thickness = 1.dp)

    if (aircraft.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(20.dp),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "No aircraft match filter",
            style = MaterialTheme.typography.titleSmall,
            color = SkyWatchColors.TextSecondary,
          )
          if (searchQuery.isNotBlank() && !isGlobalSearchMode) {
            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onSubmitGlobalSearch),
              shape = RoundedCornerShape(8.dp),
              color = SkyWatchColors.RadarCyan.copy(alpha = 0.16f),
              border = BorderStroke(1.dp, SkyWatchColors.RadarCyan),
            ) {
              Text(
                text = "Search \"$searchQuery\" globally via adsb.lol",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = SkyWatchColors.RadarCyanBright,
              )
            }
          }
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        items(items = aircraft, key = { it.hex }) { ac ->
          FlightRowCard(
            aircraft = ac,
            selected = ac.hex == selectedHex,
            onClick = { onSelectAircraft(ac) },
          )
        }
      }
    }
  }
}

@Composable
private fun FlightRowCard(
  aircraft: Aircraft,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val bandColor = SkyWatchColors.altitudeBandColor(
    altitudeFt = aircraft.effectiveAltitudeFt,
    onGround = aircraft.onGround,
    isMilitary = aircraft.isMilitary,
    isEmergency = aircraft.isEmergency,
  )

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(9.dp))
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(9.dp),
    color = if (selected) SkyWatchColors.SurfaceCardSelected else SkyWatchColors.SurfaceCard,
    border = BorderStroke(
      width = if (selected) 1.3.dp else 1.dp,
      color = if (selected) SkyWatchColors.RadarCyan else SkyWatchColors.BorderSubtle,
    ),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Altitude / Role color accent stripe
      Box(
        modifier = Modifier
          .width(3.5.dp)
          .height(38.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(bandColor),
      )
      Spacer(modifier = Modifier.width(9.dp))

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Text(
            text = aircraft.displayCallsign,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
            color = SkyWatchColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          aircraft.aircraftType?.let { type ->
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = SkyWatchColors.SurfaceElevated,
            ) {
              Text(
                text = type,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                color = SkyWatchColors.RadarCyanBright,
              )
            }
          }
          if (aircraft.isMilitary) {
            MiniTag("MIL", SkyWatchColors.MilitaryGold)
          } else if (aircraft.isWidebody) {
            MiniTag("HVY", SkyWatchColors.VioletHigh)
          }
          if (aircraft.isEmergency) {
            MiniTag(aircraft.squawk ?: "ALRT", SkyWatchColors.AlertCrimson)
          }
        }

        Text(
          text = buildString {
            append(aircraft.registration ?: aircraft.hex.uppercase())
            aircraft.description?.let { append(" · $it") }
          },
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
          color = SkyWatchColors.TextSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Spacer(modifier = Modifier.width(6.dp))

      Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        val climbGlyph = when {
          (aircraft.baroRateFpm ?: 0) > 200 -> " ▲"
          (aircraft.baroRateFpm ?: 0) < -200 -> " ▼"
          else -> ""
        }
        Text(
          text = aircraft.formattedAltitude + climbGlyph,
          style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp),
          color = bandColor,
        )
        Text(
          text = aircraft.formattedSpeed,
          style = MaterialTheme.typography.labelSmall,
          color = SkyWatchColors.TextSecondary,
        )
      }
    }
  }
}

@Composable
private fun MiniTag(text: String, color: androidx.compose.ui.graphics.Color) {
  Surface(
    shape = RoundedCornerShape(4.dp),
    color = color.copy(alpha = 0.18f),
    border = BorderStroke(0.8.dp, color.copy(alpha = 0.6f)),
  ) {
    Text(
      text = text,
      modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
      color = color,
    )
  }
}
