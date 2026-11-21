package com.bb.coloringbook

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

private typealias Art = Pair<String, (Pen) -> Unit>

/** Shared pattern helpers. */
private fun Pen.hex(cx: Float, cy: Float, r: Float, rot: Double = PI / 6) = path {
    for (i in 0..6) {
        val a = rot + PI / 3 * i
        val x = cx + (cos(a) * r).toFloat(); val y = cy + (sin(a) * r).toFloat()
        if (i == 0) m(x, y) else l(x, y)
    }
    z()
}

private fun Pen.poly(cx: Float, cy: Float, n: Int, r: Float, rot: Double = -PI / 2) = path {
    for (i in 0..n) {
        val a = rot + 2 * PI / n * i
        val x = cx + (cos(a) * r).toFloat(); val y = cy + (sin(a) * r).toFloat()
        if (i == 0) m(x, y) else l(x, y)
    }
    z()
}

private fun Pen.wavyH(y: Float, amp: Float, period: Float, phase: Float = 0f, x0: Float = -2f, x1: Float = 102f) = path {
    var x = x0
    m(x, y + amp * sin((x / period * 2 * PI + phase)).toFloat())
    while (x < x1) { x += 1f; l(x, y + amp * sin((x / period * 2 * PI + phase)).toFloat()) }
}

private fun Pen.wavyV(x: Float, amp: Float, period: Float, phase: Float = 0f) = path {
    var y = -2f
    m(x + amp * sin((y / period * 2 * PI + phase)).toFloat(), y)
    while (y < 102f) { y += 1f; l(x + amp * sin((y / period * 2 * PI + phase)).toFloat(), y) }
}

/** Lens-shaped leaf from (x,y) pointing at angle [a] (radians). */
private fun Pen.leaf(x: Float, y: Float, a: Double, len: Float, w: Float, vein: Boolean = true) {
    val tx = x + (cos(a) * len).toFloat(); val ty = y + (sin(a) * len).toFloat()
    val mx = (x + tx) / 2; val my = (y + ty) / 2
    val nx = (cos(a + PI / 2) * w).toFloat(); val ny = (sin(a + PI / 2) * w).toFloat()
    path(whiteFill = true) { m(x, y); q(mx + nx, my + ny, tx, ty); q(mx - nx, my - ny, x, y); z() }
    if (vein) line(x, y, x + (tx - x) * 0.85f, y + (ty - y) * 0.85f)
}

/** Simple round-petal flower. */
private fun Pen.flower(cx: Float, cy: Float, r: Float, petals: Int = 6) {
    for (i in 0 until petals) {
        val a = 2 * PI / petals * i
        disc(cx + (cos(a) * r * 0.6f).toFloat(), cy + (sin(a) * r * 0.6f).toFloat(), r * 0.42f)
    }
    disc(cx, cy, r * 0.35f)
}

private fun Pen.star4(x: Float, y: Float, r: Float) = path {
    m(x, y - r); q(x, y, x + r, y); q(x, y, x, y + r); q(x, y, x - r, y); q(x, y, x, y - r); z()
}

private fun Pen.pine(x: Float, base: Float, h: Float) {
    rectDisc(x - h * 0.05f, base - h * 0.18f, x + h * 0.05f, base)
    for (i in 0 until 3) {
        val top = base - h + i * h * 0.22f
        val bottom = base - h * 0.15f - (2 - i) * h * 0.2f
        val w = h * (0.18f + i * 0.08f)
        path(whiteFill = true) { m(x, top); l(x + w, bottom); l(x - w, bottom); z() }
    }
}

/** Fish-scale rows (discs drawn top to bottom). */
private fun Pen.scales(r: Float, y0: Float = -4f, y1: Float = 104f, inner: Boolean = true) {
    var row = 0; var y = y0
    while (y <= y1) {
        var x = -r + (if (row % 2 == 1) r else 0f)
        while (x <= 100f + r) { disc(x, y, r); if (inner) circle(x, y, r * 0.55f); x += 2 * r }
        y += r * 1.1f; row++
    }
}

object Designs {
    // ======================================================================== Geometric & tiles
    val geometric: List<Art> = listOf(
        "Plaid" to { p -> p.run {
            val pos = listOf(4f, 13f, 17f, 30f, 41f, 45f, 55f, 59f, 70f, 83f, 87f, 96f)
            for (v in pos) { line(v, 4f, v, 96f); line(4f, v, 96f, v) }
        } },
        "Pinwheel quilt" to { p -> p.run {
            rect(3f, 3f, 97f, 97f)
            val n = 4; val sz = 88f / n
            for (i in 0 until n) for (j in 0 until n) {
                val x = 6f + i * sz; val y = 6f + j * sz
                rect(x, y, x + sz, y + sz)
                if ((i + j) % 2 == 0) line(x, y, x + sz, y + sz) else line(x + sz, y, x, y + sz)
                if ((i + j) % 2 == 0) line(x + sz / 2, y, x + sz / 2, y + sz / 2) else line(x, y + sz / 2, x + sz / 2, y + sz / 2)
            }
        } },
        "Honeycomb" to { p -> p.run {
            clip({ m(4f, 4f); l(96f, 4f); l(96f, 96f); l(4f, 96f); z() }) {
                val r = 9f; val w = sqrt(3f) * r
                var row = 0; var y = 0f
                while (y < 110f) {
                    var x = if (row % 2 == 0) 0f else w / 2
                    while (x < 110f) { hex(x, y, r); hex(x, y, r * 0.5f); x += w }
                    y += r * 1.5f; row++
                }
            }
            rect(4f, 4f, 96f, 96f)
        } },
        "Star tiles" to { p -> p.run {
            rect(2f, 2f, 98f, 98f)
            val cs = listOf(14f, 38f, 62f, 86f)
            for (cx in cs) for (cy in cs) {
                poly(cx, cy, 4, 10f, 0.0); poly(cx, cy, 4, 10f, PI / 4)
                circle(cx, cy, 4f)
                if (cx < 80f) line(cx + 10f, cy, cx + 14f, cy)
                if (cy < 80f) line(cx, cy + 10f, cx, cy + 14f)
            }
            for (cx in listOf(26f, 50f, 74f)) for (cy in listOf(26f, 50f, 74f)) poly(cx, cy, 4, 5f, 0.0)
        } },
        "Triangle medallion" to { p -> p.run {
            clip({ m(50f, 4f); c(76f, 4f, 96f, 24f, 96f, 50f); c(96f, 76f, 76f, 96f, 50f, 96f); c(24f, 96f, 4f, 76f, 4f, 50f); c(4f, 24f, 24f, 4f, 50f, 4f); z() }) {
                val h = 10f
                var k = -12
                while (k <= 12) {
                    val y = 50f + k * h; line(-10f, y, 110f, y)
                    val d = k * h * 2 / sqrt(3f)
                    line(50f + d - 70f, 50f - 70f * sqrt(3f), 50f + d + 70f, 50f + 70f * sqrt(3f))
                    line(50f + d + 70f, 50f - 70f * sqrt(3f), 50f + d - 70f, 50f + 70f * sqrt(3f))
                    k++
                }
            }
            circle(50f, 50f, 46f)
            hex(50f, 50f, 11.5f, 0.0)
        } },
        "Argyle" to { p -> p.run {
            clip({ m(4f, 4f); l(96f, 4f); l(96f, 96f); l(4f, 96f); z() }) {
                var c0 = -100f
                while (c0 < 200f) { line(c0, 0f, c0 + 100f, 100f); line(c0 + 100f, 0f, c0, 100f); c0 += 16f }
                p.width(0.3f)
                c0 = -92f
                while (c0 < 200f) { line(c0, 0f, c0 + 100f, 100f); line(c0 + 100f, 0f, c0, 100f); c0 += 16f }
                p.width(0.5f)
            }
            rect(4f, 4f, 96f, 96f)
        } },
    )

