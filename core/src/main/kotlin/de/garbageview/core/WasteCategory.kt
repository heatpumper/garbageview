package de.garbageview.core

/**
 * Kategorien, die auf der Karte angezeigt werden.
 *
 * [overpassSelectors] sind Overpass-QL-Filter mit Elementtyp, aber ohne Bounding-Box.
 * Mülleimer und Beutelspender sind praktisch immer Punkte (`node`); das macht die Abfrage schneller.
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
        overpassSelectors = listOf("""node["amenity"="waste_basket"]"""),
    ),
    DOG_WASTE(
        label = "Hundekot",
        colorHex = "#8D6E63",
        overpassSelectors = listOf("""node["vending"="excrement_bags"]"""),
    ),
    GLASS(
        label = "Glas",
        colorHex = "#2E7D32",
        overpassSelectors = listOf(
            """nwr["amenity"="recycling"]["recycling:glass_bottles"="yes"]""",
            """nwr["amenity"="recycling"]["recycling:glass"="yes"]""",
        ),
    ),
    PAPER(
        label = "Papier",
        colorHex = "#1565C0",
        overpassSelectors = listOf(
            """nwr["amenity"="recycling"]["recycling:paper"="yes"]""",
            """nwr["amenity"="recycling"]["recycling:cardboard"="yes"]""",
        ),
    ),
    CLOTHES(
        label = "Altkleider",
        colorHex = "#AD1457",
        overpassSelectors = listOf(
            """nwr["amenity"="recycling"]["recycling:clothes"="yes"]""",
            """nwr["amenity"="recycling"]["recycling:shoes"="yes"]""",
        ),
    ),
    RECYCLING_CENTRE(
        label = "Wertstoffhof",
        colorHex = "#EF6C00",
        overpassSelectors = listOf("""nwr["amenity"="recycling"]["recycling_type"="centre"]"""),
    ),
    OTHER_RECYCLING(
        label = "Sonstige Wertstoffe",
        colorHex = "#00838F",
        overpassSelectors = listOf("""nwr["amenity"="recycling"]"""),
    ),
    WASTE_DISPOSAL(
        label = "Restmüll-Sammelstelle",
        colorHex = "#424242",
        overpassSelectors = listOf("""nwr["amenity"="waste_disposal"]"""),
    ),
}
