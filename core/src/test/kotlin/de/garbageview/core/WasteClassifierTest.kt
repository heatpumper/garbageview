package de.garbageview.core

import org.junit.Assert.assertEquals
import org.junit.Test

class WasteClassifierTest {

    private fun classify(vararg tags: Pair<String, String>) = WasteClassifier.classify(tags.toMap())

    @Test
    fun wasteBasket() {
        assertEquals(setOf(WasteCategory.WASTE_BASKET), classify("amenity" to "waste_basket"))
    }

    @Test
    fun dogWasteBasket() {
        assertEquals(
            setOf(WasteCategory.DOG_WASTE),
            classify("amenity" to "waste_basket", "waste" to "dog_excrement"),
        )
        assertEquals(
            setOf(WasteCategory.WASTE_BASKET, WasteCategory.DOG_WASTE),
            classify("amenity" to "waste_basket", "vending" to "excrement_bags"),
        )
    }

    @Test
    fun glassViaLegacyTag() {
        assertEquals(setOf(WasteCategory.GLASS), classify("amenity" to "recycling", "recycling:glass" to "yes"))
    }

    @Test
    fun multiMaterialContainer() {
        val categories = classify(
            "amenity" to "recycling",
            "recycling:glass_bottles" to "yes",
            "recycling:paper" to "yes",
            "recycling:clothes" to "yes",
        )
        assertEquals(setOf(WasteCategory.GLASS, WasteCategory.PAPER, WasteCategory.CLOTHES), categories)
        assertEquals(WasteCategory.GLASS, WasteClassifier.primary(categories))
    }

    @Test
    fun recyclingWithoutKnownMaterialIsOther() {
        assertEquals(
            setOf(WasteCategory.OTHER_RECYCLING),
            classify("amenity" to "recycling", "recycling:batteries" to "yes"),
        )
    }

    @Test
    fun centreIsPrimary() {
        val categories = classify("amenity" to "recycling", "recycling_type" to "centre", "recycling:glass" to "yes")
        assertEquals(WasteCategory.RECYCLING_CENTRE, WasteClassifier.primary(categories))
    }

    @Test
    fun unrelated() {
        assertEquals(emptySet<WasteCategory>(), classify("amenity" to "bench"))
        assertEquals(null, WasteClassifier.primary(emptySet()))
    }
}
