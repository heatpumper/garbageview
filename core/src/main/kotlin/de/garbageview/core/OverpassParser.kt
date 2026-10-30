package de.garbageview.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** Overpass hat die Abfrage abgebrochen (z. B. Zeitüberschreitung oder Überlastung). */
class OverpassException(message: String) : Exception(message)

/**
 * Liest JSON im Format der Overpass API. Dieses Format verwenden auch die Regionsdateien
 * (scripts/build_data.py) und der Cache ([CacheFormat]).
 */
object OverpassParser {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * @throws OverpassException wenn Overpass einen Laufzeitfehler meldet. Solche Antworten
     *   enthalten keine oder nur einen Teil der Daten und dürfen nicht gecacht werden.
     */
    fun parse(body: String): List<WastePoi> {
        val root = json.parseToJsonElement(body).jsonObject
        val remark = root["remark"]?.jsonPrimitive?.contentOrNull
        if (remark != null && remark.contains("error", ignoreCase = true)) {
            throw OverpassException(remark)
        }
        val elements = root["elements"]?.jsonArray ?: return emptyList()
        return elements.mapNotNull { parseElement(it.jsonObject) }
    }

    private fun parseElement(element: JsonObject): WastePoi? {
        val type = element["type"]?.jsonPrimitive?.contentOrNull ?: return null
        val id = element["id"]?.jsonPrimitive?.longOrNull ?: return null
        val center = element["center"]?.jsonObject
        val lat = (element["lat"] ?: center?.get("lat"))?.jsonPrimitive?.doubleOrNull ?: return null
        val lon = (element["lon"] ?: center?.get("lon"))?.jsonPrimitive?.doubleOrNull ?: return null
        val tags = element["tags"]?.jsonObject
            ?.mapNotNull { (k, v) -> v.jsonPrimitive.contentOrNull?.let { k to it } }
            ?.toMap()
            ?: emptyMap()

        val categories = WasteClassifier.classify(tags)
        val primary = WasteClassifier.primary(categories) ?: return null
        return WastePoi(type, id, lat, lon, primary, categories, tags)
    }
}
