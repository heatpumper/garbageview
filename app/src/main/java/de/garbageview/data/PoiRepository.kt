package de.garbageview.data

import de.garbageview.core.OverpassParser
import de.garbageview.core.OverpassQuery
import de.garbageview.core.TileGrid
import de.garbageview.core.WastePoi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Lädt Standorte kachelweise und speichert sie im Arbeitsspeicher und im Cache-Verzeichnis.
 * Ein [Mutex] sorgt dafür, dass höchstens eine Overpass-Abfrage gleichzeitig läuft.
 */
class PoiRepository(
    private val client: OverpassClient,
    private val cacheDir: File,
    val grid: TileGrid = TileGrid(),
    private val maxAgeMs: Long = 7L * 24 * 60 * 60 * 1000,
) {
    private val memory = ConcurrentHashMap<String, List<WastePoi>>()
    private val networkLock = Mutex()

    fun cached(tile: TileGrid.Tile): List<WastePoi>? = memory[tile.key]

    suspend fun load(tile: TileGrid.Tile): List<WastePoi> {
        memory[tile.key]?.let { return it }
        readDisk(tile)?.let { pois ->
            memory[tile.key] = pois
            return pois
        }
        return networkLock.withLock {
            // Während des Wartens kann eine andere Abfrage die Kachel bereits geladen haben.
            memory[tile.key]?.let { return@withLock it }
            val body = client.query(OverpassQuery.build(grid.bounds(tile)))
            val pois = withContext(Dispatchers.Default) { OverpassParser.parse(body) }
            writeDisk(tile, body)
            memory[tile.key] = pois
            pois
        }
    }

    private suspend fun readDisk(tile: TileGrid.Tile): List<WastePoi>? = withContext(Dispatchers.IO) {
        val file = fileFor(tile)
        if (!file.exists() || System.currentTimeMillis() - file.lastModified() > maxAgeMs) {
            return@withContext null
        }
        runCatching { OverpassParser.parse(file.readText()) }.getOrNull()
    }

    private suspend fun writeDisk(tile: TileGrid.Tile, body: String) = withContext(Dispatchers.IO) {
        runCatching {
            cacheDir.mkdirs()
            val tmp = File(cacheDir, "${tile.key}.tmp")
            tmp.writeText(body)
            tmp.renameTo(fileFor(tile))
        }
    }

    private fun fileFor(tile: TileGrid.Tile) = File(cacheDir, "${tile.key}.json")
}
