package com.appvin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.appvin.data.DrinkStatus
import com.appvin.data.LocationWithCount
import com.appvin.data.Wine
import java.io.File
import java.text.NumberFormat
import java.util.Currency

val WineColors = listOf("Red", "White", "Rosé", "Sparkling", "Dessert", "Fortified", "Orange")

/** The colour of the wine itself, used as a small accent dot. */
fun wineTint(color: String): Color = when (color) {
    "Red" -> Color(0xFF7B1E33)
    "White" -> Color(0xFFE6D27A)
    "Rosé" -> Color(0xFFF2A0A8)
    "Sparkling" -> Color(0xFFE9DFA8)
    "Dessert" -> Color(0xFFD99A2B)
    "Fortified" -> Color(0xFF5A2A1A)
    "Orange" -> Color(0xFFE08A3C)
    else -> Color(0xFF9E8E8E)
}

fun formatMoney(amount: Double, currency: String): String =
    NumberFormat.getCurrencyInstance().apply {
        this.currency = Currency.getInstance(currency)
        maximumFractionDigits = 0
    }.format(amount)

@Composable
fun WineThumb(wine: Wine, size: Dp = 56.dp) {
    val shape = RoundedCornerShape(12.dp)
    if (wine.labelImagePath != null) {
        AsyncImage(
            model = File(wine.labelImagePath),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).clip(shape),
        )
    } else {
        Box(
            Modifier.size(size).clip(shape).background(wineTint(wine.color).copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.WineBar, contentDescription = null, tint = wineTint(wine.color))
        }
    }
}

@Composable
fun DrinkStatusPill(wine: Wine) {
    val (label, container, content) = when (wine.drinkStatus) {
        DrinkStatus.READY -> Triple("Ready to drink", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        DrinkStatus.TOO_YOUNG -> Triple("Hold until ${wine.drinkFrom}", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        DrinkStatus.PAST_PEAK -> Triple("Drink now – past peak", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        DrinkStatus.UNKNOWN -> return
    }
    Surface(shape = RoundedCornerShape(50), color = container) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = content, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = modifier.padding(top = 24.dp, bottom = 8.dp))
}

@Composable
fun LabeledValue(label: String, value: String) {
    if (value.isBlank()) return
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Dropdown to pick a storage location; null means "no location". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPicker(locations: List<LocationWithCount>, selectedId: Long?, onSelect: (Long?) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val selected = locations.firstOrNull { it.location.id == selectedId }?.location?.name ?: "No location"
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Location") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("No location") }, onClick = { onSelect(null); expanded = false })
            locations.forEach { loc ->
                DropdownMenuItem(text = { Text(loc.location.name) }, onClick = { onSelect(loc.location.id); expanded = false })
            }
        }
    }
}

@Composable
fun StatBlock(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A thin bar showing where today falls in the drinking window. */
@Composable
fun DrinkWindowBar(wine: Wine) {
    val from = wine.drinkFrom ?: return
    val until = wine.drinkUntil ?: return
    if (until <= from) return
    val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    val fraction = ((year - from).toFloat() / (until - from)).coerceIn(0f, 1f)
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            Box(Modifier.fillMaxWidth(fraction).height(10.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary))
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text("$from", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.weight(1f))
            wine.peakYear?.let { Text("Peak $it", style = MaterialTheme.typography.labelMedium) }
            Spacer(Modifier.weight(1f))
            Text("$until", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun ColorDot(color: String) {
    Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(wineTint(color)))
    Spacer(Modifier.width(6.dp))
}
