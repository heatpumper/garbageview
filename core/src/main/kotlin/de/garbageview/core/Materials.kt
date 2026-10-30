package de.garbageview.core

/** Deutsche Bezeichnungen für `recycling:*`-Schlüssel und Glasfarben. */
object Materials {
    private val labels = linkedMapOf(
        "glass_bottles" to "Glasflaschen",
        "glass" to "Glas",
        "paper" to "Papier",
        "cardboard" to "Pappe",
        "paper_packaging" to "Papierverpackungen",
        "plastic_packaging" to "Leichtverpackungen",
        "plastic_bottles" to "Plastikflaschen",
        "plastic" to "Kunststoff",
        "cans" to "Dosen",
        "beverage_cartons" to "Getränkekartons",
        "clothes" to "Kleidung",
        "shoes" to "Schuhe",
        "batteries" to "Batterien",
        "car_batteries" to "Autobatterien",
        "electrical_appliances" to "Elektrogeräte",
        "small_appliances" to "Elektrokleingeräte",
        "mobile_phones" to "Handys",
        "low_energy_bulbs" to "Energiesparlampen",
        "fluorescent_tubes" to "Leuchtstoffröhren",
        "cooking_oil" to "Speiseöl",
        "engine_oil" to "Motoröl",
        "green_waste" to "Grünschnitt",
        "organic" to "Bioabfall",
        "wood" to "Holz",
        "scrap_metal" to "Schrott",
        "hazardous_waste" to "Sondermüll",
        "paint" to "Farben",
        "tyres" to "Reifen",
        "rubble" to "Bauschutt",
        "furniture" to "Sperrmüll",
        "polystyrene_foam" to "Styropor",
        "corks" to "Korken",
        "books" to "Bücher",
        "printer_cartridges" to "Druckerpatronen",
        "waste" to "Restmüll",
    )

    private val glassColours = mapOf(
        "white" to "Weißglas",
        "clear" to "Weißglas",
        "green" to "Grünglas",
        "brown" to "Braunglas",
        "mixed" to "Buntglas",
    )

    fun accepted(tags: Map<String, String>): List<String> {
        val result = mutableListOf<String>()
        for ((key, label) in labels) {
            if (tags["recycling:$key"] == "yes") result += label
        }
        // Unbekannte, aber gesetzte Materialien nicht verschlucken.
        tags.filter { (k, v) ->
            k.startsWith("recycling:") && v == "yes" && k.count { it == ':' } == 1 &&
                k.removePrefix("recycling:") !in labels
        }.keys.sorted().forEach { result += it.removePrefix("recycling:").replace('_', ' ') }

        val colours = tags["recycling:glass_bottles:colour"] ?: tags["recycling:glass:colour"]
        colours?.split(';')?.map { it.trim() }?.mapNotNull { glassColours[it] }?.distinct()
            ?.takeIf { it.isNotEmpty() }
            ?.let { result += "Farben: ${it.joinToString(", ")}" }
        return result
    }
}
