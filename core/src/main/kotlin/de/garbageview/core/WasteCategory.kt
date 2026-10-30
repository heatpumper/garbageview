package de.garbageview.core

/**
 * Kategorien, die auf der Karte angezeigt werden.
 *
 * [overpassSelectors] sind Overpass-QL-Tagfilter (ohne Elementtyp und Bounding-Box).
 * [colorHex] wird für die Kartenmarker verwendet.
 */
enum class WasteCategory(
    val label: String,
    val colorHex: String,
    val overpassSelectors: List<String>,
) {
    WASTE_BASKET(
        label = "Mülleimer",
        colorHex = "#607D8B",
        overpassSelectors = listOf("""["amenity"="waste_basket"]"""),
    ),
    DOG_WASTE(
        label = "Hundekot",
        colorHex = "#8D6E63",
        overpassSelectors = listOf("""["vending"="excrement_bags"]"""),
    ),
    GLASS(
        label = "Glas",
        colorHex = "#2E7D32",
        overpassSelectors = listOf(
            """["amenity"="recycling"]["recycling:glass_bottles"="yes"]""",
            """["amenity"="recycling"]["recycling:glass"="yes"]""",
        ),
    ),
    PAPER(
        label = "Papier",
        colorHex = "#1565C0",
        overpassSelectors = listOf(
            """["amenity"="recycling"]["recycling:paper"="yes"]""",
            """["amenity"="recycling"]["recycling:cardboard"="yes"]""",
        ),
    ),
    CLOTHES(
        label = "Altkleider",
        colorHex = "#AD1457",
        overpassSelectors = listOf(
            """["amenity"="recycling"]["recycling:clothes"="yes"]""",
            """["amenity"="recycling"]["recycling:shoes"="yes"]""",
        ),
    ),
    RECYCLING_CENTRE(
        label = "Wertstoffhof",
        colorHex = "#EF6C00",
        overpassSelectors = listOf("""["amenity"="recycling"]["recycling_type"="centre"]"""),
    ),
    OTHER_RECYCLING(
        label = "Sonstige Wertstoffe",
        colorHex = "#00838F",
        overpassSelectors = listOf("""["amenity"="recycling"]"""),
    ),
    WASTE_DISPOSAL(
        label = "Restmüll-Sammelstelle",
        colorHex = "#424242",
        overpassSelectors = listOf("""["amenity"="waste_disposal"]"""),
    ),
}
