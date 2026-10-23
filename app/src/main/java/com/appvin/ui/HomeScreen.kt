package com.appvin.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appvin.ai.Sommelier
import com.appvin.data.DrinkStatus
import com.appvin.data.formatDate
import java.util.Calendar

@Composable
fun HomeScreen(vm: CellarViewModel, onOpenWine: (Long) -> Unit, onAdd: () -> Unit, onAsk: () -> Unit) {
    val wines by vm.wines.collectAsStateWithLifecycle()
    val activity by vm.recentActivity.collectAsStateWithLifecycle()
    val bottleCount = wines.sumOf { it.bottleCount }
    val value = wines.sumOf { (it.wine.valuePerBottle ?: 0.0) * it.bottleCount }
    val ready = wines.filter { it.wine.drinkStatus == DrinkStatus.READY || it.wine.drinkStatus == DrinkStatus.PAST_PEAK }
        .sortedBy { it.wine.drinkUntil ?: Int.MAX_VALUE }
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) { in 5..11 -> "Good morning"; in 12..17 -> "Good afternoon"; else -> "Good evening" }

    LazyColumn(Modifier.statusBarsPadding(), contentPadding = PaddingValues(20.dp)) {
        item {
            Text(greeting, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(Modifier.fillMaxWidth().padding(20.dp)) {
                    StatBlock("$bottleCount", "bottles", Modifier.weight(1f))
                    StatBlock(if (value > 0) formatMoney(value, Sommelier.CURRENCY) else "–", "estimated value", Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onAdd, modifier = Modifier.weight(1f).height(56.dp)) {
                    Icon(Icons.Filled.CameraAlt, null, Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(8.dp))
                    Text("Scan a label")
                }
                FilledTonalButton(onClick = onAsk, modifier = Modifier.weight(1f).height(56.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Chat, null, Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(8.dp))
                    Text("Ask sommelier")
                }
            }
        }

        if (ready.isNotEmpty()) {
            item {
                SectionTitle("Ready to drink")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(ready, key = { it.wine.id }) { (wine, count) ->
                        ElevatedCard(Modifier.width(170.dp).clickable { onOpenWine(wine.id) }) {
                            Column(Modifier.padding(12.dp)) {
                                WineThumb(wine, 64.dp)
                                Spacer(Modifier.height(8.dp))
                                Text(wine.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${wine.vintage ?: "NV"} · $count btl · until ${wine.drinkUntil ?: "?"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (wines.isEmpty()) {
            item {
                SectionTitle("Get started")
                Text(
                    "Take a photo of a wine label. Gemini reads it, looks the wine up on the web and fills in when to drink it and what it's worth.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        if (activity.isNotEmpty()) {
            item { SectionTitle("Recent activity") }
            items(activity, key = { it.bottle.id }) { (bottle, wine) ->
                val removed = bottle.removedAt != null
                ListItem(
                    leadingContent = { WineThumb(wine, 44.dp) },
                    headlineContent = { Text(wine.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = {
                        Text(if (removed) "${bottle.removalReason} · ${formatDate(bottle.removedAt!!)}" else "Added · ${formatDate(bottle.addedAt)}")
                    },
                    modifier = Modifier.clickable { onOpenWine(wine.id) },
                )
            }
        }
    }
}