    // ======================================================================== Indian motifs
    val indian: List<Art> = listOf(
        "Rangoli" to { p -> M(p).run {
            roundPetals(8, 30f, 47f, 9f, 0.0, white = true)
            roundPetals(8, 30f, 42f, 4.5f, 0.0, white = true)
            dots(8, 40f, 1.4f, PI / 8)
            p.disc(50f, 50f, 31f)
            ring(5f)
            roundPetals(8, 5f, 17f, 5f)
            ring(19f)
            petals(16, 19f, 29f, 3.4f)
            dots(16, 24f, 0.9f, PI / 16)
            ring(31f)
        } },
        "Kolam" to { p -> p.run {
            poly(50f, 50f, 4, 46f)
            for (i in -2..2) for (j in -2..2) {
                if (kotlin.math.abs(i) + kotlin.math.abs(j) > 2) continue
                val x = 50f + i * 15f; val y = 50f + j * 15f
                poly(x, y, 4, 7.5f); circle(x, y, 3.6f); dot(x, y, 1.1f)
            }
        } },
        "Paisley" to { p -> p.run {
            rect(4f, 4f, 96f, 96f, 6f)
            val shape: Pen.PathB.() -> Unit = {
                m(50f, 92f); c(22f, 92f, 12f, 60f, 28f, 42f); c(40f, 26f, 62f, 20f, 80f, 6f)
                c(72f, 24f, 84f, 40f, 82f, 56f); c(80f, 80f, 66f, 92f, 50f, 92f); z()
            }
            val o = 52f to 62f
            path(whiteFill = true, build = shape)
            clipK(1f, o.first, o.second, shape) { for (i in 0 until 36) { val a = 2 * PI * i / 36; line(o.first, o.second, o.first + (cos(a) * 60).toFloat(), o.second + (sin(a) * 60).toFloat()) } }
            path(whiteFill = true, k = 0.78f, ox = o.first, oy = o.second, build = shape)
            path(whiteFill = true, k = 0.58f, ox = o.first, oy = o.second, build = shape)
            clipK(0.58f, o.first, o.second, shape) { for (i in 0 until 16) { val a = 2 * PI * i / 16; line(o.first, o.second, o.first + (cos(a) * 40).toFloat(), o.second + (sin(a) * 40).toFloat()) } }
            path(whiteFill = true, k = 0.4f, ox = o.first, oy = o.second, build = shape)
            flower(o.first, o.second + 2f, 7f, 8)
            for ((x, y) in listOf(12f to 12f, 88f to 88f, 12f to 88f, 88f to 12f)) { circle(x, y, 3.5f); dot(x, y, 1f) }
        } },
        "Mehndi circle" to { p -> M(p).run {
            ring(6f)
            roundPetals(8, 6f, 22f, 6f)
            roundPetals(8, 6f, 15f, 3f)
            ring(25f)
            drops(16, 25f, 38f, 3.2f)
            dots(16, 36.5f, 0.9f, PI / 16)
            ring(40.5f)
            scallops(30, 43.4f, 2.9f)
            dots(30, 47.6f, 0.8f, PI / 30)
        } },
        "Lotus" to { p -> p.run {
            circle(50f, 46f, 34f); circle(50f, 46f, 28f)
            for (y in listOf(80f, 87f, 94f)) wavyH(y, 1.4f, 16f, y)
            ovalDisc(6f, 76f, 36f, 86f); line(21f, 81f, 30f, 77f)
            ovalDisc(64f, 82f, 96f, 92f); line(80f, 87f, 72f, 83f)
            for (sx in listOf(1f, -1f)) {
                fun x(v: Float) = 50f + sx * (v - 50f)
                path(whiteFill = true) { m(50f, 78f); c(x(26f), 80f, x(10f), 66f, x(8f), 54f); c(x(24f), 54f, x(40f), 64f, 50f, 78f); z() }
            }
            for (sx in listOf(1f, -1f)) {
                fun x(v: Float) = 50f + sx * (v - 50f)
                path(whiteFill = true) { m(50f, 78f); c(x(34f), 72f, x(22f), 56f, x(24f), 38f); c(x(36f), 46f, x(46f), 60f, 50f, 78f); z() }
                line(x(28f), 46f, x(44f), 70f)
            }
            path(whiteFill = true) { m(50f, 78f); c(40f, 60f, 42f, 36f, 50f, 22f); c(58f, 36f, 60f, 60f, 50f, 78f); z() }
            line(50f, 30f, 50f, 72f)
        } },
        "Diya" to { p -> p.run {
            circle(50f, 34f, 22f); circle(50f, 34f, 16f)
            for (i in 0 until 12) { val a = 2 * PI * i / 12; line(50f + (cos(a) * 24).toFloat(), 34f + (sin(a) * 24).toFloat(), 50f + (cos(a) * 30).toFloat(), 34f + (sin(a) * 30).toFloat()) }
            ovalDisc(36f, 84f, 64f, 92f)
            path(whiteFill = true) { m(14f, 60f); q(50f, 100f, 86f, 60f); z() }
            clip({ m(14f, 60f); q(50f, 100f, 86f, 60f); z() }) {
                path { for (i in 0..14) { val x = 14f + i * 5f; if (i == 0) m(x, 68f) else l(x, if (i % 2 == 0) 68f else 73f) } }
                for (x in listOf(26f, 38f, 50f, 62f, 74f)) { circle(x, 79f, 2.6f) }
            }
            ovalDisc(14f, 55f, 86f, 65f)
            ovalDisc(28f, 57f, 72f, 63f)
            line(50f, 58f, 50f, 52f)
            path(whiteFill = true) { m(50f, 18f); c(58f, 32f, 60f, 44f, 50f, 52f); c(40f, 44f, 42f, 32f, 50f, 18f); z() }
            path(whiteFill = true) { m(50f, 32f); c(54f, 40f, 54f, 46f, 50f, 49f); c(46f, 46f, 46f, 40f, 50f, 32f); z() }
            for (x in listOf(8f, 92f)) { star4(x, 20f, 4f); star4(x, 84f, 3f) }
        } },
    )

