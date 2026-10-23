package com.appvin.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.appvin.data.Wine
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWineScreen(vm: CellarViewModel, onBack: () -> Unit, onSaved: (Long) -> Unit) {
    val scan by vm.scan.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) cameraUri?.let(vm::scanLabel)
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(vm::scanLabel)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add wine") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            when (val state = scan) {
                ScanState.Idle -> Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Take a clear photo of the front label. Gemini reads it and searches the web for the drinking window and current value.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Button(
                        onClick = {
                            val file = vm.repo.newCameraFile()
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                            cameraUri = uri
                            takePicture.launch(uri)
                        },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                    ) {
                        Icon(Icons.Filled.CameraAlt, null)
                        Spacer(Modifier.width(12.dp))
                        Text("Take photo of label", style = MaterialTheme.typography.titleMedium)
                    }
                    FilledTonalButton(
                        onClick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, null)
                        Spacer(Modifier.width(12.dp))
                        Text("Choose from gallery")
                    }
                    OutlinedButton(onClick = vm::startManualEntry, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Icon(Icons.Filled.Edit, null)
                        Spacer(Modifier.width(12.dp))
                        Text("Enter by hand")
                    }
                }

                is ScanState.Working -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    state.photoPath?.let {
                        AsyncImage(
                            model = File(it), contentDescription = null, contentScale = ContentScale.Crop,
                            modifier = Modifier.size(220.dp).clip(RoundedCornerShape(24.dp)),
                        )
                        Spacer(Modifier.height(24.dp))
                    }
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(state.step, style = MaterialTheme.typography.titleMedium)
                }

                is ScanState.Draft -> DraftForm(vm, state, onSaved)
            }
        }
    }
}

@Composable
private fun DraftForm(vm: CellarViewModel, state: ScanState.Draft, onSaved: (Long) -> Unit) {
    val locations by vm.locations.collectAsStateWithLifecycle()
    var wine by remember(state) { mutableStateOf(state.wine) }
    var count by rememberSaveable { mutableIntStateOf(1) }
    var locationId by rememberSaveable { mutableStateOf<Long?>(null) }
    var slot by rememberSaveable { mutableStateOf("") }

    Column(Modifier.verticalScroll(rememberScrollState()).imePadding().padding(16.dp)) {
        state.message?.let {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
            Spacer(Modifier.height(12.dp))
        }
        wine.labelImagePath?.let {
            AsyncImage(
                model = File(it), contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(20.dp)),
            )
            Spacer(Modifier.height(12.dp))
        }
        Text("Check the details and correct anything Gemini got wrong.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        WineForm(wine, onChange = { wine = it })
        OutlinedButton(onClick = { vm.researchDraft(wine) }, enabled = wine.name.isNotBlank() || wine.producer.isNotBlank()) {
            Icon(Icons.Filled.TravelExplore, null)
            Spacer(Modifier.width(8.dp))
            Text("Search the web again")
        }

        SectionTitle("Bottles")
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalButton(onClick = { if (count > 1) count-- }) { Text("−", style = MaterialTheme.typography.titleLarge) }
            Text("$count", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 24.dp))
            FilledTonalButton(onClick = { count++ }) { Text("+", style = MaterialTheme.typography.titleLarge) }
        }
        Spacer(Modifier.height(12.dp))
        LocationPicker(locations, locationId, { locationId = it }, Modifier.fillMaxWidth())
        OutlinedTextField(slot, { slot = it }, label = { Text("Rack / shelf (optional)") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { vm.saveDraft(wine, count, locationId, slot, onSaved) },
            enabled = wine.name.isNotBlank() || wine.producer.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text("Save to cellar", style = MaterialTheme.typography.titleMedium) }
        Spacer(Modifier.height(32.dp))
    }
}

/** Editable fields of a wine, shared by adding and editing. */
@Composable
fun WineForm(wine: Wine, onChange: (Wine) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 12.dp)) {
        TextInput("Producer", wine.producer) { onChange(wine.copy(producer = it)) }
        TextInput("Wine", wine.name) { onChange(wine.copy(name = it)) }
        NumberInput("Vintage", wine.vintage) { onChange(wine.copy(vintage = it?.toInt())) }
        Text("Colour", style = MaterialTheme.typography.labelLarge)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WineColors.forEach { c ->
                FilterChip(wine.color == c, onClick = { onChange(wine.copy(color = c)) }, label = { Text(c) })
            }
        }
        TextInput("Country", wine.country) { onChange(wine.copy(country = it)) }
        TextInput("Region", wine.region) { onChange(wine.copy(region = it)) }
        TextInput("Appellation", wine.appellation) { onChange(wine.copy(appellation = it)) }
        TextInput("Grapes", wine.grapes) { onChange(wine.copy(grapes = it)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextInput("Bottle size", wine.bottleSize, Modifier.weight(1f)) { onChange(wine.copy(bottleSize = it)) }
            TextInput("Alcohol", wine.alcohol, Modifier.weight(1f)) { onChange(wine.copy(alcohol = it)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberInput("Drink from", wine.drinkFrom, Modifier.weight(1f)) { onChange(wine.copy(drinkFrom = it?.toInt())) }
            NumberInput("Drink until", wine.drinkUntil, Modifier.weight(1f)) { onChange(wine.copy(drinkUntil = it?.toInt())) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberInput("Value per bottle (€)", wine.valuePerBottle, Modifier.weight(1f)) { onChange(wine.copy(valuePerBottle = it)) }
            NumberInput("Price paid (€)", wine.purchasePrice, Modifier.weight(1f)) { onChange(wine.copy(purchasePrice = it)) }
        }
        TextInput("Tags (comma separated)", wine.tags) { onChange(wine.copy(tags = it)) }
        TextInput("Notes", wine.notes, singleLine = false) { onChange(wine.copy(notes = it)) }
    }
}

@Composable
private fun TextInput(label: String, value: String, modifier: Modifier = Modifier.fillMaxWidth(), singleLine: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = singleLine, modifier = modifier)
}

/** Keeps its own text so partial input such as "12." survives; reports a parsed number or null. */
@Composable
private fun NumberInput(label: String, value: Number?, modifier: Modifier = Modifier.fillMaxWidth(), onChange: (Double?) -> Unit) {
    fun show(n: Number?) = when (n) { null -> ""; is Double -> if (n % 1.0 == 0.0) n.toLong().toString() else n.toString(); else -> n.toString() }
    var text by remember { mutableStateOf(show(value)) }
    if (text.replace(',', '.').toDoubleOrNull() != value?.toDouble()) text = show(value)
    OutlinedTextField(
        value = text,
        onValueChange = { new ->
            text = new
            onChange(new.replace(',', '.').toDoubleOrNull())
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}
