package de.garbageview.core

import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Speichert Standorte einer Kachel im Overpass-JSON-Format (nur Typ, ID, Koordinaten, Tags),
 * damit [OverpassParser] den Cache wieder einlesen kann.
 */
object CacheFormat {

    fun encode(pois: Collection<WastePoi>): String = buildJsonObject {
        put("elements", buildJsonArray {
            for (poi in pois) {
                add(buildJsonObject {
                    put("type", poi.osmType)
                    put("id", poi.osmId)
                    put("lat", poi.lat)
                    put("lon", poi.lon)
                    put("tags", buildJsonObject { poi.tags.forEach { (k, v) -> put(k, v) } })
                })
            }
        })
    }.toString()

    fun decode(body: String): List<WastePoi> = OverpassParser.parse(body)
}
