package com.bb.coloringbook

import org.junit.Assert.*
import org.junit.Test

class RegionFinderTest {
    /** 20x20 image: square outline from (5,5) to (14,14). */
    private fun box(): RegionFinder {
        val w = 20; val h = 20
        val line = BooleanArray(w * h)
        for (i in 5..14) { line[5 * w + i] = true; line[14 * w + i] = true; line[i * w + 5] = true; line[i * w + 14] = true }
        return RegionFinder(w, h, line)
    }

    @Test fun insideRegionStaysInside() {
        val r = box().find(10, 10, grow = 0)!!
        assertTrue(r.contains(6, 6)); assertTrue(r.contains(13, 13))
        assertFalse(r.contains(5, 10)); assertFalse(r.contains(2, 2)); assertFalse(r.contains(16, 16))
        assertEquals(6, r.left); assertEquals(14, r.right)
    }

    @Test fun outsideRegionWrapsAround() {
        val r = box().find(0, 0, grow = 0)!!
        assertTrue(r.contains(19, 19)); assertTrue(r.contains(0, 19))
        assertFalse(r.contains(10, 10)); assertFalse(r.contains(14, 14))
    }

    @Test fun growCoversOutlineButNotOtherSide() {
        val r = box().find(10, 10, grow = 1)!!
        assertTrue(r.contains(5, 10))   // the outline itself
        assertFalse(r.contains(4, 10))  // outside region is not reached
    }

    @Test fun startOnLineReturnsNull() { assertNull(box().find(5, 5)) }
    @Test fun outOfBoundsReturnsNull() { assertNull(box().find(-1, 3)); assertNull(box().find(20, 3)) }

    @Test fun luminanceThresholdAndTransparency() {
        val px = intArrayOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0x00000000, 0xFF808080.toInt(), 0xFFE0E0E0.toInt())
        assertArrayEquals(booleanArrayOf(true, false, false, true, false), RegionFinder.linesFromArgb(px))
    }

    @Test fun largeImageIsFast() {
        val w = 2048; val h = 2048
        val f = RegionFinder(w, h, BooleanArray(w * h))
        val t = System.nanoTime()
        val r = f.find(1000, 1000)!!
        val ms = (System.nanoTime() - t) / 1e6
        assertEquals(w, r.w)
        assertTrue("took $ms ms", ms < 1500)
    }
}