    // ======================================================================== Zen patterns
    val zen: List<Art> = listOf(
        "Scales" to { p -> p.run {
            clip({ m(50f, 4f); c(76f, 4f, 96f, 24f, 96f, 50f); c(96f, 76f, 76f, 96f, 50f, 96f); c(24f, 96f, 4f, 76f, 4f, 50f); c(4f, 24f, 24f, 4f, 50f, 4f); z() }) { scales(6.5f) }
            circle(50f, 50f, 46f)
        } },
        "Bubbles" to { p -> p.run {
            val rnd = java.util.Random(7)
            val placed = mutableListOf<Triple<Float, Float, Float>>()
            repeat(3000) {
                val r = 2.5f + rnd.nextFloat() * 10f
                val x = 4f + rnd.nextFloat() * 92f; val y = 4f + rnd.nextFloat() * 92f
                val d0 = sqrt((x - 50f) * (x - 50f) + (y - 50f) * (y - 50f))
                if (d0 + r > 45f) return@repeat
                if (placed.any { (px, py, pr) -> sqrt((px - x) * (px - x) + (py - y) * (py - y)) < pr + r + 0.8f }) return@repeat
                placed.add(Triple(x, y, r))
            }
            for ((x, y, r) in placed) { circle(x, y, r); if (r > 6f) circle(x, y, r * 0.6f); if (r > 9f) circle(x, y, r * 0.28f) }
            circle(50f, 50f, 46f)
        } },
        "Braids" to { p -> p.run {
            clip({ m(4f, 4f); l(96f, 4f); l(96f, 96f); l(4f, 96f); z() }) {
                for (k in 0 until 6) {
                    val b = 12f + k * 15.2f
                    wavyH(b, 5f, 20f, k * 1.3f); wavyH(b, -5f, 20f, k * 1.3f)
                    if (k < 5) line(0f, b + 7.6f, 100f, b + 7.6f)
                }
            }
            rect(4f, 4f, 96f, 96f)
        } },
        "Tangle sampler" to { p -> p.run {
            val sz = 28f; val gap = 3f; val o = (100f - 3 * sz - 2 * gap) / 2
            var idx = 0
            for (j in 0 until 3) for (i in 0 until 3) {
                val x0 = o + i * (sz + gap); val y0 = o + j * (sz + gap); val x1 = x0 + sz; val y1 = y0 + sz
                val cx = (x0 + x1) / 2; val cy = (y0 + y1) / 2
                clip({ m(x0, y0); l(x1, y0); l(x1, y1); l(x0, y1); z() }) {
                    when (idx) {
                        0 -> { var c0 = -40f; while (c0 < 40f) { line(cx + c0 - 20f, cy - 20f, cx + c0 + 20f, cy + 20f); c0 += 5f } }
                        1 -> { var d = 2.5f; while (d < 14f) { rect(x0 + d, y0 + d, x1 - d, y1 - d); d += 3f } }
                        2 -> { var yy = y0 + 3.5f; while (yy < y1) { var xx = x0 + 3.5f; while (xx < x1) { circle(xx, yy, 2.4f); xx += 7f }; yy += 7f } }
                        3 -> { var yy = y0 + 3f; while (yy < y1 + 2f) { wavyH(yy, 1.2f, 8f, 0f, x0 - 2f, x1 + 2f); yy += 4.5f } }
                        4 -> { var t = 3f; while (t < sz) { line(x0 + t, y0, x0 + t, y1); line(x0, y0 + t, x1, y0 + t); t += 5f }; line(x0, y0, x1, y1); line(x1, y0, x0, y1) }
                        5 -> { var r = 4f; while (r < 42f) { circle(x0, y1, r); r += 4f } }
                        6 -> { var yy = y0; var row = 0; while (yy < y1) { path { var xx = x0 - 4f; m(xx, yy + 4f); while (xx < x1 + 4f) { xx += 4f; l(xx, yy + if (((xx - x0) / 4f).toInt() % 2 == 0) 4f else 0f) } }; yy += 4f; row++ } }
                        7 -> { var pts = listOf(x0 + 1f to y0 + 1f, x1 - 1f to y0 + 1f, x1 - 1f to y1 - 1f, x0 + 1f to y1 - 1f)
                               repeat(14) { path { m(pts[0].first, pts[0].second); for (q in 1..3) l(pts[q].first, pts[q].second); z() }
                                   pts = List(4) { n -> val a = pts[n]; val b = pts[(n + 1) % 4]; (a.first + (b.first - a.first) * 0.15f) to (a.second + (b.second - a.second) * 0.15f) } } }
                        else -> { var yy = y0 + 4.7f; while (yy < y1) { var xx = x0 + 4.7f; while (xx < x1) { circle(xx, yy, 3.2f); dot(xx, yy, 0.8f); xx += 9.3f }; yy += 9.3f } }
                    }
                }
                rect(x0, y0, x1, y1)
                idx++
            }
        } },
        "Twisted squares" to { p -> p.run {
            var pts = listOf(8f to 8f, 92f to 8f, 92f to 92f, 8f to 92f)
            repeat(30) {
                path { m(pts[0].first, pts[0].second); for (q in 1..3) l(pts[q].first, pts[q].second); z() }
                pts = List(4) { n -> val a = pts[n]; val b = pts[(n + 1) % 4]; (a.first + (b.first - a.first) * 0.1f) to (a.second + (b.second - a.second) * 0.1f) }
            }
        } },
        "Wave grid" to { p -> p.run {
            clip({ m(4f, 4f); l(96f, 4f); l(96f, 96f); l(4f, 96f); z() }) {
                var y = 2f; while (y < 104f) { wavyH(y, 2.2f, 18f, y * 0.3f); y += 8f }
                var x = 2f; while (x < 104f) { wavyV(x, 2.2f, 18f, x * 0.3f); x += 12f }
            }
            rect(4f, 4f, 96f, 96f)
        } },
    )

