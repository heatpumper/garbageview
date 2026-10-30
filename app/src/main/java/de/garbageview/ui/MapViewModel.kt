package de.garbageview.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import de.garbageview.GarbageViewApp
import de.garbageview.core.BoundingBox
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

data class MapUiState(
    val enabled: Set<WasteCategory> = WasteCategory.entries.toSet(),
    /** Alle geladenen Standorte im sichtbaren Bereich, bereits nach [enabled] gefiltert. */
    val pois: List<WastePoi> = emptyList(),
    val loading: Boolean = false,
    val zoomTooLow: Boolean = true,
    val error: String? = null,
    val selected: WastePoi? = null,
)

class MapViewModel(private val repository: PoiRepository) : ViewModel() {

    private val _state = MutableStateFlow(MapUiState())
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    /** Ungefilterte Standorte des aktuellen Ausschnitts, nach Schlüssel dedupliziert. */
    private var visible: Map<String, WastePoi> = emptyMap()
    private var loadJob: Job? = null

    fun onViewportChanged(bbox: BoundingBox, zoom: Double) {
        loadJob?.cancel()
        val grid = repository.grid
        val tiles = grid.tilesFor(bbox)
        if (zoom < MIN_ZOOM || tiles.size > MAX_TILES) {
            _state.update { it.copy(zoomTooLow = true, loading = false, error = null) }
            return
        }
        val center = (bbox.south + bbox.north) / 2 to (bbox.west + bbox.east) / 2
        val ordered = grid.sortedByDistance(tiles, center.first, center.second)

        // Bereits geladene Kacheln sofort anzeigen.
        val collected = LinkedHashMap<String, WastePoi>()
        ordered.mapNotNull { repository.cached(it) }.forEach { list -> list.forEach { collected[it.key] = it } }
        publish(collected, loading = ordered.any { repository.cached(it) == null })

        loadJob = viewModelScope.launch {
            var error: String? = null
            for (tile in ordered) {
                if (repository.cached(tile) != null) continue
                try {
                    repository.load(tile).forEach { collected[it.key] = it }
                    publish(collected, loading = true)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    error = "Daten konnten nicht geladen werden: ${e.message ?: e.javaClass.simpleName}"
                }
            }
            publish(collected, loading = false, error = error)
        }
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

    fun dismissError() = _state.update { it.copy(error = null) }

    private fun publish(collected: Map<String, WastePoi>, loading: Boolean, error: String? = null) {
        visible = collected.toMap()
        _state.update {
            it.copy(
                pois = filter(visible, it.enabled),
                loading = loading,
                zoomTooLow = false,
                error = error,
            )
        }
    }

    private fun filter(pois: Map<String, WastePoi>, enabled: Set<WasteCategory>) =
        pois.values.filter { poi -> poi.categories.any { it in enabled } }

    companion object {
        /** Unterhalb dieser Zoomstufe werden keine Daten geladen (Last auf Overpass begrenzen). */
        const val MIN_ZOOM = 13.0
        const val MAX_TILES = 12

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as GarbageViewApp
                MapViewModel(app.repository)
            }
        }
    }
}
