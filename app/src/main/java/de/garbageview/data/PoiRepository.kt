package de.garbageview.data

import android.util.Log
import de.garbageview.core.CacheFormat
import de.garbageview.core.TileGrid
import de.garbageview.core.WastePoi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.GZIPInputStream

/**
 * Lädt Standorte regionsweise (siehe [TileGrid.REGION_DEGREES]) und hält sie im Arbeitsspeicher
 * und als gepackte Datei im Cache-Verzeichnis.
 */
class PoiRepository(
    private val client: RegionClient,
    private val cacheDir: File,
    val grid: TileGrid = TileGrid(),
    private val maxAgeMs: Long = 3L * 24 * 60 * 60 * 1000,
) {
    /** Zuletzt benutzte Regionen; begrenzt, damit große Städte den Speicher nicht füllen. */
    private val memory = object : LinkedHashMap<TileGrid.Tile, List<WastePoi>>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<TileGrid.Tile, List<WastePoi>>) =
            size > MAX_REGIONS_IN_MEMORY
    }

    @Synchronized
    fun cached(region: TileGrid.Tile): List<WastePoi>? = memory[region]

    suspend fun load(region: TileGrid.Tile): List<WastePoi> {
        cached(region)?.let { return it }
        val started = System.currentTimeMillis()
        val file = File(cacheDir, "region_${region.x}_${region.y}.json.gz")
        val fresh = withContext(Dispatchers.IO) {
            file.exists() && System.currentTimeMillis() - file.lastModified() < maxAgeMs
        }
        val bytes = if (fresh) {
            withContext(Dispatchers.IO) { file.readBytes() }
        } else {
            client.fetch(region)?.also { writeCache(file, it) }
        }
        val pois = if (bytes == null) {
            emptyList()
        } else {
            withContext(Dispatchers.Default) {
                CacheFormat.decode(GZIPInputStream(bytes.inputStream()).bufferedReader().readText())
            }
        }
        Log.i(TAG, "Region ${region.key}: ${pois.size} Standorte in ${System.currentTimeMillis() - started} ms" +
            if (fresh) " (Cache)" else "")
        synchronized(this) { memory[region] = pois }
        return pois
    }

    private suspend fun writeCache(file: File, bytes: ByteArray) = withContext(Dispatchers.IO) {
        runCatching {
            cacheDir.mkdirs()
            val tmp = File(cacheDir, "${file.name}.tmp")
            tmp.writeBytes(bytes)
            tmp.renameTo(file)
        }
    }

    private companion object {
        const val TAG = "GarbageView"
        const val MAX_REGIONS_IN_MEMORY = 6
    }
}