    // ======================================================================== Nature & botanicals
    val nature: List<Art> = listOf(
        "Flower wreath" to { p -> p.run {
            M(p).run { leaves(18, 22f, 40f, 3.4f, PI / 18) }
            circle(50f, 50f, 31f)
            for (i in 0 until 6) { val a = 2 * PI * i / 6 - PI / 2; flower(50f + (cos(a) * 31).toFloat(), 50f + (sin(a) * 31).toFloat(), 8f, 6) }
            M(p).run { ring(14f); roundPetals(6, 3f, 13f, 3.5f) }
            circle(50f, 50f, 46f)
        } },
        "Big leaf" to { p -> p.run {
            path { m(50f, 92f); q(52f, 96f, 56f, 100f) }
            val leafShape: Pen.PathB.() -> Unit = { m(50f, 92f); c(8f, 80f, 6f, 30f, 50f, 6f); c(94f, 30f, 92f, 80f, 50f, 92f); z() }
            path(whiteFill = true, build = leafShape)
            clip(leafShape) {
                path { m(50f, 92f); q(47f, 50f, 50f, 6f) }
                for (k in 0 until 8) {
                    val y = 84f - k * 9.5f
                    path { m(49f, y); q(34f, y - 4f, 14f, y - 16f) }
                    path { m(49f, y); q(64f, y - 4f, 86f, y - 16f) }
                }
            }
        } },
        "Fern" to { p -> p.run {
            fun b(t: Float, a: Float, c: Float, d: Float) = (1 - t) * (1 - t) * a + 2 * (1 - t) * t * c + t * t * d
            path { m(50f, 98f); q(36f, 52f, 62f, 6f) }
            for (i in 0 until 13) {
                val t = 0.08f + i * 0.068f
                val x = b(t, 50f, 36f, 62f); val y = b(t, 98f, 52f, 6f)
                val dx = 2 * (1 - t) * (36f - 50f) + 2 * t * (62f - 36f); val dy = 2 * (1 - t) * (52f - 98f) + 2 * t * (6f - 52f)
                val ta = kotlin.math.atan2(dy, dx).toDouble()
                val len = 20f * (1 - t) + 5f
                leaf(x, y, ta - 1.0, len, len * 0.22f)
                leaf(x, y, ta + 1.0, len, len * 0.22f)
            }
            leaf(62f, 6f, -1.0, 5f, 1.6f, vein = false)
        } },
        "Sunflower" to { p -> p.run {
            path(whiteFill = true) { m(48f, 70f); q(46f, 86f, 48f, 100f); l(52f, 100f); q(50f, 86f, 52f, 70f); z() }
            leaf(50f, 88f, -2.6, 28f, 7f); leaf(50f, 80f, -0.5, 26f, 7f)
            M(p, 50f, 40f).run { roundPetals(16, 13f, 32f, 4.6f, PI / 16, white = true); roundPetals(16, 13f, 27f, 4f, 0.0, white = true) }
            disc(50f, 40f, 14f)
            clip({ m(36f, 40f); c(36f, 21f, 64f, 21f, 64f, 40f); c(64f, 59f, 36f, 59f, 36f, 40f); z() }) {
                var c0 = -40f; while (c0 < 40f) { line(50f + c0 - 20f, 20f, 50f + c0 + 20f, 60f); line(50f + c0 + 20f, 20f, 50f + c0 - 20f, 60f); c0 += 4.5f }
            }
            circle(50f, 40f, 14f)
        } },
        "Tree of life" to { p -> p.run {
            line(0f, 92f, 100f, 92f)
            for (x in listOf(10f, 20f, 80f, 90f)) leaf(x, 92f, -PI / 2 - 0.3, 8f, 2f, false)
            disc(50f, 40f, 33f)
            M(p, 50f, 40f).run { scallops(14, 25f, 5.3f); scallops(9, 14.5f, 4.6f, PI / 9); ring(5f) }
            path(whiteFill = true) { m(26f, 100f); q(40f, 92f, 44f, 86f); q(46f, 72f, 45f, 64f); l(55f, 64f); q(54f, 72f, 56f, 86f); q(60f, 92f, 74f, 100f); z() }
            path { m(47f, 72f); q(50f, 80f, 48f, 92f) }
        } },
        "Garden" to { p -> p.run {
            clip({ m(50f, 4f); c(76f, 4f, 96f, 24f, 96f, 50f); c(96f, 76f, 76f, 96f, 50f, 96f); c(24f, 96f, 4f, 76f, 4f, 50f); c(4f, 24f, 24f, 4f, 50f, 4f); z() }) {
                circle(24f, 22f, 7f)
                val fl = listOf(Triple(22f, 54f, 8f), Triple(40f, 44f, 9f), Triple(60f, 50f, 8f), Triple(78f, 40f, 9f), Triple(50f, 66f, 7f))
                for ((x, y, _) in fl) line(x, y, x, 100f)
                for ((x, y, _) in fl) { leaf(x, y + 18f, -0.6, 9f, 2.5f); leaf(x, y + 24f, -2.5, 9f, 2.5f) }
                var x = 2f
                val rnd = java.util.Random(3)
                while (x < 98f) { val h = 10f + rnd.nextFloat() * 10f; path(whiteFill = true) { m(x, 100f); l(x + 2f, 100f - h); l(x + 4f, 100f) }; x += 4f }
                for ((fx, fy, r) in fl) flower(fx, fy, r, 7)
            }
            circle(50f, 50f, 46f)
        } },
    )

