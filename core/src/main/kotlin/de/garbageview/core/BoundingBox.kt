package de.garbageview.core

/** Geografisches Rechteck in WGS84-Grad. */
data class BoundingBox(
    val south: Double,
    val west: Double,
    val north: Double,
    val east: Double,
) {
    init {
        require(south <= north) { "south ($south) > north ($north)" }
        require(west <= east) { "west ($west) > east ($east)" }
    }

    fun intersects(other: BoundingBox): Boolean =
        south <= other.north && north >= other.south && west <= other.east && east >= other.west

    fun contains(lat: Double, lon: Double): Boolean =
        lat in south..north && lon in west..east

    /** Format für Overpass-Filter: (süd,west,nord,ost). */
    fun toOverpass(): String = "$south,$west,$north,$east"

    companion object {
        /** Grobe Ausdehnung Deutschlands inkl. kleinem Rand. */
        val GERMANY = BoundingBox(south = 47.2, west = 5.8, north = 55.1, east = 15.1)
    }
}
