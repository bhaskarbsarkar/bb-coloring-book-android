package com.bb.coloringbook

import com.bb.coloringbook.Pen.Companion.ang
import com.bb.coloringbook.Pen.Companion.px
import com.bb.coloringbook.Pen.Companion.py
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Ring-shape building blocks for mandalas, in 100x100 units centred at (50,50). */
private class M(val p: Pen) {
    fun ring(r: Float) = p.circle(50f, 50f, r)

    /** Pointed lens petals between radii r0..r1. */
    fun petals(n: Int, r0: Float, r1: Float, w: Float, phase: Double = 0.0) {
        for (i in 0 until n) {
            val a = ang(i, n, phase); val na = a + PI / 2; val mr = (r0 + r1) / 2
            p.path {
                m(px(a, r0), py(a, r0))
                q(px(a, mr) + (cos(na) * w).toFloat(), py(a, mr) + (sin(na) * w).toFloat(), px(a, r1), py(a, r1))
                q(px(a, mr) - (cos(na) * w).toFloat(), py(a, mr) - (sin(na) * w).toFloat(), px(a, r0), py(a, r0))
            }
        }
    }

    /** Petals with a round tip (like a lotus / daisy). */
    fun roundPetals(n: Int, r0: Float, r1: Float, w: Float, phase: Double = 0.0, white: Boolean = false) {
        for (i in 0 until n) {
            val a = ang(i, n, phase); val na = a + PI / 2
            val ox = (cos(na) * w).toFloat(); val oy = (sin(na) * w).toFloat()
            val bx = px(a, r0); val by = py(a, r0); val tx = px(a, r1); val ty = py(a, r1)
            val k = (r1 - r0) * 0.35f
            p.path(whiteFill = white) {
                m(bx, by)
                c(px(a, r0 + k) + ox, py(a, r0 + k) + oy, px(a, r1 + k * 0.3f) + ox, py(a, r1 + k * 0.3f) + oy, tx, ty)
                c(px(a, r1 + k * 0.3f) - ox, py(a, r1 + k * 0.3f) - oy, px(a, r0 + k) - ox, py(a, r0 + k) - oy, bx, by)
                z()
            }
        }
    }

    /** Zig-zag star band between r0 and r1. */
    fun spikes(n: Int, r0: Float, r1: Float, phase: Double = 0.0) {
        p.path {
            for (i in 0..2 * n) {
                val a = ang(i, 2 * n, phase); val r = if (i % 2 == 0) r0 else r1
                if (i == 0) m(px(a, r), py(a, r)) else l(px(a, r), py(a, r))
            }
            z()
        }
    }

    /** Ring of touching circles (scallops). */
    fun scallops(n: Int, r: Float, size: Float, phase: Double = 0.0) {
        for (i in 0 until n) { val a = ang(i, n, phase); p.circle(px(a, r), py(a, r), size) }
    }

    fun dots(n: Int, r: Float, size: Float, phase: Double = 0.0) {
        for (i in 0 until n) { val a = ang(i, n, phase); p.dot(px(a, r), py(a, r), size) }
    }

    fun polygon(n: Int, r: Float, phase: Double = 0.0) {
        p.path {
            for (i in 0..n) { val a = ang(i, n, phase); if (i == 0) m(px(a, r), py(a, r)) else l(px(a, r), py(a, r)) }
            z()
        }
    }

    /** Wavy closed ring. */
    fun wave(n: Int, r: Float, amp: Float, phase: Double = 0.0) {
        p.path {
            val steps = n * 16
            for (i in 0..steps) {
                val a = 2 * PI * i / steps - PI / 2
                val rr = r + amp * sin(n * a + phase).toFloat()
                if (i == 0) m(px(a, rr), py(a, rr)) else l(px(a, rr), py(a, rr))
            }
            z()
        }
    }

    fun rays(n: Int, r0: Float, r1: Float, phase: Double = 0.0) {
        for (i in 0 until n) { val a = ang(i, n, phase); p.line(px(a, r0), py(a, r0), px(a, r1), py(a, r1)) }
    }

    /** Hearts pointing to the centre. */
    fun hearts(n: Int, r: Float, size: Float, phase: Double = 0.0) {
        for (i in 0 until n) {
            val a = ang(i, n, phase)
            // local frame: u = outward, v = sideways
            fun x(u: Float, v: Float) = px(a, r + u * size) + (cos(a + PI / 2) * v * size).toFloat()
            fun y(u: Float, v: Float) = py(a, r + u * size) + (sin(a + PI / 2) * v * size).toFloat()
            p.path {
                m(x(-1f, 0f), y(-1f, 0f))
                c(x(-0.2f, 1.1f), y(-0.2f, 1.1f), x(1.1f, 1.0f), y(1.1f, 1.0f), x(0.55f, 0f), y(0.55f, 0f))
                c(x(1.1f, -1.0f), y(1.1f, -1.0f), x(-0.2f, -1.1f), y(-0.2f, -1.1f), x(-1f, 0f), y(-1f, 0f))
                z()
            }
        }
    }

    /** Leaves with a centre vein. */
    fun leaves(n: Int, r0: Float, r1: Float, w: Float, phase: Double = 0.0) {
        petals(n, r0, r1, w, phase)
        for (i in 0 until n) {
            val a = ang(i, n, phase)
            p.line(px(a, r0 + 1f), py(a, r0 + 1f), px(a, r1 - 1.5f), py(a, r1 - 1.5f))
        }
    }