    // ======================================================================== Ocean & waves
    val ocean: List<Art> = listOf(
        "Seigaiha waves" to { p -> p.run {
            clip({ m(4f, 4f); l(96f, 4f); l(96f, 96f); l(4f, 96f); z() }) {
                var row = 0; var y = 0f
                while (y < 112f) {
                    var x = if (row % 2 == 0) 0f else 8f
                    while (x < 112f) { disc(x, y, 8f); circle(x, y, 5.6f); circle(x, y, 3.2f); x += 16f }
                    y += 5f; row++
                }
            }
            rect(4f, 4f, 96f, 96f)
        } },
        "Scallop shell" to { p -> p.run {
            wavyH(95f, 1f, 14f)
            val cx = 50f; val cy = 82f
            val n = 10
            val edge = List(n + 1) { i -> val a = Math.toRadians(-165.0 + 150.0 * i / n); (cx + cos(a).toFloat() * 40f) to (cy + sin(a).toFloat() * 40f) }
            val shell: Pen.PathB.() -> Unit = {
                m(cx, cy); l(edge[0].first, edge[0].second)
                for (i in 0 until n) {
                    val am = Math.toRadians(-165.0 + 150.0 * (i + 0.5) / n)
                    q(cx + cos(am).toFloat() * 47f, cy + sin(am).toFloat() * 47f, edge[i + 1].first, edge[i + 1].second)
                }
                z()
            }
            path(whiteFill = true, build = shell)
            clip(shell) {
                for (e in edge) line(cx, cy, e.first, e.second)
                circle(cx, cy, 16f); circle(cx, cy, 29f)
            }
            path(whiteFill = true) { m(43f, 80f); l(57f, 80f); l(55f, 87f); l(45f, 87f); z() }
            for ((x, y, r) in listOf(Triple(12f, 20f, 3f), Triple(18f, 10f, 2f), Triple(86f, 16f, 2.5f))) circle(x, y, r)
        } },
        "Nautilus" to { p -> p.run {
            val cx = 54f; val cy = 50f
            fun P(t: Double): Pair<Float, Float> { val r = 1.6 * exp(0.2 * t); return (cx + (cos(t) * r).toFloat()) to (cy + (sin(t) * r).toFloat()) }
            val tEnd = 16.1
            path { var t = 0.0; val s0 = P(t); m(s0.first, s0.second); while (t < tEnd) { t += 0.04; val q0 = P(t); l(q0.first, q0.second) } }
            val e0 = P(tEnd); val e1 = P(tEnd - 2 * PI); line(e0.first, e0.second, e1.first, e1.second)
            var t = 2 * PI + 0.5
            while (t < tEnd) { val a = P(t); val b = P(t - 2 * PI); line(a.first, a.second, b.first, b.second); t += 0.42 }
            for ((x, y, r) in listOf(Triple(12f, 86f, 3f), Triple(18f, 76f, 2f), Triple(10f, 70f, 1.5f))) circle(x, y, r)
        } },
        "Ocean sunrise" to { p -> p.run {
            clip({ m(50f, 6f); c(74f, 6f, 94f, 26f, 94f, 50f); c(94f, 74f, 74f, 94f, 50f, 94f); c(26f, 94f, 6f, 74f, 6f, 50f); c(6f, 26f, 26f, 6f, 50f, 6f); z() }) {
                for (i in 0 until 14) { val a = PI + PI * (i + 0.5) / 14; line(50f + (cos(a) * 13).toFloat(), 52f + (sin(a) * 13).toFloat(), 50f + (cos(a) * 50).toFloat(), 52f + (sin(a) * 50).toFloat()) }
                disc(50f, 52f, 14f)
                for (k in 0 until 7) {
                    val y = 52f + k * 6.5f
                    path(whiteFill = true) {
                        var x = -2f; m(x, y + 1.8f * sin(x / 12f + k).toFloat())
                        while (x < 102f) { x += 1f; l(x, y + 1.8f * sin(x / 12f + k).toFloat()) }
                        l(102f, 102f); l(-2f, 102f); z()
                    }
                }
                for ((x, y) in listOf(28f to 26f, 36f to 20f, 70f to 24f)) { path { m(x - 3f, y); q(x - 1.5f, y - 2f, x, y) }; path { m(x, y); q(x + 1.5f, y - 2f, x + 3f, y) } }
            }
            circle(50f, 50f, 44f)
        } },
        "Jellyfish" to { p -> p.run {
            fun jelly(cx: Float, cy: Float, k: Float) {
                for (i in 0 until 5) {
                    val x = cx + (i - 2) * 7f * k
                    path(whiteFill = true) {
                        var y = cy; m(x - 1.6f * k, y)
                        while (y < cy + 44f * k) { y += 1f; l(x - 1.6f * k + 3f * k * sin(y / (4f * k) + i).toFloat(), y) }
                        while (y > cy) { y -= 1f; l(x + 1.6f * k + 3f * k * sin(y / (4f * k) + i).toFloat(), y) }
                        z()
                    }
                }
                path(whiteFill = true) {
                    m(cx - 24f * k, cy); c(cx - 24f * k, cy - 32f * k, cx + 24f * k, cy - 32f * k, cx + 24f * k, cy)
                    var x = cx + 24f * k
                    repeat(6) { q(x - 4f * k, cy + 5f * k, x - 8f * k, cy); x -= 8f * k }
                    z()
                }
                path { m(cx - 16f * k, cy - 3f * k); c(cx - 16f * k, cy - 22f * k, cx + 16f * k, cy - 22f * k, cx + 16f * k, cy - 3f * k); z() }
                for (dx in listOf(-8f, 0f, 8f)) circle(cx + dx * k, cy - 10f * k, 2.2f * k)
            }
            jelly(44f, 40f, 1f); jelly(80f, 20f, 0.45f); jelly(82f, 70f, 0.4f)
            for ((x, y, r) in listOf(Triple(12f, 14f, 3f), Triple(18f, 6f, 2f), Triple(14f, 86f, 2.5f))) circle(x, y, r)
        } },
        "Starfish" to { p -> p.run {
            var y = 8f; while (y < 100f) { wavyH(y, 1.2f, 22f, y); y += 7f }
            val cx = 50f; val cy = 52f
            fun pt(i: Double, r: Float) = (cx + (cos(-PI / 2 + 2 * PI * i / 5) * r).toFloat()) to (cy + (sin(-PI / 2 + 2 * PI * i / 5) * r).toFloat())
            path(whiteFill = true) {
                val s0 = pt(0.0, 42f); m(s0.first, s0.second)
                for (i in 0 until 5) { val c0 = pt(i + 0.5, 13f); val e = pt(i + 1.0, 42f); q(c0.first, c0.second, e.first, e.second) }
                z()
            }
            circle(cx, cy, 6f)
            for (i in 0 until 5) for ((r, s) in listOf(12f to 3f, 20f to 2.6f, 28f to 2f, 34.5f to 1.3f)) { val q0 = pt(i.toDouble(), r); circle(q0.first, q0.second, s) }
        } },
    )

