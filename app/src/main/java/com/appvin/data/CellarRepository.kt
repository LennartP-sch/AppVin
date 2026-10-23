package com.appvin.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.appvin.ai.Sommelier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class CellarRepository(private val context: Context, private val dao: CellarDao) {

    val winesInCellar = dao.winesInCellar()
    val activeBottles = dao.activeBottles()
    val pastBottles = dao.pastBottles()
    val recentActivity = dao.recentActivity()
    val locations = dao.locations()
    val chatMessages = dao.chatMessages()

    fun wine(id: Long) = dao.wine(id)
    fun bottlesForWine(id: Long) = dao.activeBottlesForWine(id)

    suspend fun addWine(wine: Wine, count: Int, locationId: Long?, slot: String): Long {
        val id = dao.insertWine(wine)
        addBottles(id, count, locationId, slot)
        return id
    }

    suspend fun addBottles(wineId: Long, count: Int, locationId: Long?, slot: String) =
        dao.insertBottles(List(count) { Bottle(wineId = wineId, locationId = locationId, slot = slot) })

    suspend fun updateWine(wine: Wine) = dao.updateWine(wine)

    suspend fun deleteWine(wine: Wine) {
        dao.deleteWine(wine)
        wine.labelImagePath?.let { File(it).delete() }
    }

    suspend fun updateBottle(bottle: Bottle) = dao.updateBottle(bottle)

    suspend fun removeBottle(bottle: Bottle, reason: String, rating: Int?, note: String) =
        dao.updateBottle(
            bottle.copy(removedAt = System.currentTimeMillis(), removalReason = reason, rating = rating, removalNote = note)
        )

    suspend fun addLocation(name: String, kind: String) = dao.insertLocation(StorageLocation(name = name, kind = kind))
    suspend fun updateLocation(location: StorageLocation) = dao.updateLocation(location)
    suspend fun deleteLocation(location: StorageLocation) = dao.deleteLocation(location)

    /** Runs the Google Search lookup and stores the result on the wine. */
    suspend fun refreshInfo(wine: Wine): Wine {
        val research = Sommelier.research(wine)
        val updated = wine.applyResearch(research)
        dao.updateWine(updated)
        return updated
    }

    suspend fun sendChat(text: String) {
        dao.insertChatMessage(ChatMessage(fromUser = true, text = text))
        val history = dao.chatMessages().first()
        val reply = try {
            Sommelier.chat(history, describeCellar())
        } catch (e: Exception) {
            Sommelier.Reply("Sorry, something went wrong: ${e.message}", "", null)
        }
        dao.insertChatMessage(
            ChatMessage(fromUser = false, text = reply.text, sources = reply.sources, searchSuggestionsHtml = reply.searchSuggestionsHtml)
        )
    }

    suspend fun clearChat() = dao.clearChat()

    /** A compact text listing of the cellar for the sommelier's instructions. */
    private suspend fun describeCellar(): String {
        val wines = dao.winesInCellar().first()
        if (wines.isEmpty()) return "(The cellar is empty.)"
        val bottles = dao.activeBottles().first()
        val locationNames = dao.locations().first().associate { it.location.id to it.location.name }
        return wines.joinToString("\n") { (wine, count) ->
            val places = bottles.filter { it.wineId == wine.id }
                .map { listOfNotNull(locationNames[it.locationId], it.slot.ifBlank { null }).joinToString(" ") }
                .filter { it.isNotBlank() }.distinct().joinToString("; ")
            listOf(
                "${wine.displayName} ${wine.vintage ?: "NV"}",
                wine.color,
                listOf(wine.region, wine.country).filter { it.isNotBlank() }.joinToString(", "),
                wine.grapes,
                "$count bottle(s)",
                if (places.isNotBlank()) "stored: $places" else "",
                if (wine.drinkFrom != null || wine.drinkUntil != null) "drink ${wine.drinkFrom ?: "?"}–${wine.drinkUntil ?: "?"}" else "",
                wine.valuePerBottle?.let { "about ${it.toInt()} ${wine.currency}" } ?: "",
            ).filter { it.isNotBlank() }.joinToString(" | ", prefix = "- ")
        }
    }

    /** Copies a photo into app storage, downscaled, and returns its path. */
    suspend fun saveLabelPhoto(uri: Uri): String = withContext(Dispatchers.IO) {
        val bitmap = loadScaled(uri)
        val dir = File(context.filesDir, "labels").apply { mkdirs() }
        val file = File(dir, "label_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        file.absolutePath
    }

    fun newCameraFile(): File =
        File(File(context.cacheDir, "camera").apply { mkdirs() }, "shot_${System.currentTimeMillis()}.jpg")

    private fun loadScaled(uri: Uri, maxSide: Int = 1600): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("Could not read the photo")
        return rotateFromExif(uri, decoded)
    }

    private fun rotateFromExif(uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = context.contentResolver.openInputStream(uri).use { stream ->
            stream?.let { androidx.exifinterface.media.ExifInterface(it).rotationDegrees } ?: 0
        }
        if (orientation == 0) return bitmap
        val matrix = android.graphics.Matrix().apply { postRotate(orientation.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /** Writes every bottle (in the cellar and past) to a CSV file and returns it. */
    suspend fun exportCsv(): File = withContext(Dispatchers.IO) {
        val bottles = dao.allBottlesWithWine()
        val locationNames = dao.locations().first().associate { it.location.id to it.location.name }
        val header = listOf(
            "Producer", "Wine", "Vintage", "Colour", "Country", "Region", "Appellation", "Grapes", "Size",
            "Location", "Slot", "Added", "Removed", "Reason", "Rating", "Drink from", "Drink until",
            "Value", "Currency", "Purchase price", "Tags", "Notes",
        )
        val rows = bottles.map { (b, w) ->
            listOf(
                w.producer, w.name, w.vintage?.toString() ?: "", w.color, w.country, w.region, w.appellation, w.grapes,
                w.bottleSize, locationNames[b.locationId] ?: "", b.slot, formatDate(b.addedAt),
                b.removedAt?.let(::formatDate) ?: "", b.removalReason ?: "", b.rating?.toString() ?: "",
                w.drinkFrom?.toString() ?: "", w.drinkUntil?.toString() ?: "", w.valuePerBottle?.toString() ?: "",
                w.currency, w.purchasePrice?.toString() ?: "", w.tags, w.notes,
            )
        }
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        File(dir, "appvin-cellar.csv").apply {
            writeText((listOf(header) + rows).joinToString("\n") { row -> row.joinToString(",", transform = ::csvCell) })
        }
    }

    private fun csvCell(value: String) =
        if (value.any { it == ',' || it == '"' || it == '\n' }) "\"" + value.replace("\"", "\"\"") + "\"" else value
}

fun Wine.applyResearch(r: Sommelier.Research) = copy(
    drinkFrom = r.drinkFrom ?: drinkFrom,
    drinkUntil = r.drinkUntil ?: drinkUntil,
    peakYear = r.peakYear ?: peakYear,
    valuePerBottle = r.valuePerBottle ?: valuePerBottle,
    currency = Sommelier.CURRENCY,
    description = r.description.ifBlank { description },
    foodPairings = r.foodPairings.ifBlank { foodPairings },
    servingTips = r.servingTips.ifBlank { servingTips },
    grapes = grapes.ifBlank { r.grapes },
    sources = r.sources.ifBlank { sources },
    infoUpdatedAt = System.currentTimeMillis(),
)

fun formatDate(millis: Long): String =
    java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(java.util.Date(millis))
