package de.garbageview.core

import kotlin.math.floor

/**
 * Teilt die Welt in feste Kacheln von [sizeDegrees] Grad. Abfragen und Cache arbeiten
 * pro Kachel, damit kleine Kartenverschiebungen keine neuen Abfragen auslösen.
 */
class TileGrid(val sizeDegrees: Double = 0.02) {

    data class Tile(val x: Int, val y: Int) {
        val key: String get() = "${x}_$y"
    }

    fun tilesFor(bbox: BoundingBox): List<Tile> {
        val x0 = index(bbox.west)
        val x1 = index(bbox.east)
        val y0 = index(bbox.south)
        val y1 = index(bbox.north)
        val tiles = ArrayList<Tile>((x1 - x0 + 1) * (y1 - y0 + 1))
        for (y in y0..y1) for (x in x0..x1) tiles += Tile(x, y)
        return tiles
    }

    fun bounds(tile: Tile): BoundingBox = BoundingBox(
        south = tile.y * sizeDegrees,
        west = tile.x * sizeDegrees,
        north = (tile.y + 1) * sizeDegrees,
        east = (tile.x + 1) * sizeDegrees,
    )

    fun tileOf(lat: Double, lon: Double): Tile = Tile(index(lon), index(lat))

    /** Kleinstes Rechteck, das alle [tiles] umfasst. */
    fun boundsOf(tiles: Collection<Tile>): BoundingBox {
        require(tiles.isNotEmpty())
        return BoundingBox(
            south = tiles.minOf { it.y } * sizeDegrees,
            west = tiles.minOf { it.x } * sizeDegrees,
            north = (tiles.maxOf { it.y } + 1) * sizeDegrees,
            east = (tiles.maxOf { it.x } + 1) * sizeDegrees,
        )
    }

    /** Sortiert Kacheln nach Abstand zum Mittelpunkt, damit die Mitte zuerst lädt. */
    fun sortedByDistance(tiles: List<Tile>, centerLat: Double, centerLon: Double): List<Tile> {
        val cx = centerLon / sizeDegrees - 0.5
        val cy = centerLat / sizeDegrees - 0.5
        return tiles.sortedBy { (it.x - cx) * (it.x - cx) + (it.y - cy) * (it.y - cy) }
    }

    private fun index(value: Double): Int = floor(value / sizeDegrees).toInt()
}
