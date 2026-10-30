package de.garbageview.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TileGridTest {

    private val grid = TileGrid(0.05)

    @Test
    fun tilesCoverBoundingBox() {
        val bbox = BoundingBox(52.51, 13.38, 52.56, 13.44)
        val tiles = grid.tilesFor(bbox)
        assertEquals(4, tiles.size)
        tiles.forEach { assertTrue(grid.bounds(it).intersects(bbox)) }
    }

    @Test
    fun tileBoundsRoundTrip() {
        val tile = grid.tilesFor(BoundingBox(52.52, 13.40, 52.52, 13.40)).single()
        val bounds = grid.bounds(tile)
        assertTrue(bounds.contains(52.52, 13.40))
        assertEquals(0.05, bounds.north - bounds.south, 1e-9)
    }

    @Test
    fun negativeCoordinates() {
        val tile = grid.tilesFor(BoundingBox(-0.01, -0.01, -0.01, -0.01)).single()
        assertEquals(TileGrid.Tile(-1, -1), tile)
    }

    @Test
    fun boundsOfTiles() {
        val tiles = grid.tilesFor(BoundingBox(52.51, 13.38, 52.56, 13.44))
        val bounds = grid.boundsOf(tiles)
        assertEquals(52.50, bounds.south, 1e-9)
        assertEquals(52.60, bounds.north, 1e-9)
        assertEquals(13.35, bounds.west, 1e-9)
        assertEquals(13.45, bounds.east, 1e-9)
        assertEquals(TileGrid.Tile(268, 1050), grid.tileOf(52.52, 13.40))
        assertEquals(TileGrid.Tile(670, 2626), TileGrid().tileOf(52.52, 13.40))
    }

    @Test
    fun centerFirst() {
        val tiles = grid.tilesFor(BoundingBox(52.40, 13.30, 52.60, 13.50))
        val sorted = grid.sortedByDistance(tiles, 52.52, 13.41)
        assertTrue(grid.bounds(sorted.first()).contains(52.52, 13.41))
    }
}