    // ======================================================================== Stained glass (thick lines)
    val glass: List<Art> = listOf(
        "Rose window" to { p -> M(p).run {
            ring(46f); ring(30f); ring(12f); ring(5f)
            rays(12, 12f, 46f)
            roundPetals(12, 12f, 29f, 5f, PI / 12)
            scallops(12, 38f, 6.5f, PI / 12)
            rays(6, 5f, 12f)
        } },
        "Arch window" to { p -> p.run {
            val arch: Pen.PathB.() -> Unit = { m(20f, 96f); l(20f, 40f); q(20f, 6f, 50f, 4f); q(80f, 6f, 80f, 40f); l(80f, 96f); z() }
            clip(arch) {
                for (x in listOf(35f, 50f, 65f)) line(x, 0f, x, 100f)
                for (y in listOf(58f, 77f)) line(0f, y, 100f, y)
                disc(50f, 30f, 13f)
                for (i in 0 until 4) { val a = PI / 2 * i; circle(50f + (cos(a) * 6).toFloat(), 30f + (sin(a) * 6).toFloat(), 5f) }
            }
            path(build = arch)
            line(10f, 96f, 90f, 96f)
        } },
        "Sunburst" to { p -> p.run {
            clip({ m(4f, 4f); l(96f, 4f); l(96f, 96f); l(4f, 96f); z() }) {
                for (i in 0 until 14) { val a = PI + PI * (i + 0.5) / 14; line(50f, 96f, 50f + (cos(a) * 140).toFloat(), 96f + (sin(a) * 140).toFloat()) }
                for (r in listOf(36f, 58f, 80f)) circle(50f, 96f, r)
                disc(50f, 96f, 16f)
            }
            rect(4f, 4f, 96f, 96f)
        } },
        "Mosaic" to { p -> p.run {
            val rnd = java.util.Random(11)
            clip({ m(4f, 4f); l(96f, 4f); l(96f, 96f); l(4f, 96f); z() }) {
                repeat(11) {
                    val x = 10f + rnd.nextFloat() * 80f; val y = 10f + rnd.nextFloat() * 80f
                    val a = rnd.nextFloat() * PI
                    line(x - (cos(a) * 150).toFloat(), y - (sin(a) * 150).toFloat(), x + (cos(a) * 150).toFloat(), y + (sin(a) * 150).toFloat())
                }
            }
            rect(4f, 4f, 96f, 96f)
        } },
        "Tulip window" to { p -> p.run {
            clip({ m(6f, 6f); l(94f, 6f); l(94f, 94f); l(6f, 94f); z() }) {
                var t = 6f; while (t < 100f) { line(t, 0f, t, 100f); line(0f, t, 100f, t); t += 17.6f }
            }
            path(whiteFill = true) { m(48f, 60f); l(48f, 94f); l(52f, 94f); l(52f, 60f); z() }
            path(whiteFill = true) { m(50f, 92f); c(36f, 84f, 26f, 72f, 24f, 60f); c(36f, 64f, 46f, 74f, 50f, 92f); z() }
            path(whiteFill = true) { m(50f, 86f); c(64f, 80f, 74f, 70f, 76f, 58f); c(64f, 62f, 54f, 70f, 50f, 86f); z() }
            path(whiteFill = true) { m(34f, 26f); l(42f, 38f); l(50f, 22f); l(58f, 38f); l(66f, 26f); q(70f, 56f, 50f, 62f); q(30f, 56f, 34f, 26f); z() }
            path { m(42f, 38f); q(45f, 52f, 50f, 62f) }; path { m(58f, 38f); q(55f, 52f, 50f, 62f) }
            rect(6f, 6f, 94f, 94f)
        } },
        "Lattice medallion" to { p -> p.run {
            clip({ m(4f, 4f); l(96f, 4f); l(96f, 96f); l(4f, 96f); z() }) {
                var c0 = -100f
                while (c0 < 200f) { line(c0, 0f, c0 + 100f, 100f); line(c0 + 100f, 0f, c0, 100f); c0 += 14f }
            }
            disc(50f, 50f, 18f)
            for (i in 0 until 4) { val a = PI / 2 * i + PI / 4; circle(50f + (cos(a) * 7).toFloat(), 50f + (sin(a) * 7).toFloat(), 6.5f) }
            disc(50f, 50f, 4f)
            rect(4f, 4f, 96f, 96f)
        } },
    )