    /** Teardrops: circle bulb on the outside, point inward. */
    fun drops(n: Int, r0: Float, r1: Float, w: Float, phase: Double = 0.0) {
        for (i in 0 until n) {
            val a = ang(i, n, phase); val na = a + PI / 2
            val ox = (cos(na) * w).toFloat(); val oy = (sin(na) * w).toFloat()
            p.path {
                m(px(a, r0), py(a, r0))
                c(px(a, r0 + (r1 - r0) * 0.5f) + ox, py(a, r0 + (r1 - r0) * 0.5f) + oy, px(a, r1) + ox * 1.2f, py(a, r1) + oy * 1.2f, px(a, r1), py(a, r1))
                c(px(a, r1) - ox * 1.2f, py(a, r1) - oy * 1.2f, px(a, r0 + (r1 - r0) * 0.5f) - ox, py(a, r0 + (r1 - r0) * 0.5f) - oy, px(a, r0), py(a, r0))
                z()
            }
        }
    }
}

/** Ten mandala styles. */
object Mandalas {
    val all: List<Pair<String, (Pen) -> Unit>> = listOf(
        "Lotus" to { p -> M(p).run {
            ring(46f); ring(6f)
            roundPetals(8, 6f, 20f, 5f)
            roundPetals(8, 6f, 16f, 3.5f, PI / 8)
            ring(22f)
            roundPetals(12, 22f, 34f, 5.5f); roundPetals(12, 22f, 30f, 3f, PI / 12)
            ring(36f)
            petals(24, 36f, 46f, 2.2f)
        } },
        "Star burst" to { p -> M(p).run {
            ring(46f); ring(4f)
            spikes(8, 4f, 14f); ring(16f)
            spikes(12, 16f, 26f, PI / 12); ring(28f)
            spikes(16, 28f, 38f); ring(40f)
            spikes(24, 40f, 46f, PI / 24)
        } },
        "Sunshine" to { p -> M(p).run {
            ring(46f); ring(12f); ring(8f)
            scallops(10, 10f, 2f)
            ring(20f); rays(20, 12f, 20f)
            drops(12, 20f, 33f, 4f)
            ring(35f)
            scallops(24, 40.5f, 5.3f)
        } },
        "Scallop flower" to { p -> M(p).run {
            ring(46f)
            scallops(6, 6f, 6f); ring(3f)
            ring(13f)
            scallops(12, 17f, 4.4f); ring(21.5f)
            scallops(18, 26f, 4.6f, PI / 18); ring(30.5f)
            scallops(24, 35f, 4.6f); ring(39.5f)
            dots(24, 43f, 1.2f, PI / 24)
        } },
        "Geometric" to { p -> M(p).run {
            ring(46f)
            polygon(4, 9f); polygon(4, 9f, PI / 4)
            ring(13f)
            polygon(4, 26f); polygon(4, 26f, PI / 4); polygon(4, 26f, PI / 8); polygon(4, 26f, 3 * PI / 8)
            ring(28f)
            spikes(16, 28f, 36f); ring(36f)
            scallops(32, 41f, 4.2f)
        } },
        "Snowflake" to { p -> M(p).run {
            ring(46f); polygon(6, 5f)
            for (i in 0 until 6) {
                val a = ang(i, 6); val q = p
                q.line(px(a, 5f), py(a, 5f), px(a, 40f), py(a, 40f))
                for ((r, len) in listOf(14f to 6f, 24f to 8f, 33f to 6f)) {
                    for (s in listOf(-1, 1)) {
                        val b = a + s * PI / 4
                        q.line(px(a, r), py(a, r), px(a, r) + (cos(b) * len).toFloat(), py(a, r) + (sin(b) * len).toFloat())
                    }
                }
            }
            ring(20f)
            polygon(6, 40f, PI / 6)
            scallops(6, 42f, 3.5f)
        } },
        "Hearts" to { p -> M(p).run {
            ring(46f); ring(5f)
            hearts(6, 11f, 5f)
            ring(18f)
            hearts(10, 24.5f, 5.2f, PI / 10)
            ring(31f)
            hearts(14, 37.5f, 5.4f)
            ring(43.5f)
        } },
        "Leafy" to { p -> M(p).run {
            ring(46f); ring(5f)
            leaves(6, 5f, 17f, 4f)
            ring(19f)
            leaves(12, 19f, 31f, 3.6f, PI / 12)
            ring(33f)
            leaves(20, 33f, 45f, 2.6f)
        } },
        "Waves" to { p -> M(p).run {
            ring(46f); ring(5f)
            wave(6, 11f, 2.5f)
            wave(9, 20f, 2.5f)
            scallops(12, 20f, 2.2f)
            wave(12, 29f, 2.3f)
            wave(16, 37f, 2.2f, PI)
            dots(16, 42.5f, 1.4f)
        } },
        "Classic" to { p -> M(p).run {
            ring(46f); ring(37.7f); ring(28.5f); ring(18.4f); ring(10f); ring(4.6f)
            petals(8, 4.6f, 18.4f, 5.5f)
            petals(8, 4.6f, 10f, 2.3f, PI / 8)
            petals(16, 18.4f, 28.5f, 3.2f)
            petals(16, 28.5f, 37.7f, 2.8f, PI / 16)
            petals(32, 37.7f, 46f, 1.8f)
            scallops(16, 23.5f, 1.6f, PI / 16)
        } },
    )
}
