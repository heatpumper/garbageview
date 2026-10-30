package de.garbageview.core

/**
 * Kategorien, die auf der Karte angezeigt werden. [colorHex] wird für die Kartenmarker verwendet.
 *
 * Welche OSM-Tags zu welcher Kategorie gehören, legt [WasteClassifier] fest; welche Objekte
 * überhaupt exportiert werden, scripts/build_data.py.
 */
enum class WasteCategory(
    val label: String,
    val colorHex: String,
) {
    WASTE_BASKET(
        label = "Mülleimer",
        colorHex = "#607D8B",
    ),
    DOG_WASTE(
        label = "Hundekot",
        colorHex = "#8D6E63",
    ),
    GLASS(
        label = "Glas",
        colorHex = "#2E7D32",
    ),
    PAPER(
        label = "Papier",
        colorHex = "#1565C0",
    ),
    CLOTHES(
        label = "Altkleider",
        colorHex = "#AD1457",
    ),
    RECYCLING_CENTRE(
        label = "Wertstoffhof",
        colorHex = "#EF6C00",
    ),
    OTHER_RECYCLING(
        label = "Sonstige Wertstoffe",
        colorHex = "#00838F",
    ),
    WASTE_DISPOSAL(
        label = "Restmüll-Sammelstelle",
        colorHex = "#424242",
    ),
}
