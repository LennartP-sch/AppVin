package com.appvin.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appvin.data.DrinkStatus

private enum class SortBy(val label: String) { NAME("Name"), VINTAGE("Vintage"), DRINK_BY("Drink by"), VALUE("Value") }

@Composable
fun CollectionScreen(vm: CellarViewModel, onOpenWine: (Long) -> Unit, onAdd: () -> Unit) {
    val wines by vm.wines.collectAsStateWithLifecycle()
    val bottles by vm.activeBottles.collectAsStateWithLifecycle()
    val locations by vm.locations.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var color by rememberSaveable { mutableStateOf<String?>(null) }
    var readyOnly by rememberSaveable { mutableStateOf(false) }
    var locationId by rememberSaveable { mutableStateOf<Long?>(null) }
    var sort by rememberSaveable { mutableStateOf(SortBy.NAME) }

    val shown = wines.filter { (w, _) ->
        val text = listOf(w.name, w.producer, w.region, w.country, w.appellation, w.grapes, w.tags, w.vintage?.toString() ?: "")
            .joinToString(" ")
        (query.isBlank() || query.split(" ").all { text.contains(it, ignoreCase = true) }) &&
            (color == null || w.color == color) &&
            (!readyOnly || w.drinkStatus == DrinkStatus.READY || w.drinkStatus == DrinkStatus.PAST_PEAK) &&
            (locationId == null || bottles.any { it.wineId == w.id && it.locationId == locationId })
    }.let { list ->
        when (sort) {
            SortBy.NAME -> list
            SortBy.VINTAGE -> list.sortedBy { it.wine.vintage ?: Int.MAX_VALUE }
            SortBy.DRINK_BY -> list.sortedBy { it.wine.drinkUntil ?: Int.MAX_VALUE }
            SortBy.VALUE -> list.sortedByDescending { it.wine.valuePerBottle ?: 0.0 }
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAdd, icon = { Icon(Icons.Filled.CameraAlt, null) }, text = { Text("Add wine") })
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = padding.calculateBottomPadding() + 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text("Cellar", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${shown.sumOf { it.bottleCount }} bottles · ${shown.size} wines",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    placeholder = { Text("Search wine, region, grape…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(readyOnly, onClick = { readyOnly = !readyOnly }, label = { Text("Ready to drink") }) }
                    items(WineColors) { c ->
                        FilterChip(color == c, onClick = { color = if (color == c) null else c }, label = { Text(c) })
                    }
                }
                if (locations.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(locations, key = { it.location.id }) { loc ->
                            val id = loc.location.id
                            FilterChip(locationId == id, onClick = { locationId = if (locationId == id) null else id }, label = { Text(loc.location.name) })
                        }
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    item { Text("Sort", style = MaterialTheme.typography.labelLarge) }
                    items(SortBy.entries) { s -> FilterChip(sort == s, onClick = { sort = s }, label = { Text(s.label) }) }
                }
            }
            if (shown.isEmpty()) {
                item {
                    Text(
                        if (wines.isEmpty()) "Your cellar is empty. Tap “Add wine” to scan your first label." else "No wines match.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }
            items(shown, key = { it.wine.id }) { (wine, count) ->
                OutlinedCard(Modifier.fillMaxWidth().clickable { onOpenWine(wine.id) }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        WineThumb(wine, 64.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(wine.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ColorDot(wine.color)
                                Text(
                                    listOf(wine.vintage?.toString() ?: "NV", wine.region.ifBlank { wine.country }).filter { it.isNotBlank() }.joinToString(" · "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            DrinkStatusPill(wine)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("×$count", style = MaterialTheme.typography.titleLarge)
                            wine.valuePerBottle?.let {
                                Text(formatMoney(it, wine.currency), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
