package de.garbageview.data

import de.garbageview.BuildConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Minimaler Overpass-Client.
 *
 * Die öffentlichen Instanzen sind nicht für Produktiv-Apps gedacht (siehe docs/DATENQUELLEN.md).
 * Für den Prototyp halten wir uns an die Regeln: eindeutiger User-Agent, eine Abfrage zur Zeit
 * (siehe [PoiRepository]), Pause bei HTTP 429 und Ausweichen auf eine zweite Instanz.
 * Abfragen werden abgebrochen, sobald die aufrufende Coroutine abgebrochen wird.
 */
class OverpassClient(
    private val endpoints: List<String> = DEFAULT_ENDPOINTS,
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(75, TimeUnit.SECONDS)
        .build(),
) {

    suspend fun query(ql: String): String {
        var lastError: IOException? = null
        for ((index, endpoint) in endpoints.withIndex()) {
            val request = Request.Builder()
                .url(endpoint)
                .header("User-Agent", USER_AGENT)
                .post(FormBody.Builder().add("data", ql).build())
                .build()
            try {
                http.newCall(request).await().use { response ->
                    val body = response.body?.string()
                    when {
                        response.isSuccessful && body != null -> return body
                        response.code == 429 || response.code == 504 -> {
                            lastError = IOException("Server ausgelastet (HTTP ${response.code})")
                            if (index < endpoints.lastIndex) delay(RETRY_DELAY_MS)
                        }
                        else -> lastError = IOException("Serverfehler HTTP ${response.code}")
                    }
                }
            } catch (e: IOException) {
                lastError = e
            }
        }
        throw lastError ?: IOException("Kein Server erreichbar")
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
        val DEFAULT_ENDPOINTS = listOf(
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter",
        )
        private const val RETRY_DELAY_MS = 2_000L
        private val USER_AGENT =
            "GarbageView/${BuildConfig.VERSION_NAME} (Android; https://github.com/heatpumper/garbageview)"
    }
}
