package com.appvin.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.util.Calendar

/** A wine label (producer + name + vintage). Physical copies of it are [Bottle]s. */
@Entity
data class Wine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val producer: String = "",
    val vintage: Int? = null,
    val color: String = "",
    val country: String = "",
    val region: String = "",
    val appellation: String = "",
    val grapes: String = "",
    val bottleSize: String = "750 ml",
    val alcohol: String = "",
    val labelImagePath: String? = null,
    // Filled in by Gemini + Google Search
    val drinkFrom: Int? = null,
    val drinkUntil: Int? = null,
    val peakYear: Int? = null,
    val valuePerBottle: Double? = null,
    val currency: String = "EUR",
    val description: String = "",
    val foodPairings: String = "",
    val servingTips: String = "",
    /** One "title\turl" per line. */
    val sources: String = "",
    val infoUpdatedAt: Long? = null,
    // Owner's own data
    val purchasePrice: Double? = null,
    val tags: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
) {
    val displayName: String
        get() = listOf(producer, name).filter { it.isNotBlank() }.distinct().joinToString(" · ")

    val drinkStatus: DrinkStatus
        get() {
            val year = Calendar.getInstance().get(Calendar.YEAR)
            return when {
                drinkFrom == null && drinkUntil == null -> DrinkStatus.UNKNOWN
                drinkFrom != null && year < drinkFrom -> DrinkStatus.TOO_YOUNG
                drinkUntil != null && year > drinkUntil -> DrinkStatus.PAST_PEAK
                else -> DrinkStatus.READY
            }
        }

    val tagList: List<String>
        get() = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}

enum class DrinkStatus { READY, TOO_YOUNG, PAST_PEAK, UNKNOWN }

@Entity
data class StorageLocation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: String = "Cellar",
)

@Entity(
    foreignKeys = [
        ForeignKey(entity = Wine::class, parentColumns = ["id"], childColumns = ["wineId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = StorageLocation::class, parentColumns = ["id"], childColumns = ["locationId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("wineId"), Index("locationId")],
)
data class Bottle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wineId: Long,
    val locationId: Long? = null,
    /** Free text such as "Rack B, row 3". */
    val slot: String = "",
    val addedAt: Long = System.currentTimeMillis(),
    val removedAt: Long? = null,
    /** "Drunk", "Gifted" or "Sold" once removed. */
    val removalReason: String? = null,
    val removalNote: String = "",
    /** 1–5 stars given when the bottle was opened. */
    val rating: Int? = null,
)

@Entity
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromUser: Boolean,
    val text: String,
    val sources: String = "",
    /** Google Search suggestion chip HTML, which must be shown with grounded answers. */
    val searchSuggestionsHtml: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/** A wine together with how many bottles of it are in the cellar. */
data class WineWithCount(
    @Embedded val wine: Wine,
    val bottleCount: Int,
)

data class BottleWithWine(
    @Embedded val bottle: Bottle,
    @Relation(parentColumn = "wineId", entityColumn = "id") val wine: Wine,
)

data class LocationWithCount(
    @Embedded val location: StorageLocation,
    val bottleCount: Int,
)

fun parseSources(raw: String): List<Pair<String, String>> =
    raw.lines().mapNotNull { line ->
        val parts = line.split("\t")
        if (parts.size == 2 && parts[1].isNotBlank()) parts[0] to parts[1] else null
    }

fun formatSources(sources: List<Pair<String, String>>): String =
    sources.distinctBy { it.second }.joinToString("\n") { "${it.first}\t${it.second}" }
