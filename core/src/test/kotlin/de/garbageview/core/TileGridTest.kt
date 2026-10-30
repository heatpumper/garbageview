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
    fun centerFirst() {
        val tiles = grid.tilesFor(BoundingBox(52.40, 13.30, 52.60, 13.50))
        val sorted = grid.sortedByDistance(tiles, 52.52, 13.41)
        assertTrue(grid.bounds(sorted.first()).contains(52.52, 13.41))
    }
}
