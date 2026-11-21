package com.bb.coloringbook

/**
 * Finds enclosed regions in line art. Pure Kotlin (no Android types) so it is unit-testable.
 *
 * @param isLine true where the pixel is part of an outline (a fill boundary).
 */
class RegionFinder(val width: Int, val height: Int, private val isLine: BooleanArray) {

    /** A region: mask over the bounding box [left, top, right) x [top, bottom). 1 = inside. */
    class Region(val left: Int, val top: Int, val right: Int, val bottom: Int, val mask: ByteArray) {
        val w get() = right - left
        val h get() = bottom - top
        fun contains(x: Int, y: Int): Boolean =
            x in left until right && y in top until bottom && mask[(y - top) * w + (x - left)].toInt() != 0
    }

    private val visited = ByteArray(width * height)
    private var stack = IntArray(4096)

    /**
     * Scanline flood fill from (sx, sy) over non-line pixels, then grows the result
     * [grow] pixels into adjacent line pixels so color tucks under anti-aliased edges.
     * Returns null when the start point is on a line or out of bounds.
     */
    fun find(sx: Int, sy: Int, grow: Int = 2): Region? {
        if (sx !in 0 until width || sy !in 0 until height) return null
        if (isLine[sy * width + sx]) return null
        java.util.Arrays.fill(visited, 0)

        var minX = sx; var maxX = sx; var minY = sy; var maxY = sy
        var sp = 0
        fun push(v: Int) {
            if (sp == stack.size) stack = stack.copyOf(stack.size * 2)
            stack[sp++] = v
        }
        push(sy * width + sx)
        while (sp > 0) {
            val p = stack[--sp]
            if (visited[p].toInt() != 0) continue
            val y = p / width
            val row = y * width
            var x1 = p - row
            while (x1 > 0 && !isLine[row + x1 - 1] && visited[row + x1 - 1].toInt() == 0) x1--
            var x2 = p - row
            while (x2 < width - 1 && !isLine[row + x2 + 1] && visited[row + x2 + 1].toInt() == 0) x2++
            if (x1 < minX) minX = x1; if (x2 > maxX) maxX = x2
            if (y < minY) minY = y; if (y > maxY) maxY = y
            var aboveOpen = false; var belowOpen = false
            for (x in x1..x2) {
                visited[row + x] = 1
                if (y > 0) {
                    val q = row - width + x
                    val ok = !isLine[q] && visited[q].toInt() == 0
                    if (ok && !aboveOpen) push(q)
                    aboveOpen = ok
                }
                if (y < height - 1) {
                    val q = row + width + x
                    val ok = !isLine[q] && visited[q].toInt() == 0
                    if (ok && !belowOpen) push(q)
                    belowOpen = ok
                }
            }
        }

        val left = maxOf(0, minX - grow); val top = maxOf(0, minY - grow)
        val right = minOf(width, maxX + grow + 1); val bottom = minOf(height, maxY + grow + 1)
        val w = right - left; val h = bottom - top
        var mask = ByteArray(w * h)
        for (y in top until bottom) {
            val src = y * width; val dst = (y - top) * w
            for (x in left until right) mask[dst + x - left] = visited[src + x]
        }
        // Grow into line pixels only (never across them into neighbouring regions).
        repeat(grow) {
            val next = mask.copyOf()
            for (y in 0 until h) for (x in 0 until w) {
                val i = y * w + x
                if (mask[i].toInt() != 0) continue
                if (!isLine[(y + top) * width + x + left]) continue
                if ((x > 0 && mask[i - 1].toInt() != 0) || (x < w - 1 && mask[i + 1].toInt() != 0) ||
                    (y > 0 && mask[i - w].toInt() != 0) || (y < h - 1 && mask[i + w].toInt() != 0)
                ) next[i] = 1
            }
            mask = next
        }
        return Region(left, top, right, bottom, mask)
    }

    companion object {
        /** Pixel is a line if its luminance is below [threshold] (0..255). Transparent = white. */
        fun linesFromArgb(pixels: IntArray, threshold: Int = 170): BooleanArray =
            BooleanArray(pixels.size) { i ->
                val c = pixels[i]
                val a = (c ushr 24) and 0xff
                val r = (c shr 16) and 0xff; val g = (c shr 8) and 0xff; val b = c and 0xff
                var lum = (r * 299 + g * 587 + b * 114) / 1000
                lum = (lum * a + 255 * (255 - a)) / 255 // composite on white
                lum < threshold
            }
    }
}
