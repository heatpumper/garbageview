package de.garbageview.data

import de.garbageview.BuildConfig
import de.garbageview.core.TileGrid
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Lädt gepackte Regionsdateien aus dem GitHub-Release "daten"
 * (erzeugt von .github/workflows/data.yml).
 */
class RegionClient(
    private val baseUrl: String = BASE_URL,
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
) {

    /** Gepackte Datei der Region, oder null, wenn es dort keine Standorte gibt. */
    suspend fun fetch(region: TileGrid.Tile): ByteArray? {
        val request = Request.Builder()
            .url("$baseUrl/region_${region.x}_${region.y}.json.gz")
            .header("User-Agent", USER_AGENT)
            .build()
        http.newCall(request).await().use { response ->
            return when {
                response.isSuccessful -> response.body?.bytes() ?: throw IOException("Leere Antwort")
                response.code == 404 -> null
                else -> throw IOException("Serverfehler HTTP ${response.code}")
            }
        }
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) = cont.resume(response)
            override fun onFailure(call: Call, e: IOException) {
                if (!cont.isCancelled) cont.resumeWithException(e)
            }
        })
    }

    companion object {
        const val BASE_URL = "https://github.com/heatpumper/garbageview/releases/download/daten"
        private val USER_AGENT = "GarbageView/${BuildConfig.VERSION_NAME} (Android)"
    }
}
