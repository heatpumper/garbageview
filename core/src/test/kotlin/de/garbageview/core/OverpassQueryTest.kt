package de.garbageview.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverpassQueryTest {

    private val bbox = BoundingBox(51.0, 7.0, 51.05, 7.05)

    @Test
    fun allCategoriesCollapseRecyclingSelectors() {
        val query = OverpassQuery.build(bbox)
        assertTrue(query.startsWith("[out:json][timeout:25];"))
        assertTrue(query.contains("""nwr["amenity"="recycling"](51.0,7.0,51.05,7.05);"""))
        assertTrue(query.contains("""nwr["amenity"="waste_basket"](51.0,7.0,51.05,7.05);"""))
        assertFalse(query.contains("recycling:glass"))
        assertTrue(query.endsWith("\nout center;"))
    }

    @Test
    fun singleCategory() {
        val query = OverpassQuery.build(bbox, listOf(WasteCategory.GLASS))
        assertEquals(2, query.lines().count { it.trim().startsWith("nwr") })
        assertTrue(query.contains("""["recycling:glass_bottles"="yes"]"""))
    }
}
