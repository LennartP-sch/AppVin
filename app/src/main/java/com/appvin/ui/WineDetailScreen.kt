package com.appvin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.appvin.data.Bottle
import com.appvin.data.LocationWithCount
import com.appvin.data.Wine
import com.appvin.data.formatDate
import com.appvin.data.parseSources
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WineDetailScreen(vm: CellarViewModel, wineId: Long, onBack: () -> Unit, onEdit: () -> Unit, onAsk: (String) -> Unit) {
    val wine by remember(wineId) { vm.repo.wine(wineId) }.collectAsStateWithLifecycle(null)
    val bottles by remember(wineId) { vm.repo.bottlesForWine(wineId) }.collectAsStateWithLifecycle(emptyList())
    val locations by vm.locations.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmDelete by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<Bottle?>(null) }
    var moving by remember { mutableStateOf<Bottle?>(null) }
    var adding by remember { mutableStateOf(false) }

    LaunchedEffect(error) {
        error?.let { snackbar.showSnackbar(it); vm.clearError() }
    }

    val w = wine ?: return
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (refreshing == w.id) {
                        CircularProgressIndicator(Modifier.size(24.dp).padding(2.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { vm.refreshInfo(w) }) { Icon(Icons.Filled.Refresh, "Update from the web") }
                    }
                    IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Edit") }
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Delete") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            if (w.labelImagePath != null) {
                AsyncImage(
                    model = File(w.labelImagePath), contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(24.dp)),
                )
            } else {
                Box(
                    Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(24.dp)).background(wineTint(w.color).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.WineBar, null, Modifier.size(48.dp), tint = wineTint(w.color)) }
            }
            Spacer(Modifier.height(16.dp))
            if (w.producer.isNotBlank()) Text(w.producer, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(w.name.ifBlank { w.producer }, style = MaterialTheme.typography.headlineMedium)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                ColorDot(w.color)
                Text(
                    listOf(w.vintage?.toString() ?: "NV", w.color, w.region, w.country).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            DrinkStatusPill(w)

            Card(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row {
                        StatBlock("${bottles.size}", "in cellar", Modifier.weight(1f))
                        StatBlock(w.valuePerBottle?.let { formatMoney(it, w.currency) } ?: "–", "per bottle", Modifier.weight(1f))
                    }
                    if (w.drinkFrom != null && w.drinkUntil != null) {
                        Spacer(Modifier.height(16.dp))
                        Text("Drinking window", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(6.dp))
                        DrinkWindowBar(w)
                    }
                    if (w.infoUpdatedAt == null && refreshing != w.id) {
                        TextButton(onClick = { vm.refreshInfo(w) }) { Text("Look up drinking window and value") }
                    }
                }
            }

            FilledTonalButton(
                onClick = { onAsk("Tell me about my ${w.displayName} ${w.vintage ?: ""}. Is now a good time to open it, and what should I serve with it?") },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(52.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, null)
                Spacer(Modifier.width(8.dp))
                Text("Ask the sommelier about this wine")
            }

            if (w.description.isNotBlank()) { SectionTitle("About"); Text(w.description, style = MaterialTheme.typography.bodyLarge) }
            if (w.foodPairings.isNotBlank()) { SectionTitle("Food pairings"); Text(w.foodPairings, style = MaterialTheme.typography.bodyLarge) }
            if (w.servingTips.isNotBlank()) { SectionTitle("Serving"); Text(w.servingTips, style = MaterialTheme.typography.bodyLarge) }

            SectionTitle("Bottles")
            bottles.forEach { bottle ->
                val place = listOfNotNull(locations.firstOrNull { it.location.id == bottle.locationId }?.location?.name, bottle.slot.ifBlank { null })
                    .joinToString(" · ").ifBlank { "No location" }
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).clickable { moving = bottle }) {
                        Text(place, style = MaterialTheme.typography.bodyLarge)
                        Text("Added ${formatDate(bottle.addedAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = { removing = bottle }) { Text("Remove") }
                }
                HorizontalDivider()
            }
            TextButton(onClick = { adding = true }) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.width(4.dp))
                Text("Add bottles")
            }

            SectionTitle("Details")
            LabeledValue("Appellation", w.appellation)
            LabeledValue("Grapes", w.grapes)
            LabeledValue("Bottle", listOf(w.bottleSize, w.alcohol).filter { it.isNotBlank() }.joinToString(" · "))
            LabeledValue("Price paid", w.purchasePrice?.let { formatMoney(it, w.currency) } ?: "")
            LabeledValue("Tags", w.tagList.joinToString(", "))
            LabeledValue("Notes", w.notes)

            val sources = parseSources(w.sources)
            if (sources.isNotEmpty()) {
                SectionTitle("Sources")
                val uriHandler = LocalUriHandler.current
                sources.forEach { (title, url) ->
                    Text(
                        title, color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline,
                        modifier = Modifier.padding(vertical = 4.dp).clickable { uriHandler.openUri(url) },
                    )
                }
            }
            w.infoUpdatedAt?.let {
                Text(
                    "Web info updated ${formatDate(it)} with Gemini and Google Search. Values are estimates.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this wine?") },
            text = { Text("This deletes the wine and all its bottles, including its history. To record that you drank a bottle, use Remove instead.") },
            confirmButton = { TextButton(onClick = { vm.deleteWine(w); onBack() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
    removing?.let { bottle ->
        RemoveBottleDialog(onDismiss = { removing = null }) { reason, rating, note ->
            vm.removeBottle(bottle, reason, rating, note)
            removing = null
        }
    }
    moving?.let { bottle ->
        PlaceDialog("Move bottle", locations, bottle.locationId, bottle.slot, showCount = false, onDismiss = { moving = null }) { _, loc, slot ->
            vm.moveBottle(bottle, loc, slot)
            moving = null
        }
    }
    if (adding) {
        PlaceDialog("Add bottles", locations, bottles.firstOrNull()?.locationId, "", showCount = true, onDismiss = { adding = false }) { count, loc, slot ->
            vm.addBottles(w.id, count, loc, slot)
            adding = false
        }
    }
}

@Composable
private fun RemoveBottleDialog(onDismiss: () -> Unit, onConfirm: (String, Int?, String) -> Unit) {
    var reason by remember { mutableStateOf("Drunk") }
    var rating by remember { mutableIntStateOf(0) }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove a bottle") },
        text = {
            Column {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Drunk", "Gifted", "Sold").forEach { r -> FilterChip(reason == r, onClick = { reason = r }, label = { Text(r) }) }
                }
                if (reason == "Drunk") {
                    Text("How was it?", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
                    Row {
                        (1..5).forEach { star ->
                            IconButton(onClick = { rating = star }) {
                                Icon(if (star <= rating) Icons.Filled.Star else Icons.Filled.StarBorder, "$star stars", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(reason, rating.takeIf { it > 0 && reason == "Drunk" }, note) }) { Text("Remove") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PlaceDialog(
    title: String,
    locations: List<LocationWithCount>,
    initialLocation: Long?,
    initialSlot: String,
    showCount: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int, Long?, String) -> Unit,
) {
    var count by remember { mutableIntStateOf(1) }
    var location by remember { mutableStateOf(initialLocation) }
    var slot by remember { mutableStateOf(initialSlot) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (showCount) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalButton(onClick = { if (count > 1) count-- }) { Text("−") }
                        Text("$count", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 20.dp))
                        FilledTonalButton(onClick = { count++ }) { Text("+") }
                    }
                }
                LocationPicker(locations, location, { location = it })
                OutlinedTextField(slot, { slot = it }, label = { Text("Rack / shelf (optional)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(count, location, slot) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWineScreen(vm: CellarViewModel, wineId: Long, onDone: () -> Unit) {
    val stored by remember(wineId) { vm.repo.wine(wineId) }.collectAsStateWithLifecycle(null)
    var wine by remember(stored != null) { mutableStateOf<Wine?>(stored) }
    val current = wine ?: return
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit wine") },
                navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = { TextButton(onClick = { vm.updateWine(current); onDone() }) { Text("Save") } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            WineForm(current, onChange = { wine = it })
            Spacer(Modifier.height(40.dp))
        }
    }
}
