package com.bb.coloringbook

import android.graphics.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Small drawing helper for line art. All coordinates are in a 100 x 100 unit space
 * (or 100 x [h] for non-square pages) and are scaled to the bitmap size.
 */
class Pen(private val c: Canvas, private val s: Float, lineWidth: Float) {
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = Color.BLACK; strokeWidth = lineWidth * s
        strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.BLACK }
    private val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.WHITE }

    fun width(w: Float) { stroke.strokeWidth = w * s }

    fun circle(x: Float, y: Float, r: Float) = c.drawCircle(x * s, y * s, r * s, stroke)
    fun dot(x: Float, y: Float, r: Float) = c.drawCircle(x * s, y * s, r * s, fill)
    /** White-filled outlined circle: hides lines behind it. */
    fun disc(x: Float, y: Float, r: Float) { c.drawCircle(x * s, y * s, r * s, white); circle(x, y, r) }
    fun oval(l: Float, t: Float, r: Float, b: Float) = c.drawOval(l * s, t * s, r * s, b * s, stroke)
    fun ovalDisc(l: Float, t: Float, r: Float, b: Float) { c.drawOval(l * s, t * s, r * s, b * s, white); oval(l, t, r, b) }
    fun rect(l: Float, t: Float, r: Float, b: Float, round: Float = 0f) =
        c.drawRoundRect(l * s, t * s, r * s, b * s, round * s, round * s, stroke)
    fun rectDisc(l: Float, t: Float, r: Float, b: Float, round: Float = 0f) {
        c.drawRoundRect(l * s, t * s, r * s, b * s, round * s, round * s, white); rect(l, t, r, b, round)
    }
    fun line(x0: Float, y0: Float, x1: Float, y1: Float) = c.drawLine(x0 * s, y0 * s, x1 * s, y1 * s, stroke)
    fun arc(l: Float, t: Float, r: Float, b: Float, start: Float, sweep: Float) =
        c.drawArc(l * s, t * s, r * s, b * s, start, sweep, false, stroke)

    /** Path in units. [whiteFill] paints the inside white first so it covers lines behind. */
    fun path(whiteFill: Boolean = false, solid: Boolean = false, build: PathB.() -> Unit) {
        val b = PathB(s).apply(build)
        if (solid) { c.drawPath(b.p, fill); return }
        if (whiteFill) c.drawPath(b.p, white)
        c.drawPath(b.p, stroke)
    }

    /** Draw [block] clipped to the shape built by [shape] (for stripes/patterns inside a shape). */
    fun clip(shape: PathB.() -> Unit, block: () -> Unit) {
        c.save(); c.clipPath(PathB(s).apply(shape).p); block(); c.restore()
    }

    class PathB(private val s: Float) {
        val p = Path()
        fun m(x: Float, y: Float) = p.moveTo(x * s, y * s)
        fun l(x: Float, y: Float) = p.lineTo(x * s, y * s)
        fun q(cx: Float, cy: Float, x: Float, y: Float) = p.quadTo(cx * s, cy * s, x * s, y * s)
        fun c(x1: Float, y1: Float, x2: Float, y2: Float, x: Float, y: Float) =
            p.cubicTo(x1 * s, y1 * s, x2 * s, y2 * s, x * s, y * s)
        fun z() = p.close()
    }

    // ---- polar helpers for mandalas (centre 50,50) ----
    companion object {
        const val CX = 50f
        const val CY = 50f
        fun px(a: Double, r: Float) = CX + (cos(a) * r).toFloat()
        fun py(a: Double, r: Float) = CY + (sin(a) * r).toFloat()
        fun ang(i: Int, n: Int, phase: Double = 0.0) = 2 * PI * i / n + phase - PI / 2
    }
}