    // ======================================================================== Patterned animals
    val animals: List<Art> = listOf(
        "Owl" to { p -> p.run {
            rectDisc(6f, 86f, 94f, 92f, 3f)
            path(whiteFill = true) { m(30f, 36f); l(26f, 12f); l(44f, 26f); z() }
            path(whiteFill = true) { m(70f, 36f); l(74f, 12f); l(56f, 26f); z() }
            ovalDisc(24f, 20f, 76f, 90f)
            clipK(1f, 0f, 0f, { m(50f, 20f); c(64f, 20f, 76f, 36f, 76f, 55f); c(76f, 75f, 64f, 90f, 50f, 90f); c(36f, 90f, 24f, 75f, 24f, 55f); c(24f, 36f, 36f, 20f, 50f, 20f); z() }) {
                scales(4f, 50f, 100f)
            }
            for (sx in listOf(1f, -1f)) {
                fun x(v: Float) = 50f + sx * (v - 50f)
                val wing: Pen.PathB.() -> Unit = { m(x(26f), 48f); c(x(14f), 62f, x(18f), 80f, x(34f), 88f); c(x(32f), 74f, x(32f), 60f, x(26f), 48f); z() }
                path(whiteFill = true, build = wing)
                clip(wing) { for (k in 0 until 7) path { m(x(10f), 52f + k * 6f); q(x(26f), 56f + k * 6f, x(40f), 50f + k * 6f) } }
            }
            ovalDisc(30f, 26f, 70f, 54f)
            for (ex in listOf(40f, 60f)) { disc(ex, 39f, 9f); circle(ex, 39f, 6f); disc(ex, 39f, 3.5f); dot(ex + 0.8f, 39f, 1.8f) }
            path(whiteFill = true) { m(47f, 48f); l(53f, 48f); l(50f, 55f); z() }
            for (x in listOf(40f, 45f, 55f, 60f)) ovalDisc(x - 2.5f, 83f, x + 2.5f, 89f)
        } },
        "Fox" to { p -> p.run {
            val head: Pen.PathB.() -> Unit = { m(50f, 88f); l(16f, 48f); l(14f, 10f); l(36f, 28f); l(50f, 26f); l(64f, 28f); l(86f, 10f); l(84f, 48f); z() }
            path(whiteFill = true, build = head)
            clip(head) {
                var y = 30f; while (y < 50f) { wavyH(y, 1.2f, 10f, y); y += 4.5f }
                for (sx in listOf(1f, -1f)) { fun x(v: Float) = 50f + sx * (v - 50f)
                    var k = 0f; while (k < 3f) { path { m(x(18f + k * 3f), 16f + k * 5f); l(x(20f + k * 2f), 40f - k * 2f); l(x(34f - k * 2f), 30f + k) }; k += 1f } }
            }
            path(whiteFill = true) { m(50f, 88f); l(28f, 58f); q(40f, 52f, 50f, 62f); q(60f, 52f, 72f, 58f); z() }
            clip({ m(50f, 88f); l(28f, 58f); q(40f, 52f, 50f, 62f); q(60f, 52f, 72f, 58f); z() }) {
                for ((x, y) in listOf(36f to 64f, 42f to 70f, 58f to 70f, 64f to 64f, 46f to 76f, 54f to 76f)) circle(x, y, 1.8f)
            }
            for (ex in listOf(38f, 62f)) path(whiteFill = true) { m(ex - 6f, 48f); q(ex, 42f, ex + 6f, 48f); q(ex, 52f, ex - 6f, 48f); z() }
            dot(38f, 48f, 1.6f); dot(62f, 48f, 1.6f)
            dot(50f, 84f, 2.6f)
            path(build = head)
        } },
        "Elephant" to { p -> p.run {
            line(0f, 94f, 100f, 94f)
            for (x in listOf(42f, 76f)) rectDisc(x, 68f, x + 11f, 94f, 3f)
            path(whiteFill = true) { m(91f, 56f); q(96f, 64f, 94f, 72f); l(96f, 76f) }
            ovalDisc(36f, 34f, 94f, 82f)
            path(whiteFill = true) { m(54f, 36f); l(82f, 38f); l(84f, 62f); l(56f, 62f); z() }
            clip({ m(54f, 36f); l(82f, 38f); l(84f, 62f); l(56f, 62f); z() }) {
                rect(57f, 40f, 81f, 59f)
                path { for (i in 0..8) { val x = 57f + i * 3f; if (i == 0) m(x, 50f) else l(x, if (i % 2 == 0) 50f else 45f) } }
                for (x in listOf(60f, 66f, 72f, 78f)) circle(x, 54.5f, 1.8f)
            }
            for (x in listOf(56f, 64f, 72f, 80f)) path(whiteFill = true) { m(x - 3f, 62f); l(x, 67f); l(x + 3f, 62f) }
            rectDisc(58f, 70f, 69f, 94f, 3f)
            path(whiteFill = true) { m(20f, 46f); c(14f, 60f, 8f, 76f, 14f, 88f); l(21f, 87f); c(16f, 74f, 22f, 60f, 30f, 54f); z() }
            clip({ m(20f, 46f); c(14f, 60f, 8f, 76f, 14f, 88f); l(21f, 87f); c(16f, 74f, 22f, 60f, 30f, 54f); z() }) { var y = 50f; while (y < 90f) { line(0f, y, 40f, y - 3f); y += 5f } }
            disc(32f, 40f, 16f)
            val ear: Pen.PathB.() -> Unit = { m(36f, 28f); c(60f, 16f, 66f, 54f, 44f, 60f); c(40f, 58f, 38f, 46f, 36f, 28f); z() }
            path(whiteFill = true, build = ear)
            clip(ear) { M(p, 46f, 40f).run { ring(5f); ring(10f); roundPetals(8, 5f, 14f, 2.6f); ring(16f); scallops(12, 19f, 2.8f) } }
            path(build = ear)
            dot(26f, 37f, 1.8f)
        } },
        "Koi fish" to { p -> p.run {
            val body: Pen.PathB.() -> Unit = { m(10f, 50f); c(22f, 26f, 60f, 26f, 74f, 50f); c(60f, 74f, 22f, 74f, 10f, 50f); z() }
            val tail: Pen.PathB.() -> Unit = { m(70f, 50f); c(80f, 40f, 88f, 26f, 96f, 24f); q(88f, 50f, 96f, 76f); c(88f, 74f, 80f, 60f, 70f, 50f); z() }
            path(whiteFill = true, build = tail)
            clip(tail) { for (k in 0 until 9) line(70f, 50f, 100f, 18f + k * 8f) }
            for (fin in listOf<Pen.PathB.() -> Unit>({ m(34f, 34f); q(40f, 12f, 60f, 32f); z() }, { m(38f, 66f); q(44f, 84f, 58f, 68f); z() })) {
                path(whiteFill = true, build = fin); clip(fin) { for (k in 0 until 8) line(30f + k * 4f, 0f, 46f + k * 2f, 100f) }
            }
            path(whiteFill = true, build = body)
            clip(body) { scales(4f, 20f, 80f) }
            path(whiteFill = true) { m(10f, 50f); c(14f, 38f, 22f, 34f, 28f, 34f); q(34f, 50f, 28f, 66f); c(22f, 66f, 14f, 62f, 10f, 50f); z() }
            path(build = body)
            disc(20f, 46f, 3.5f); dot(21f, 46f, 1.7f)
            for ((x, y, r) in listOf(Triple(10f, 22f, 3f), Triple(16f, 12f, 2f), Triple(8f, 6f, 1.5f))) circle(x, y, r)
        } },
        "Cat" to { p -> p.run {
            line(0f, 94f, 100f, 94f)
            val tail: Pen.PathB.() -> Unit = { m(66f, 86f); c(92f, 86f, 94f, 56f, 82f, 44f); l(76f, 48f); c(84f, 60f, 82f, 78f, 66f, 78f); z() }
            path(whiteFill = true, build = tail)
            clip(tail) { for (k in 0 until 8) line(60f, 40f + k * 7f, 100f, 46f + k * 7f) }
            val body: Pen.PathB.() -> Unit = { m(50f, 94f); c(22f, 94f, 22f, 52f, 40f, 44f); l(60f, 44f); c(78f, 52f, 78f, 94f, 50f, 94f); z() }
            path(whiteFill = true, build = body)
            clip(body) { var y = 52f; var k = 0; while (y < 96f) { wavyH(y, 1.6f, 12f, k * 1.7f); y += 6f; k++ } }
            path(build = body)
            path(whiteFill = true) { m(32f, 30f); l(30f, 8f); l(46f, 20f); z() }
            path(whiteFill = true) { m(68f, 30f); l(70f, 8f); l(54f, 20f); z() }
            disc(50f, 32f, 17f)
            M(p, 50f, 22f).run { roundPetals(6, 1f, 7f, 2f) }
            for (ex in listOf(43f, 57f)) { path(whiteFill = true) { m(ex - 4f, 33f); q(ex, 28f, ex + 4f, 33f); q(ex, 37f, ex - 4f, 33f); z() }; dot(ex, 33f, 1.3f) }
            path(whiteFill = true) { m(48f, 39f); l(52f, 39f); l(50f, 41.5f); z() }
            path { m(50f, 41.5f); q(48f, 45f, 45f, 43f) }; path { m(50f, 41.5f); q(52f, 45f, 55f, 43f) }
        } },
        "Butterfly" to { p -> p.run {
            for (sx in listOf(1f, -1f)) {
                fun x(v: Float) = 50f + sx * (v - 50f)
                val up: Pen.PathB.() -> Unit = { m(50f, 46f); c(x(30f), 6f, x(2f), 10f, x(7f), 34f); c(x(10f), 50f, x(30f), 52f, 50f, 50f); z() }
                val lo: Pen.PathB.() -> Unit = { m(50f, 52f); c(x(30f), 54f, x(10f), 64f, x(18f), 84f); c(x(26f), 96f, x(46f), 80f, 50f, 58f); z() }
                path(whiteFill = true, build = up)
                clip(up) { for (k in 0 until 11) { val a = Math.toRadians(180.0 + k * 9.0); line(50f, 48f, 50f + sx * (cos(a) * 70).toFloat(), 48f + (sin(a) * 70).toFloat()) } }
                path(whiteFill = true, k = 0.55f, ox = x(30f), oy = 30f, build = up)
                for ((cx, cy) in listOf(x(30f) to 30f)) { circle(cx, cy, 4f); dot(cx, cy, 1.2f) }
                path(whiteFill = true, build = lo)
                path(k = 0.75f, ox = x(36f), oy = 68f, build = lo); path(k = 0.45f, ox = x(36f), oy = 68f, build = lo)
                for ((cx, cy) in listOf(x(24f) to 80f, x(14f) to 30f)) circle(cx, cy, 2.4f)
            }
            ovalDisc(46f, 34f, 54f, 82f)
            for (y in listOf(46f, 54f, 62f, 70f)) line(46.5f, y, 53.5f, y)
            disc(50f, 29f, 5.5f)
            path { m(48f, 24f); q(42f, 12f, 36f, 10f) }; path { m(52f, 24f); q(58f, 12f, 64f, 10f) }
            disc(36f, 10f, 2f); disc(64f, 10f, 2f)
        } },
    )

