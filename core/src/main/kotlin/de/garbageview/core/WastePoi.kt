package de.garbageview.core

/** Ein Standort aus OpenStreetMap. */
data class WastePoi(
    /** Elementtyp ("node", "way", "relation"). */
    val osmType: String,
    val osmId: Long,
    val lat: Double,
    val lon: Double,
    /** Hauptkategorie für Darstellung und Filter. */
    val category: WasteCategory,
    /** Alle zutreffenden Kategorien (ein Container kann z. B. Glas und Papier annehmen). */
    val categories: Set<WasteCategory>,
    val tags: Map<String, String>,
) {
    val key: String get() = "$osmType/$osmId"

    val name: String? get() = tags["name"]
    val operator: String? get() = tags["operator"]
    val openingHours: String? get() = tags["opening_hours"]
    val description: String? get() = tags["description"] ?: tags["note"]

    /** Lesbare Liste der angenommenen Materialien. */
    val acceptedMaterials: List<String>
        get() = Materials.accepted(tags)

    val osmUrl: String get() = "https://www.openstreetmap.org/$osmType/$osmId"
}
