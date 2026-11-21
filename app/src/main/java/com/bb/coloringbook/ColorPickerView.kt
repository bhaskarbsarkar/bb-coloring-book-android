package com.bb.coloringbook

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

/** HSV picker: saturation/value square on top, hue bar below. Supports any color. */
class ColorPickerView(context: Context, initial: Int) : View(context) {
    private val hsv = FloatArray(3).also { Color.colorToHSV(initial, it) }
    var onChange: ((Int) -> Unit)? = null
    val color get() = Color.HSVToColor(hsv)

    private val d = resources.displayMetrics.density
    private val gap = 16 * d
    private val hueH = 36 * d
    private val sv = RectF(); private val hue = RectF()
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3 * d }
    private var hueShader: Shader? = null

    fun setColor(c: Int) { Color.colorToHSV(c, hsv); invalidate() }

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val w = MeasureSpec.getSize(wSpec)
        setMeasuredDimension(w, min(w, (320 * d).toInt()) + (gap + hueH).toInt())
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        sv.set(0f, 0f, w.toFloat(), h - gap - hueH)
        hue.set(0f, h - hueH, w.toFloat(), h.toFloat())
        val colors = IntArray(7) { Color.HSVToColor(floatArrayOf(it * 60f % 360f, 1f, 1f)) }.also { it[6] = Color.RED }
        hueShader = LinearGradient(hue.left, 0f, hue.right, 0f, colors, null, Shader.TileMode.CLAMP)
    }

    override fun onDraw(c: Canvas) {
        val pure = Color.HSVToColor(floatArrayOf(hsv[0], 1f, 1f))
        val sat = LinearGradient(sv.left, 0f, sv.right, 0f, Color.WHITE, pure, Shader.TileMode.CLAMP)
        val value = LinearGradient(0f, sv.top, 0f, sv.bottom, Color.WHITE, Color.BLACK, Shader.TileMode.CLAMP)
        p.shader = ComposeShader(value, sat, PorterDuff.Mode.MULTIPLY)
        c.drawRoundRect(sv, 8 * d, 8 * d, p)
        p.shader = hueShader
        c.drawRoundRect(hue, 8 * d, 8 * d, p)
        p.shader = null

        val x = sv.left + hsv[1] * sv.width(); val y = sv.top + (1 - hsv[2]) * sv.height()
        ring.color = if (hsv[2] > 0.5f) Color.BLACK else Color.WHITE
        c.drawCircle(x, y, 10 * d, ring)
        val hx = hue.left + hsv[0] / 360f * hue.width()
        ring.color = Color.WHITE; c.drawRect(hx - 4 * d, hue.top, hx + 4 * d, hue.bottom, ring)
    }

    private var dragHue = false
    override fun onTouchEvent(e: MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(true)
        if (e.actionMasked == MotionEvent.ACTION_DOWN) dragHue = e.y > sv.bottom + gap / 2
        if (dragHue) {
            hsv[0] = ((e.x - hue.left) / hue.width()).coerceIn(0f, 0.999f) * 360f
        } else {
            hsv[1] = ((e.x - sv.left) / sv.width()).coerceIn(0f, 1f)
            hsv[2] = 1f - ((e.y - sv.top) / sv.height()).coerceIn(0f, 1f)
        }
        invalidate()
        onChange?.invoke(color)
        return true
    }
}
