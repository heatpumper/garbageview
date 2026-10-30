package de.garbageview.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** Liest die JSON-Antwort der Overpass API. */
object OverpassParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(body: String): List<WastePoi> {
        val root = json.parseToJsonElement(body).jsonObject
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
