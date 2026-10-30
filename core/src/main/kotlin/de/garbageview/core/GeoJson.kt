package de.garbageview.core

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Wandelt Standorte in eine GeoJSON-FeatureCollection für MapLibre um. */
object GeoJson {
    const val PROP_KEY = "key"
    const val PROP_CATEGORY = "category"
    const val PROP_NAME = "name"

    fun featureCollection(pois: Collection<WastePoi>): String = buildJsonObject {
        put("type", "FeatureCollection")
        put("features", buildJsonArray {
            for (poi in pois) {
                add(buildJsonObject {
                    put("type", "Feature")
                    put("id", poi.key)
                    put("geometry", buildJsonObject {
                        put("type", "Point")
                        put("coordinates", JsonArray(listOf(JsonPrimitive(poi.lon), JsonPrimitive(poi.lat))))
                    })
                    put("properties", buildJsonObject {
                        put(PROP_KEY, poi.key)
                        put(PROP_CATEGORY, poi.category.name)
                        poi.name?.let { put(PROP_NAME, it) }
                    })
                })
            }
        })
    }.toString()
}
