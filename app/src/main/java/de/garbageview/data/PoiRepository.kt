package de.garbageview.data

import android.util.Log
import de.garbageview.core.CacheFormat
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
 *
 * Fehlende Kacheln eines Ausschnitts werden mit **einer** Overpass-Abfrage über ihr
 * umschließendes Rechteck geladen und danach auf die Kacheln verteilt. Ein [Mutex] sorgt dafür,
 * dass höchstens eine Abfrage gleichzeitig läuft.
 */
class PoiRepository(
    private val client: OverpassClient,
    private val cacheDir: File,
    val grid: TileGrid = TileGrid(),
    private val maxAgeMs: Long = 7L * 24 * 60 * 60 * 1000,
) {
    private val memory = ConcurrentHashMap<TileGrid.Tile, List<WastePoi>>()
    private val networkLock = Mutex()

    fun cached(tile: TileGrid.Tile): List<WastePoi>? = memory[tile]

    /** Lädt alle [tiles] (aus Cache oder Netz) und liefert ihre Standorte. */
    suspend fun load(tiles: Collection<TileGrid.Tile>): List<WastePoi> {
        for (tile in tiles) {
            if (tile !in memory) readDisk(tile)?.let { memory[tile] = it }
        }
        val missing = tiles.filter { it !in memory }
        if (missing.isNotEmpty()) {
            networkLock.withLock {
                val stillMissing = missing.filter { it !in memory }
                if (stillMissing.isNotEmpty()) fetch(stillMissing)
            }
        }
        return tiles.flatMap { memory[it].orEmpty() }
    }

    private suspend fun fetch(missing: List<TileGrid.Tile>) {
        val bounds = grid.boundsOf(missing)
        val covered = grid.tilesFor(bounds).filter {
            // tilesFor schließt Kacheln am Rand mit ein; nur echte Innenkacheln zählen.
            val b = grid.bounds(it)
            b.south >= bounds.south - EPS && b.north <= bounds.north + EPS &&
                b.west >= bounds.west - EPS && b.east <= bounds.east + EPS
        }
        val started = System.currentTimeMillis()
        Log.i(TAG, "Overpass-Abfrage für ${covered.size} Kacheln: $bounds")
        val body = client.query(OverpassQuery.build(bounds))
        val pois = withContext(Dispatchers.Default) { OverpassParser.parse(body) }
        Log.i(TAG, "${pois.size} Standorte in ${System.currentTimeMillis() - started} ms geladen")

        val byTile = pois.groupBy { grid.tileOf(it.lat, it.lon) }
        for (tile in covered) {
            val list = byTile[tile].orEmpty()
            memory[tile] = list
            writeDisk(tile, list)
        }
    }

    private suspend fun readDisk(tile: TileGrid.Tile): List<WastePoi>? = withContext(Dispatchers.IO) {
        val file = fileFor(tile)
        if (!file.exists() || System.currentTimeMillis() - file.lastModified() > maxAgeMs) {
            return@withContext null
        }
        runCatching { CacheFormat.decode(file.readText()) }.getOrNull()
    }

    private suspend fun writeDisk(tile: TileGrid.Tile, pois: List<WastePoi>) = withContext(Dispatchers.IO) {
        runCatching {
            cacheDir.mkdirs()
            val tmp = File(cacheDir, "${tile.key}.tmp")
            tmp.writeText(CacheFormat.encode(pois))
            tmp.renameTo(fileFor(tile))
        }
    }

    private fun fileFor(tile: TileGrid.Tile) = File(cacheDir, "${tile.key}.json")

    private companion object {
        const val TAG = "GarbageView"
        const val EPS = 1e-9
    }
}
