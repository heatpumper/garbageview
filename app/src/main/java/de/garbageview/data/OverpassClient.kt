package de.garbageview.data

import de.garbageview.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Minimaler Overpass-Client.
 *
 * Die öffentlichen Instanzen sind nicht für Produktiv-Apps gedacht (siehe docs/DATENQUELLEN.md).
 * Für den Prototyp halten wir uns an die Regeln: eindeutiger User-Agent, eine Abfrage zur Zeit
 * (siehe [PoiRepository]), Pause bei HTTP 429 und Ausweichen auf eine zweite Instanz.
 */
class OverpassClient(
    private val endpoints: List<String> = DEFAULT_ENDPOINTS,
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .build(),
) {

    suspend fun query(ql: String): String = withContext(Dispatchers.IO) {
        var lastError: IOException? = null
        for ((index, endpoint) in endpoints.withIndex()) {
            val request = Request.Builder()
                .url(endpoint)
                .header("User-Agent", USER_AGENT)
                .post(FormBody.Builder().add("data", ql).build())
                .build()
            try {
                http.newCall(request).execute().use { response ->
                    val body = response.body?.string()
                    when {
                        response.isSuccessful && body != null -> return@withContext body
                        response.code == 429 || response.code == 504 -> {
                            lastError = IOException("Overpass ausgelastet (HTTP ${response.code})")
                            if (index < endpoints.lastIndex) delay(RETRY_DELAY_MS)
                        }
                        else -> lastError = IOException("Overpass-Fehler HTTP ${response.code}")
                    }
                }
            } catch (e: IOException) {
                lastError = e
            }
        }
        throw lastError ?: IOException("Keine Overpass-Instanz erreichbar")
    }

    companion object {
        val DEFAULT_ENDPOINTS = listOf(
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter",
        )
        private const val RETRY_DELAY_MS = 2_000L
        private val USER_AGENT =
            "GarbageView/${BuildConfig.VERSION_NAME} (Android; https://github.com/heatpumper/garbageview)"
    }
}
