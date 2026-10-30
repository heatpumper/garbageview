package de.garbageview.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.garbageview.GarbageViewApp
import de.garbageview.core.BoundingBox
import de.garbageview.core.OverpassException
import de.garbageview.core.WasteCategory
import de.garbageview.core.WastePoi
import de.garbageview.data.PoiRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

/** Was die Suche gerade macht; wird in der Statusleiste angezeigt. */
sealed interface SearchStatus {
    /** Karte zu weit herausgezoomt, es wird nicht gesucht. */
    data object ZoomTooLow : SearchStatus
    data object Searching : SearchStatus
    data object Done : SearchStatus
    data class Failed(val message: String) : SearchStatus
}

data class MapUiState(
    val enabled: Set<WasteCategory> = WasteCategory.entries.toSet(),
    /** Geladene Standorte im sichtbaren Bereich, bereits nach [enabled] gefiltert. */
    val pois: List<WastePoi> = emptyList(),
    /** Anzahl je Kategorie im sichtbaren Bereich, unabhängig vom Filter. */
    val counts: Map<WasteCategory, Int> = emptyMap(),
    val status: SearchStatus = SearchStatus.ZoomTooLow,
    val selected: WastePoi? = null,
)

class MapViewModel(private val repository: PoiRepository) : ViewModel() {

    private val _state = MutableStateFlow(MapUiState())
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    /** Ungefilterte Standorte des aktuellen Ausschnitts, nach Schlüssel dedupliziert. */
    private var visible: Map<String, WastePoi> = emptyMap()
    private var loadJob: Job? = null
    private var lastViewport: Pair<BoundingBox, Double>? = null

    fun onViewportChanged(bbox: BoundingBox, zoom: Double) {
        lastViewport = bbox to zoom
        loadJob?.cancel()
        val grid = repository.grid
        val tiles = grid.tilesFor(bbox)
        if (zoom < MIN_ZOOM || tiles.size > MAX_TILES) {
            publish(emptyList(), SearchStatus.ZoomTooLow)
            return
        }
        val ordered = grid.sortedByDistance(tiles, (bbox.south + bbox.north) / 2, (bbox.west + bbox.east) / 2)
        val cached = ordered.mapNotNull { repository.cached(it) }
        if (cached.size == ordered.size) {
            publish(cached.flatten(), SearchStatus.Done)
            return
        }
        // Bereits bekannte Kacheln sofort zeigen, den Rest suchen.
        publish(cached.flatten(), SearchStatus.Searching)
        loadJob = viewModelScope.launch {
            try {
                publish(repository.load(ordered), SearchStatus.Done)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("GarbageView", "Suche fehlgeschlagen", e)
                val message = when (e) {
                    is OverpassException -> "Der Datenserver ist gerade überlastet."
                    is IOException -> e.message ?: "Keine Verbindung zum Datenserver."
                    else -> e.message ?: e.javaClass.simpleName
                }
                _state.update { it.copy(status = SearchStatus.Failed(message)) }
            }
        }
    }

    fun retry() {
        lastViewport?.let { (bbox, zoom) -> onViewportChanged(bbox, zoom) }
    }

    fun toggle(category: WasteCategory) {
        _state.update {
            val enabled = if (category in it.enabled) it.enabled - category else it.enabled + category
            it.copy(enabled = enabled, pois = filter(visible, enabled))
        }
    }

    fun select(key: String?) {
        _state.update { s -> s.copy(selected = key?.let { visible[it] }) }
    }

    private fun publish(pois: List<WastePoi>, status: SearchStatus) {
        visible = pois.associateBy { it.key }
        val counts = WasteCategory.entries.associateWith { c -> visible.values.count { c in it.categories } }
        _state.update {
            it.copy(pois = filter(visible, it.enabled), counts = counts, status = status)
        }
    }

    private fun filter(pois: Map<String, WastePoi>, enabled: Set<WasteCategory>) =
        pois.values.filter { poi -> poi.categories.any { it in enabled } }

    companion object {
        /** Unterhalb dieser Zoomstufe wird nicht gesucht (Last auf Overpass begrenzen). */
        const val MIN_ZOOM = 13.0
        const val MAX_TILES = 9

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as GarbageViewApp
                MapViewModel(app.repository)
            }
        }
    }
}
