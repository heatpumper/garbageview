package de.garbageview.core

/** Ordnet OSM-Tags den App-Kategorien zu. */
object WasteClassifier {

    fun classify(tags: Map<String, String>): Set<WasteCategory> {
        val result = linkedSetOf<WasteCategory>()
        val amenity = tags["amenity"]
        val isRecycling = amenity == "recycling"

        if (isRecycling && tags["recycling_type"] == "centre") result += WasteCategory.RECYCLING_CENTRE
        if (isRecycling && (yes(tags, "recycling:glass_bottles") || yes(tags, "recycling:glass"))) {
            result += WasteCategory.GLASS
        }
        if (isRecycling && (yes(tags, "recycling:paper") || yes(tags, "recycling:cardboard"))) {
            result += WasteCategory.PAPER
        }
        if (isRecycling && (yes(tags, "recycling:clothes") || yes(tags, "recycling:shoes"))) {
            result += WasteCategory.CLOTHES
        }
        if (isRecycling && result.isEmpty()) result += WasteCategory.OTHER_RECYCLING

        if (amenity == "waste_basket") {
            val waste = tags["waste"].orEmpty().split(';').map { it.trim() }
            if (waste.singleOrNull() == "dog_excrement") result += WasteCategory.DOG_WASTE
            else result += WasteCategory.WASTE_BASKET
        }
        if (tags["vending"] == "excrement_bags") result += WasteCategory.DOG_WASTE
        if (amenity == "waste_disposal") result += WasteCategory.WASTE_DISPOSAL

        return result
    }

    /**
     * Hauptkategorie für Farbe und Symbol. Ein Wertstoffhof bleibt ein Wertstoffhof,
     * auch wenn er Glas annimmt; ansonsten gilt die Reihenfolge der Enum-Konstanten.
     */
    fun primary(categories: Set<WasteCategory>): WasteCategory? =
        if (WasteCategory.RECYCLING_CENTRE in categories) WasteCategory.RECYCLING_CENTRE
        else WasteCategory.entries.firstOrNull { it in categories }

    private fun yes(tags: Map<String, String>, key: String) = tags[key] == "yes"
}
