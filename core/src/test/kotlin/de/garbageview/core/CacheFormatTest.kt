package de.garbageview.core

import org.junit.Assert.assertEquals
import org.junit.Test

class CacheFormatTest {

    @Test
    fun roundTrip() {
        val pois = listOf(
            WastePoi("node", 1, 52.5, 13.4, WasteCategory.WASTE_BASKET, setOf(WasteCategory.WASTE_BASKET),
                mapOf("amenity" to "waste_basket")),
            WastePoi("way", 2, 52.6, 13.5, WasteCategory.RECYCLING_CENTRE,
                setOf(WasteCategory.RECYCLING_CENTRE, WasteCategory.GLASS),
                mapOf("amenity" to "recycling", "recycling_type" to "centre", "recycling:glass" to "yes",
                    "name" to "Hof \"Süd\"")),
        )
        assertEquals(pois, CacheFormat.decode(CacheFormat.encode(pois)))
    }

    @Test
    fun emptyTile() {
        assertEquals(emptyList<WastePoi>(), CacheFormat.decode(CacheFormat.encode(emptyList())))
    }
}