    // ======================================================================== Calm scenes
    val scenes: List<Art> = listOf(
        "Mountain lake" to { p -> p.run {
            circle(72f, 20f, 8f)
            disc(22f, 14f, 4f); disc(28f, 12f, 5f); disc(34f, 14f, 4f); line(22f, 18f, 34f, 18f)
            path(whiteFill = true) { m(0f, 60f); l(22f, 26f); l(36f, 44f); l(52f, 18f); l(72f, 50f); l(84f, 36f); l(100f, 56f); l(100f, 60f); z() }
            path { m(16.8f, 34f); l(20f, 37f); l(23f, 33f); l(26f, 36f); l(28.2f, 34f) }
            path { m(45.8f, 28f); l(49f, 31f); l(52f, 27f); l(55f, 30f); l(58.25f, 28f) }
            path { m(78.9f, 42f); l(82f, 45f); l(85f, 41f); l(88.8f, 42f) }
            line(0f, 60f, 100f, 60f)
            for ((y, ph) in listOf(67f to 0f, 75f to 2f, 84f to 4f, 93f to 1f)) wavyH(y, 1f, 18f, ph)
            for ((x, h) in listOf(6f to 16f, 12f to 12f, 92f to 14f)) pine(x, 60f, h)
            for ((x, y) in listOf(40f to 12f, 46f to 10f)) { path { m(x - 3f, y); q(x - 1.5f, y - 2f, x, y) }; path { m(x, y); q(x + 1.5f, y - 2f, x + 3f, y) } }
        } },
        "Sunset sea" to { p -> p.run {
            for (r in listOf(46f, 38f, 30f)) circle(50f, 58f, r)
            disc(50f, 58f, 22f)
            path(whiteFill = true) { m(-2f, 58f); l(102f, 58f); l(102f, 102f); l(-2f, 102f); z() }
            for ((y, ph) in listOf(64f to 0f, 71f to 2f, 79f to 4f, 88f to 1f, 96f to 3f)) wavyH(y, 1.2f, 16f, ph)
            path(whiteFill = true) { m(16f, 56f); l(34f, 56f); l(31f, 60f); l(19f, 60f); z() }
            path(whiteFill = true) { m(25f, 54f); l(25f, 38f); l(34f, 54f); z() }
            path(whiteFill = true) { m(24f, 54f); l(24f, 42f); l(17f, 54f); z() }
            for ((x, y) in listOf(66f to 18f, 72f to 14f, 78f to 20f)) { path { m(x - 3f, y); q(x - 1.5f, y - 2f, x, y) }; path { m(x, y); q(x + 1.5f, y - 2f, x + 3f, y) } }
        } },
        "Moonlit hills" to { p -> p.run {
            circle(70f, 22f, 13f); circle(66f, 18f, 2.6f); circle(74f, 26f, 3.2f); circle(73f, 15f, 1.6f)
            for ((x, y) in listOf(14f to 12f, 30f to 22f, 44f to 8f, 90f to 40f, 22f to 36f)) star4(x, y, 3f)
            path(whiteFill = true) { m(-2f, 60f); q(25f, 44f, 50f, 58f); q(75f, 72f, 102f, 52f); l(102f, 102f); l(-2f, 102f); z() }
            path(whiteFill = true) { m(-2f, 74f); q(30f, 60f, 60f, 76f); q(80f, 86f, 102f, 70f); l(102f, 102f); l(-2f, 102f); z() }
            rectDisc(64f, 62f, 76f, 72f); path(whiteFill = true) { m(62f, 62f); l(70f, 55f); l(78f, 62f); z() }; rect(68f, 64f, 72f, 68f)
            path(whiteFill = true) { m(-2f, 88f); q(40f, 78f, 102f, 92f); l(102f, 102f); l(-2f, 102f); z() }
            for ((x, y) in listOf(16f to 66f, 24f to 68f)) { line(x, y, x, y + 6f); disc(x, y - 2f, 3.5f) }
        } },
        "Pine forest" to { p -> p.run {
            circle(80f, 16f, 8f)
            path(whiteFill = true) { m(-2f, 58f); q(30f, 42f, 60f, 54f); q(80f, 60f, 102f, 46f); l(102f, 102f); l(-2f, 102f); z() }
            for ((x, h) in listOf(10f to 26f, 26f to 22f, 44f to 28f, 62f to 24f, 82f to 30f)) pine(x, 70f, h)
            line(-2f, 84f, 102f, 84f)
            for ((x, h) in listOf(4f to 34f, 20f to 40f, 38f to 36f, 56f to 42f, 74f to 38f, 92f to 40f)) pine(x, 92f, h)
            line(0f, 92f, 100f, 92f)
        } },
        "Desert dunes" to { p -> p.run {
            for (r in listOf(22f, 16f)) circle(62f, 34f, r)
            disc(62f, 34f, 10f)
            path(whiteFill = true) { m(-2f, 56f); q(30f, 40f, 60f, 58f); q(80f, 68f, 102f, 54f); l(102f, 102f); l(-2f, 102f); z() }
            path(whiteFill = true) { m(-2f, 74f); q(40f, 58f, 70f, 74f); q(90f, 84f, 102f, 76f); l(102f, 102f); l(-2f, 102f); z() }
            path(whiteFill = true) { m(-2f, 90f); q(50f, 80f, 102f, 92f); l(102f, 102f); l(-2f, 102f); z() }
            rectDisc(14f, 62f, 20f, 78f, 3f); rectDisc(14f, 74f, 25f, 78f, 2f)
            rectDisc(38f, 54f, 44f, 70f, 3f); rectDisc(31f, 66f, 44f, 70f, 2f)
            rectDisc(22f, 46f, 34f, 96f, 6f)
            for (x in listOf(25.5f, 28f, 30.5f)) line(x, 50f, x, 94f)
            for ((x, y) in listOf(12f to 20f, 18f to 16f)) { path { m(x - 3f, y); q(x - 1.5f, y - 2f, x, y) }; path { m(x, y); q(x + 1.5f, y - 2f, x + 3f, y) } }
        } },
        "Zen garden" to { p -> p.run {
            clip({ m(4f, 4f); l(96f, 4f); l(96f, 96f); l(4f, 96f); z() }) {
                var y = 6f; while (y < 98f) { line(0f, y, 100f, y); y += 4f }
                for ((cx, cy, rx, ry) in listOf(listOf(30f, 38f, 9f, 6f), listOf(66f, 64f, 11f, 7.5f), listOf(76f, 24f, 5f, 3.5f))) {
                    for (g in listOf(16f, 12f, 8f, 4f)) ovalDisc(cx - rx - g, cy - ry - g * 0.8f, cx + rx + g, cy + ry + g * 0.8f)
                    ovalDisc(cx - rx, cy - ry, cx + rx, cy + ry)
                    path { m(cx - rx * 0.4f, cy - ry * 0.5f); q(cx, cy, cx + rx * 0.3f, cy + ry * 0.6f) }
                }
            }
            rect(4f, 4f, 96f, 96f)
        } },
    )
}
