package com.appvin.ui

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.appvin.AppVinApp
import com.appvin.ai.Sommelier
import com.appvin.data.Bottle
import com.appvin.data.StorageLocation
import com.appvin.data.Wine
import com.appvin.data.applyResearch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed interface ScanState {
    data object Idle : ScanState
    data class Working(val step: String, val photoPath: String?) : ScanState
    /** A wine ready to review and save; [message] explains anything that went wrong on the way. */
    data class Draft(val wine: Wine, val message: String? = null) : ScanState
}

class CellarViewModel(app: Application) : AndroidViewModel(app) {
    val repo = (app as AppVinApp).repository

    private fun <T> listState(flow: Flow<List<T>>): StateFlow<List<T>> =
        flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val wines = listState(repo.winesInCellar)
    val activeBottles = listState(repo.activeBottles)
    val locations = listState(repo.locations)
    val pastBottles = listState(repo.pastBottles)
    val recentActivity = listState(repo.recentActivity)
    val chatMessages = listState(repo.chatMessages)

    private val _scan = MutableStateFlow<ScanState>(ScanState.Idle)
    val scan = _scan.asStateFlow()

    private val _chatBusy = MutableStateFlow(false)
    val chatBusy = _chatBusy.asStateFlow()

    /** Id of the wine whose web info is being refreshed. */
    private val _refreshing = MutableStateFlow<Long?>(null)
    val refreshing = _refreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    fun clearError() { _error.value = null }

    fun startManualEntry() { _scan.value = ScanState.Draft(Wine(name = "")) }
    fun resetScan() { _scan.value = ScanState.Idle }

    /** Photo → Gemini reads the label → Gemini searches the web → a draft to review. */
    fun scanLabel(uri: Uri) = viewModelScope.launch {
        _scan.value = ScanState.Working("Saving photo…", null)
        val path = try {
            repo.saveLabelPhoto(uri)
        } catch (e: Exception) {
            _scan.value = ScanState.Draft(Wine(name = ""), "Could not read the photo: ${e.message}")
            return@launch
        }
        _scan.value = ScanState.Working("Reading the label…", path)
        val wine = try {
            val label = Sommelier.readLabel(BitmapFactory.decodeFile(path))
            Wine(
                name = label.name, producer = label.producer, vintage = label.vintage, color = label.color,
                country = label.country, region = label.region, appellation = label.appellation,
                grapes = label.grapes, bottleSize = label.bottleSize, alcohol = label.alcohol, labelImagePath = path,
            )
        } catch (e: Exception) {
            _scan.value = ScanState.Draft(Wine(name = "", labelImagePath = path), "Could not read the label: ${e.message}")
            return@launch
        }
        _scan.value = ScanState.Working("Searching the web for drinking window and value…", path)
        _scan.value = try {
            ScanState.Draft(wine.applyResearch(Sommelier.research(wine)))
        } catch (e: Exception) {
            ScanState.Draft(wine, "Label read, but the web search failed: ${e.message}")
        }
    }

    /** Re-runs the web lookup on the draft after the user corrected the label details. */
    fun researchDraft(wine: Wine) = viewModelScope.launch {
        _scan.value = ScanState.Working("Searching the web for drinking window and value…", wine.labelImagePath)
        _scan.value = try {
            ScanState.Draft(wine.applyResearch(Sommelier.research(wine)))
        } catch (e: Exception) {
            ScanState.Draft(wine, "Web search failed: ${e.message}")
        }
    }

    fun saveDraft(wine: Wine, count: Int, locationId: Long?, slot: String, onSaved: (Long) -> Unit) =
        viewModelScope.launch {
            val id = repo.addWine(wine, count, locationId, slot)
            _scan.value = ScanState.Idle
            onSaved(id)
        }

    fun refreshInfo(wine: Wine) = viewModelScope.launch {
        _refreshing.value = wine.id
        try {
            repo.refreshInfo(wine)
        } catch (e: Exception) {
            _error.value = "Web search failed: ${e.message}"
        }
        _refreshing.value = null
    }

    fun updateWine(wine: Wine) = viewModelScope.launch { repo.updateWine(wine) }
    fun deleteWine(wine: Wine) = viewModelScope.launch { repo.deleteWine(wine) }
    fun addBottles(wineId: Long, count: Int, locationId: Long?, slot: String) =
        viewModelScope.launch { repo.addBottles(wineId, count, locationId, slot) }
    fun moveBottle(bottle: Bottle, locationId: Long?, slot: String) =
        viewModelScope.launch { repo.updateBottle(bottle.copy(locationId = locationId, slot = slot)) }
    fun removeBottle(bottle: Bottle, reason: String, rating: Int?, note: String) =
        viewModelScope.launch { repo.removeBottle(bottle, reason, rating, note) }

    fun addLocation(name: String, kind: String) = viewModelScope.launch { repo.addLocation(name, kind) }
    fun updateLocation(location: StorageLocation) = viewModelScope.launch { repo.updateLocation(location) }
    fun deleteLocation(location: StorageLocation) = viewModelScope.launch { repo.deleteLocation(location) }

    fun sendChat(text: String) = viewModelScope.launch {
        _chatBusy.value = true
        repo.sendChat(text)
        _chatBusy.value = false
    }

    fun clearChat() = viewModelScope.launch { repo.clearChat() }

    fun exportCsv(onReady: (File) -> Unit) = viewModelScope.launch { onReady(repo.exportCsv()) }
}
