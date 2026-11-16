package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CountryLocation
import com.example.data.model.GeographicalRegion
import com.example.data.model.VpnServer
import com.example.data.repository.BackendSyncStatus
import com.example.ui.components.NovaGlassCard
import com.example.ui.components.ServerStatusChip
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NovaAmber
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationsScreen(
  countries: List<CountryLocation>,
  searchQuery: String,
  selectedRegion: GeographicalRegion,
  syncStatus: BackendSyncStatus,
  onSearchQueryChange: (String) -> Unit,
  onRegionSelected: (GeographicalRegion) -> Unit,
  onRefreshClick: () -> Unit,
  onToggleFavorite: (CountryLocation) -> Unit,
  onConnectSmartInCountry: (CountryLocation) -> Unit,
  onOpenServerDetails: (VpnServer) -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  var inspectedCountry by remember { mutableStateOf<CountryLocation?>(null) }

  val filteredCountries = remember(countries, searchQuery, selectedRegion, strings.isArabic) {
    val q = searchQuery.trim().lowercase()
    countries.filter { country ->
      val regionMatch = selectedRegion == GeographicalRegion.ALL || country.region == selectedRegion
      val queryMatch = if (q.isEmpty()) {
        true
      } else {
        country.nameEn.lowercase().contains(q) ||
          country.nameAr.lowercase().contains(q) ||
          country.isoCode.lowercase().contains(q) ||
          strings.regionLabel(country.region).lowercase().contains(q) ||
          country.cities.any { city ->
            city.nameEn.lowercase().contains(q) || city.nameAr.lowercase().contains(q)
          }
      }
      regionMatch && queryMatch
    }.sortedWith(
      compareByDescending<CountryLocation> { it.availableServerCount > 0 }
        .thenByDescending { it.isFavorite }
        .thenBy { if (strings.isArabic) it.nameAr else it.nameEn },
    )
  }

  val favorites = remember(countries) { countries.filter { it.isFavorite } }
  val recents = remember(countries) {
    countries.filter { it.lastConnectedEpochMs != null }.sortedByDescending { it.lastConnectedEpochMs }
  }

  Box(modifier = modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 680.dp)
        .align(Alignment.TopCenter)
        .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = strings.globalLocationsTitle,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
        IconButton(
          onClick = onRefreshClick,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("refresh_locations_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = strings.refreshServers,
            tint = NovaCyan,
          )
        }
      }

      if (syncStatus is BackendSyncStatus.Syncing) {
        LinearProgressIndicator(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        )
      }

      // Search Field
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { Text(strings.searchPlaceholder) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { onSearchQueryChange("") }) {
              Icon(Icons.Rounded.Close, contentDescription = strings.dismiss)
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp)
          .testTag("locations_search_input"),
      )

      // Region Filter Chips
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
      ) {
        items(GeographicalRegion.entries) { region ->
          FilterChip(
            selected = selectedRegion == region,
            onClick = { onRegionSelected(region) },
            label = { Text(strings.regionLabel(region)) },
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      if (filteredCountries.isEmpty()) {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Rounded.Public,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = strings.noMatchingCountries,
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      } else {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          contentPadding = PaddingValues(bottom = 24.dp),
        ) {
          if (searchQuery.isBlank() && selectedRegion == GeographicalRegion.ALL && favorites.isNotEmpty()) {
            item(key = "fav_header") {
              SectionHeader(title = strings.favoritesSection)
            }
            item(key = "fav_row") {
              LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(favorites, key = { "fav_${it.isoCode}" }) { country ->
                  QuickCountryPill(country = country, onClick = { inspectedCountry = country })
                }
              }
            }
          }

          if (searchQuery.isBlank() && selectedRegion == GeographicalRegion.ALL && recents.isNotEmpty()) {
            item(key = "recent_header") {
              SectionHeader(title = strings.recentSection)
            }
            item(key = "recent_row") {
              LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(recents, key = { "rec_${it.isoCode}" }) { country ->
                  QuickCountryPill(country = country, onClick = { inspectedCountry = country })
                }
              }
            }
          }

          item(key = "all_header") {
            SectionHeader(title = "${strings.allCountriesSection} (${filteredCountries.size})")
          }

          items(filteredCountries, key = { it.isoCode }) { country ->
            CountryRowCard(
              country = country,
              onToggleFavorite = { onToggleFavorite(country) },
              onClick = { inspectedCountry = country },
            )
          }
        }
      }
    }
  }

  // Country Cities & Servers Bottom Sheet
  inspectedCountry?.let { selected ->
    ModalBottomSheet(
      onDismissRequest = { inspectedCountry = null },
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(text = selected.flagEmoji, fontSize = 32.sp)
          Column {
            Text(
              text = if (strings.isArabic) selected.nameAr else selected.nameEn,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = strings.regionLabel(selected.region),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        if (!selected.hasDeployedServers) {
          NovaGlassCard(accentBorder = NovaAmber.copy(alpha = 0.5f)) {
            Text(
              text = strings.serverNotAvailable,
              style = MaterialTheme.typography.titleMedium,
              color = NovaAmber,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = strings.serverUnavailableHint,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        } else {
          // Smart Selection in this country
          NovaGlassCard(
            accentBorder = NovaCyan.copy(alpha = 0.5f),
            onClick = {
              inspectedCountry = null
              onConnectSmartInCountry(selected)
            },
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = NovaCyan)
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = strings.smartBestInCountry,
                  style = MaterialTheme.typography.titleMedium,
                  color = NovaCyan,
                )
                Text(
                  text = strings.smartBestInCountryDesc,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          Text(
            text = strings.selectCityOrServer,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          selected.cities.forEach { city ->
            Text(
              text = if (strings.isArabic) city.nameAr else city.nameEn,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
            )
            city.servers.forEach { server ->
              NovaGlassCard(
                onClick = {
                  inspectedCountry = null
                  onOpenServerDetails(server)
                },
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = server.hostname.ifBlank { server.id },
                      fontFamily = JetBrainsMonoFamily,
                      fontWeight = FontWeight.SemiBold,
                      fontSize = 14.sp,
                    )
                    Text(
                      text = buildString {
                        append(server.measuredLatencyMs?.let { "$it ms" } ?: strings.notMeasured)
                        server.loadPercent?.let { append(" • $it% load") }
                      },
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  ServerStatusChip(status = server.status)
                }
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(20.dp))
      }
    }
  }
}

@Composable
private fun SectionHeader(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
  )
}

@Composable
private fun QuickCountryPill(
  country: CountryLocation,
  onClick: () -> Unit,
) {
  val strings = LocalAppStrings.current
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    modifier = Modifier.clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(text = country.flagEmoji, fontSize = 20.sp)
      Column {
        Text(
          text = if (strings.isArabic) country.nameAr else country.nameEn,
          style = MaterialTheme.typography.labelLarge,
          maxLines = 1,
        )
        Text(
          text = if (country.availableServerCount > 0) {
            "${country.availableServerCount} ${strings.serversAvailableFormat}"
          } else {
            strings.serverNotAvailable
          },
          style = MaterialTheme.typography.labelMedium,
          color = if (country.availableServerCount > 0) NovaEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun CountryRowCard(
  country: CountryLocation,
  onToggleFavorite: () -> Unit,
  onClick: () -> Unit,
) {
  val strings = LocalAppStrings.current
  val hasServers = country.totalServerCount > 0

  NovaGlassCard(
    onClick = onClick,
    accentBorder = if (country.availableServerCount > 0) NovaCyan.copy(alpha = 0.35f) else null,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.weight(1f),
      ) {
        Text(text = country.flagEmoji, fontSize = 28.sp)
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (strings.isArabic) country.nameAr else country.nameEn,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "${strings.regionLabel(country.region)} • ${country.isoCode}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.height(4.dp))
          if (hasServers) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "${country.availableServerCount}/${country.totalServerCount} ${strings.serversAvailableFormat}",
                style = MaterialTheme.typography.labelMedium,
                color = if (country.availableServerCount > 0) NovaEmerald else NovaAmber,
              )
              country.bestLatencyMs?.let { ping ->
                Text(
                  text = "${ping}ms",
                  fontFamily = JetBrainsMonoFamily,
                  fontSize = 11.sp,
                  color = NovaCyan,
                )
              }
              country.averageLoadPercent?.let { load ->
                Text(
                  text = "$load%",
                  fontFamily = JetBrainsMonoFamily,
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          } else {
            Text(
              text = strings.serverNotAvailable,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            )
          }
        }
      }

      IconButton(
        onClick = onToggleFavorite,
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Icon(
          imageVector = if (country.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
          contentDescription = strings.favoritesSection,
          tint = if (country.isFavorite) NovaAmber else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
