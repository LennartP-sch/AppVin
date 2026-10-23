package com.appvin.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appvin.data.StorageLocation
import com.appvin.data.formatDate

@Composable
fun MoreScreen(vm: CellarViewModel, onLocations: () -> Unit, onPast: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(top = 16.dp)) {
        Text("More", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        ListItem(
            headlineContent = { Text("Cellars & fridges") },
            supportingContent = { Text("Where your bottles are stored") },
            leadingContent = { Icon(Icons.Filled.Warehouse, null) },
            modifier = Modifier.clickable(onClick = onLocations),
        )
        ListItem(
            headlineContent = { Text("Past bottles") },
            supportingContent = { Text("Everything drunk, gifted or sold, with your ratings") },
            leadingContent = { Icon(Icons.Filled.History, null) },
            modifier = Modifier.clickable(onClick = onPast),
        )
        ListItem(
            headlineContent = { Text("Export to spreadsheet") },
            supportingContent = { Text("Share the whole cellar as a CSV file") },
            leadingContent = { Icon(Icons.Filled.FileDownload, null) },
            modifier = Modifier.clickable {
                vm.exportCsv { file ->
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                    val share = Intent(Intent.ACTION_SEND).setType("text/csv").putExtra(Intent.EXTRA_STREAM, uri)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(Intent.createChooser(share, "Export cellar"))
                }
            },
        )
        Text(
            "Label reading, wine information and the sommelier use Google Gemini with Google Search. Drinking windows and values are estimates.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(20.dp),
        )
    }
}

private val locationKinds = listOf("Cellar", "Fridge", "Rack", "Other")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationsScreen(vm: CellarViewModel, onBack: () -> Unit) {
    val locations by vm.locations.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<StorageLocation?>(null) }
    var deleting by remember { mutableStateOf<StorageLocation?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cellars & fridges") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = StorageLocation(name = "") }) { Icon(Icons.Filled.Add, "Add location") }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            if (locations.isEmpty()) {
                item {
                    Text(
                        "Add your cellar, wine fridge or racks so you always know where each bottle is.",
                        style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(20.dp),
                    )
                }
            }
            items(locations, key = { it.location.id }) { (location, count) ->
                ListItem(
                    headlineContent = { Text(location.name) },
                    supportingContent = { Text("${location.kind} · $count bottles") },
                    leadingContent = { Icon(if (location.kind == "Fridge") Icons.Filled.Kitchen else Icons.Filled.Warehouse, null) },
                    trailingContent = { IconButton(onClick = { deleting = location }) { Icon(Icons.Filled.Delete, "Delete") } },
                    modifier = Modifier.clickable { editing = location },
                )
            }
        }
    }

    editing?.let { location ->
        var name by remember(location) { mutableStateOf(location.name) }
        var kind by remember(location) { mutableStateOf(location.kind) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(if (location.id == 0L) "New location" else "Edit location") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        locationKinds.forEach { k -> FilterChip(kind == k, onClick = { kind = k }, label = { Text(k) }) }
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    if (location.id == 0L) vm.addLocation(name.trim(), kind) else vm.updateLocation(location.copy(name = name.trim(), kind = kind))
                    editing = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }
    deleting?.let { location ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete ${location.name}?") },
            text = { Text("The bottles stay in your cellar but will have no location.") },
            confirmButton = { TextButton(onClick = { vm.deleteLocation(location); deleting = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastBottlesScreen(vm: CellarViewModel, onBack: () -> Unit, onOpenWine: (Long) -> Unit) {
    val past by vm.pastBottles.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Past bottles") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (past.isEmpty()) {
                item { Text("No bottles removed yet.", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(20.dp)) }
            }
            items(past, key = { it.bottle.id }) { (bottle, wine) ->
                ListItem(
                    leadingContent = { WineThumb(wine, 48.dp) },
                    headlineContent = { Text("${wine.displayName} ${wine.vintage ?: ""}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = {
                        Column {
                            Text("${bottle.removalReason} · ${formatDate(bottle.removedAt!!)}")
                            if (bottle.removalNote.isNotBlank()) Text(bottle.removalNote, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    },
                    trailingContent = {
                        bottle.rating?.let {
                            Row {
                                Text("$it")
                                Icon(Icons.Filled.Star, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    modifier = Modifier.clickable { onOpenWine(wine.id) },
                )
            }
        }
    }
}
