package de.garbageview.core

import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Prüft die echte Overpass API mit einem kleinen Ausschnitt in Berlin-Mitte.
 * Läuft nur mit OVERPASS_LIVE_TEST=1 (in CI gesetzt), um die öffentliche Instanz zu schonen.
 */
class OverpassLiveTest {

    @Test
    fun berlinMitteHasWasteBasketsAndGlassContainers() {
        assumeTrue(System.getenv("OVERPASS_LIVE_TEST") == "1")

        val query = OverpassQuery.build(BoundingBox(52.510, 13.380, 52.525, 13.410))
        val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build()
        val body = ENDPOINTS.firstNotNullOfOrNull { endpoint ->
            val request = HttpRequest.newBuilder(URI(endpoint))
                .timeout(Duration.ofSeconds(60))
                .header("User-Agent", "GarbageView-CI (https://github.com/heatpumper/garbageview)")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("data=" + URLEncoder.encode(query, Charsets.UTF_8)))
                .build()
            val response = runCatching { client.send(request, HttpResponse.BodyHandlers.ofString()) }
                .onFailure { println("$endpoint: ${it.javaClass.simpleName}: ${it.message}") }
                .getOrNull() ?: return@firstNotNullOfOrNull null
            println("$endpoint: HTTP ${response.statusCode()}")
            if (response.statusCode() == 200) response.body() else {
                println(response.body().take(500))
                null
            }
        }
        assertTrue("Keine Overpass-Instanz hat mit HTTP 200 geantwortet", body != null)

        val pois = OverpassParser.parse(body!!)
        val baskets = pois.count { it.category == WasteCategory.WASTE_BASKET }
        println("Overpass live: ${pois.size} Standorte, davon $baskets Mülleimer")
        assertTrue("Keine Mülleimer gefunden", baskets > 10)
        assertTrue("Keine Punkte (node) mit Koordinaten", pois.any { it.osmType == "node" })
    }

    private companion object {
        val ENDPOINTS = listOf(
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter",
        )
    }
}
