package de.garbageview.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OverpassParserTest {

    private val sample = """
        {
          "version": 0.6,
          "elements": [
            {"type": "node", "id": 1, "lat": 52.52, "lon": 13.40,
             "tags": {"amenity": "waste_basket"}},
            {"type": "node", "id": 2, "lat": 52.53, "lon": 13.41,
             "tags": {"amenity": "recycling", "recycling_type": "container",
                      "recycling:glass_bottles": "yes",
                      "recycling:glass_bottles:colour": "white;green;brown"}},
            {"type": "way", "id": 3, "center": {"lat": 52.54, "lon": 13.42},
             "tags": {"amenity": "recycling", "recycling_type": "centre", "name": "Wertstoffhof Nord",
                      "recycling:glass_bottles": "yes", "opening_hours": "Mo-Fr 07:00-17:00"}},
            {"type": "node", "id": 4, "lat": 52.55, "lon": 13.43,
             "tags": {"amenity": "bench"}},
            {"type": "way", "id": 5, "tags": {"amenity": "waste_basket"}}
          ]
        }
    """.trimIndent()

    @Test
    fun parsesNodesAndWayCenters() {
        val pois = OverpassParser.parse(sample)
        assertEquals(listOf("node/1", "node/2", "way/3"), pois.map { it.key })

        val centre = pois.single { it.osmId == 3L }
        assertEquals(52.54, centre.lat, 1e-9)
        assertEquals(WasteCategory.RECYCLING_CENTRE, centre.category)
        assertTrue(WasteCategory.GLASS in centre.categories)
        assertEquals("Wertstoffhof Nord", centre.name)
        assertEquals("Mo-Fr 07:00-17:00", centre.openingHours)
        assertEquals("https://www.openstreetmap.org/way/3", centre.osmUrl)
    }

    @Test
    fun glassContainerMaterials() {
        val glass = OverpassParser.parse(sample).single { it.osmId == 2L }
        assertEquals(WasteCategory.GLASS, glass.category)
        assertEquals(
            listOf("Glasflaschen", "Farben: Weißglas, Grünglas, Braunglas"),
            glass.acceptedMaterials,
        )
    }

    @Test(expected = OverpassException::class)
    fun runtimeErrorIsReported() {
        OverpassParser.parse(
            """{"elements": [], "remark": "runtime error: Query timed out in \"query\" at line 3 after 61 seconds."}""",
        )
    }

    @Test
    fun emptyResponse() {
        assertEquals(emptyList<WastePoi>(), OverpassParser.parse("""{"elements": []}"""))
    }
}
