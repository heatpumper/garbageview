package de.garbageview.core

/** Erzeugt Overpass-QL-Abfragen. */
object OverpassQuery {

    /**
     * Fragt alle Kategorien in [bbox] ab. Die Filterung nach Kategorie passiert in der App,
     * damit ein Umschalten der Filter keine neue Netzwerkabfrage auslöst.
     * `out center` liefert Koordinaten für Punkte und einen Mittelpunkt für Flächen
     * (z. B. Wertstoffhöfe). Nicht `out tags` verwenden: Das lässt die Koordinaten der Punkte weg.
     */
    fun build(
        bbox: BoundingBox,
        categories: Collection<WasteCategory> = WasteCategory.entries,
        timeoutSeconds: Int = 25,
    ): String {
        val box = bbox.toOverpass()
        val selectors = categories.flatMap { it.overpassSelectors }.distinct()
        // `amenity=recycling` deckt alle spezielleren Recycling-Filter bereits ab.
        val reduced = if ("""["amenity"="recycling"]""" in selectors) {
            selectors.filterNot { it.startsWith("""["amenity"="recycling"][""") }
        } else {
            selectors
        }
        val body = reduced.joinToString("\n") { "  nwr$it($box);" }
        return "[out:json][timeout:$timeoutSeconds];\n(\n$body\n);\nout center;"
    }
}
