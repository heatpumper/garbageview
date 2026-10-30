package de.garbageview.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class GeoJsonTest {

    @Test
    fun encodesPointsWithLonLatOrder() {
        val poi = WastePoi(
            osmType = "node", osmId = 7, lat = 50.1, lon = 8.6,
            category = WasteCategory.GLASS, categories = setOf(WasteCategory.GLASS),
            tags = mapOf("name" to "Glas \"Ecke\""),
        )
        val root = Json.parseToJsonElement(GeoJson.featureCollection(listOf(poi))).jsonObject
        val feature = root["features"]!!.jsonArray.single().jsonObject
        val coords = feature["geometry"]!!.jsonObject["coordinates"]!!.jsonArray
        assertEquals(8.6, coords[0].jsonPrimitive.double, 1e-9)
        assertEquals(50.1, coords[1].jsonPrimitive.double, 1e-9)
        val props = feature["properties"]!!.jsonObject
        assertEquals("node/7", props[GeoJson.PROP_KEY]!!.jsonPrimitive.content)
        assertEquals("GLASS", props[GeoJson.PROP_CATEGORY]!!.jsonPrimitive.content)
        assertEquals("Glas \"Ecke\"", props[GeoJson.PROP_NAME]!!.jsonPrimitive.content)
    }
}
