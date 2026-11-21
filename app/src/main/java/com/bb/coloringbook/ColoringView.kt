package com.bb.coloringbook

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

enum class Tool { BRUSH, FILL, ERASER, PICKER }

/**
 * Canvas for coloring a line-art image.
 *
 * Layers: [paintLayer] (the colors, opaque, starts white) is drawn first, then [lineArt]
 * is multiplied on top, so color always sits *under* the outlines like real ink.
 *
 * Input: stylus draws (with pressure), fingers pan/zoom. When [penOnly] is false a single
 * finger also draws. The stylus side button or eraser tip erases.
 */
class ColoringView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    // ---- public state -------------------------------------------------------------------
    var tool = Tool.BRUSH
    var color = Color.parseColor("#E53935")
    /** Brush diameter in screen pixels at full pressure. */
    var brushSize = 24f
    /** Clip brush strokes to the region where the stroke started. */
    var stayInside = true
    /** Fingers never draw; only stylus draws. Auto-enabled when a stylus is first seen. */
    var penOnly = false
    var listener: Listener? = null

    interface Listener {
        fun onColorPicked(color: Int) {}
        fun onStylusDetected() {}
        fun onHistoryChanged(canUndo: Boolean, canRedo: Boolean) {}
    }

    // ---- image layers -------------------------------------------------------------------
    var lineArt: Bitmap? = null; private set
    var paintLayer: Bitmap? = null; private set
    private var paintCanvas: Canvas? = null
    private var scratch: Bitmap? = null
    private var scratchCanvas: Canvas? = null
    private var regions: RegionFinder? = null
    private val history = History()

    // ---- view transform -----------------------------------------------------------------
    private val viewMatrix = Matrix()
    private val inverse = Matrix()
    private var userMoved = false

    // ---- paints -------------------------------------------------------------------------
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val multiplyPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply { blendMode = BlendMode.MULTIPLY }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val dstInPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
    private val clearPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
    private val cursorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f }
    private val shadowPaint = Paint().apply { color = 0x33000000 }

    init {
        setBackgroundColor(Color.parseColor("#E8E4EC"))
        isFocusable = true
    }

    // =====================================================================================
    // Image loading / export
    // =====================================================================================

    /** Set a new picture. [paint] restores previously saved colors (same size) if given. */
    fun setImage(src: Bitmap, paint: Bitmap? = null) {
        val w = src.width; val h = src.height
        val art = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(art).apply { drawColor(Color.WHITE); drawBitmap(src, 0f, 0f, null) }
        lineArt = art

        val pl = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pc = Canvas(pl)
        pc.drawColor(Color.WHITE)
        if (paint != null && paint.width == w && paint.height == h) pc.drawBitmap(paint, 0f, 0f, null)
        paintLayer = pl; paintCanvas = pc

        scratch = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { scratchCanvas = Canvas(it) }

        val px = IntArray(w * h)
        art.getPixels(px, 0, w, 0, 0, w, h)
        regions = RegionFinder(w, h, RegionFinder.linesFromArgb(px))

        history.reset(pl)
        notifyHistory()
        userMoved = false
        fitToView()
        invalidate()
    }

    fun hasImage() = lineArt != null

    /** Final picture: colors with outlines multiplied on top. */
    fun renderResult(): Bitmap? {
        val art = lineArt ?: return null
        val out = paintLayer!!.copy(Bitmap.Config.ARGB_8888, true)
        Canvas(out).drawBitmap(art, 0f, 0f, multiplyPaint)
        return out
    }

    fun clearColors() {
        val pc = paintCanvas ?: return
        pc.drawColor(Color.WHITE)
        commit(Rect(0, 0, paintLayer!!.width, paintLayer!!.height))
        invalidate()
    }

    // =====================================================================================
    // Undo / redo
    // =====================================================================================

    fun canUndo() = history.canUndo()
    fun canRedo() = history.canRedo()
    fun undo() { if (history.undo(paintLayer!!)) { notifyHistory(); invalidate() } }
    fun redo() { if (history.redo(paintLayer!!)) { notifyHistory(); invalidate() } }
    private fun commit(r: Rect) { history.commit(paintLayer!!, r); notifyHistory() }
    private fun notifyHistory() = listener?.onHistoryChanged(history.canUndo(), history.canRedo())

    // =====================================================================================
    // View transform
    // =====================================================================================

    fun fitToView() {
        val art = lineArt ?: return
        if (width == 0 || height == 0) return
        val pad = 16f * resources.displayMetrics.density
        val s = min((width - 2 * pad) / art.width, (height - 2 * pad) / art.height)
        viewMatrix.reset()
        viewMatrix.postScale(s, s)
        viewMatrix.postTranslate((width - art.width * s) / 2f, (height - art.height * s) / 2f)
        viewMatrix.invert(inverse)
        userMoved = false
        invalidate()
    }

    private fun currentScale(): Float { val v = FloatArray(9); viewMatrix.getValues(v); return v[Matrix.MSCALE_X] }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (!userMoved) fitToView()
    }

    /** Image pixel -> view coordinates. */
    fun imageToView(x: Float, y: Float): PointF {
        val p = floatArrayOf(x, y); viewMatrix.mapPoints(p); return PointF(p[0], p[1])
    }

    private fun toImage(x: Float, y: Float): PointF {
        val p = floatArrayOf(x, y); inverse.mapPoints(p); return PointF(p[0], p[1])
    }

    // =====================================================================================
    // Drawing
    // =====================================================================================

    private var hoverX = -1f; private var hoverY = -1f

    override fun onDraw(canvas: Canvas) {
        val art = lineArt ?: return
        canvas.save()
        canvas.concat(viewMatrix)
        val s = currentScale()
        canvas.drawRect(4f / s, 4f / s, art.width + 4f / s, art.height + 4f / s, shadowPaint)
        canvas.drawBitmap(paintLayer!!, 0f, 0f, bitmapPaint)
        canvas.drawBitmap(art, 0f, 0f, multiplyPaint)
        canvas.restore()

        if (hoverX >= 0 && (tool == Tool.BRUSH || tool == Tool.ERASER)) {
            val r = max(1.5f, brushSize / 2f)
            cursorPaint.color = Color.WHITE; cursorPaint.strokeWidth = 3f
            canvas.drawCircle(hoverX, hoverY, r, cursorPaint)
            cursorPaint.color = if (tool == Tool.ERASER) Color.DKGRAY else color; cursorPaint.strokeWidth = 1.5f
            canvas.drawCircle(hoverX, hoverY, r, cursorPaint)
        }
    }

    // =====================================================================================
    // Input
    // =====================================================================================

    private enum class Mode { NONE, STROKE, GESTURE }
    private var mode = Mode.NONE
    private var strokePointerId = -1
    private var strokeIsFinger = false

    // gesture tracking
    private var lastMidX = 0f; private var lastMidY = 0f; private var lastDist = 0f
    private var gesturePointers = 0

    override fun onHoverEvent(e: MotionEvent): Boolean {
        if (isStylus(e, 0)) {
            noteStylus()
            if (e.actionMasked == MotionEvent.ACTION_HOVER_EXIT) { hoverX = -1f } else { hoverX = e.x; hoverY = e.y }
            invalidate()
            return true
        }
        return super.onHoverEvent(e)
    }

    private fun isStylus(e: MotionEvent, i: Int): Boolean {
        val t = e.getToolType(i)
        return t == MotionEvent.TOOL_TYPE_STYLUS || t == MotionEvent.TOOL_TYPE_ERASER
    }

    private fun noteStylus() {
        if (!penOnly) { penOnly = true; listener?.onStylusDetected() }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (lineArt == null) return false
        parent?.requestDisallowInterceptTouchEvent(true)

        // ---- stylus: always draws, fingers are ignored meanwhile (palm rejection) ----
        val stylusIndex = (0 until e.pointerCount).firstOrNull { isStylus(e, it) }
        if (stylusIndex != null || (mode == Mode.STROKE && !strokeIsFinger)) {
            if (stylusIndex != null) noteStylus()
            handleStylus(e, stylusIndex)
            return true
        }

        // ---- fingers ----
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!penOnly) {
                    strokeIsFinger = true
                    startStroke(e, 0)
                } else beginGesture(e)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (mode == Mode.STROKE && strokeIsFinger) cancelStroke()
                beginGesture(e)
            }
            MotionEvent.ACTION_MOVE -> when (mode) {
                Mode.STROKE -> moveStroke(e)
                Mode.GESTURE -> updateGesture(e)
                else -> {}
            }
            MotionEvent.ACTION_POINTER_UP -> if (mode == Mode.GESTURE) beginGesture(e, e.actionIndex)
            MotionEvent.ACTION_UP -> { if (mode == Mode.STROKE) endStroke() ; mode = Mode.NONE }
            MotionEvent.ACTION_CANCEL -> { if (mode == Mode.STROKE) cancelStroke(); mode = Mode.NONE }
        }
        return true
    }

    private fun handleStylus(e: MotionEvent, idx: Int?) {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (idx != null && e.actionIndex == idx) {
                    if (mode == Mode.STROKE && strokeIsFinger) cancelStroke()
                    strokeIsFinger = false
                    hoverX = -1f
                    startStroke(e, idx)
                }
            }
            MotionEvent.ACTION_MOVE -> if (mode == Mode.STROKE) moveStroke(e)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (mode == Mode.STROKE && e.getPointerId(e.actionIndex) == strokePointerId) {
                    endStroke(); mode = Mode.NONE
                }
            }
            MotionEvent.ACTION_CANCEL -> { if (mode == Mode.STROKE) cancelStroke(); mode = Mode.NONE }
        }
    }

    // ---- gestures (pan / pinch-zoom) ----
    private fun beginGesture(e: MotionEvent, skipIndex: Int = -1) {
        mode = Mode.GESTURE
        var sx = 0f; var sy = 0f; var n = 0
        for (i in 0 until e.pointerCount) if (i != skipIndex && !isStylus(e, i)) { sx += e.getX(i); sy += e.getY(i); n++ }
        if (n == 0) { mode = Mode.NONE; return }
        lastMidX = sx / n; lastMidY = sy / n
        lastDist = spread(e, skipIndex, lastMidX, lastMidY)
        gesturePointers = n
    }

    private fun spread(e: MotionEvent, skip: Int, mx: Float, my: Float): Float {
        var d = 0f; var n = 0
        for (i in 0 until e.pointerCount) if (i != skip && !isStylus(e, i)) { d += hypot(e.getX(i) - mx, e.getY(i) - my); n++ }
        return if (n > 1) d / n else 0f
    }

    private fun updateGesture(e: MotionEvent) {
        var sx = 0f; var sy = 0f; var n = 0
        for (i in 0 until e.pointerCount) if (!isStylus(e, i)) { sx += e.getX(i); sy += e.getY(i); n++ }
        if (n == 0) return
        if (n != gesturePointers) { beginGesture(e); return }
        val mx = sx / n; val my = sy / n
        viewMatrix.postTranslate(mx - lastMidX, my - lastMidY)
        val d = spread(e, -1, mx, my)
        if (n > 1 && lastDist > 10f && d > 10f) {
            val art = lineArt!!
            val fit = min(width.toFloat() / art.width, height.toFloat() / art.height)
            val cur = currentScale()
            val f = (d / lastDist).coerceIn(fit * 0.5f / cur, fit * 30f / cur)
            viewMatrix.postScale(f, f, mx, my)
        }
        lastMidX = mx; lastMidY = my; lastDist = d
        viewMatrix.invert(inverse)
        userMoved = true
        invalidate()
    }

    // =====================================================================================
    // Strokes, fill, picker
    // =====================================================================================

    private var strokeTool = Tool.BRUSH
    private var clip: RegionFinder.Region? = null
    private var clipBitmap: Bitmap? = null
    private var lastX = 0f; private var lastY = 0f; private var lastW = 0f
    private val dirty = Rect()

    private fun pressureWidth(e: MotionEvent, i: Int, hist: Int = -1): Float {
        val p = if (hist >= 0) e.getHistoricalPressure(i, hist) else e.getPressure(i)
        val isFinger = !isStylus(e, i)
        val k = if (isFinger) 1f else (0.25f + 0.75f * p.coerceIn(0f, 1f))
        return max(0.5f, brushSize * k / currentScale())
    }

    private fun startStroke(e: MotionEvent, i: Int) {
        strokePointerId = e.getPointerId(i)
        val pt = toImage(e.getX(i), e.getY(i))
        val eraserHeld = e.getToolType(i) == MotionEvent.TOOL_TYPE_ERASER ||
                (e.buttonState and (MotionEvent.BUTTON_STYLUS_PRIMARY or MotionEvent.BUTTON_SECONDARY)) != 0
        strokeTool = if (eraserHeld) Tool.ERASER else tool

        when (strokeTool) {
            Tool.FILL -> { fillAt(pt.x.toInt(), pt.y.toInt()); mode = Mode.NONE; return }
            Tool.PICKER -> { pickAt(pt.x.toInt(), pt.y.toInt()); mode = Mode.NONE; return }
            else -> {}
        }

        mode = Mode.STROKE
        clip = null; clipBitmap = null
        if (stayInside) {
            val r = regions!!.find(pt.x.toInt(), pt.y.toInt()) ?: nearestRegion(pt.x.toInt(), pt.y.toInt())
            if (r != null) {
                clip = r
                val colors = IntArray(r.mask.size) { if (r.mask[it].toInt() != 0) Color.BLACK else 0 }
                clipBitmap = Bitmap.createBitmap(colors, r.w, r.h, Bitmap.Config.ARGB_8888)
            }
        }
        strokePaint.color = if (strokeTool == Tool.ERASER) Color.WHITE else color
        lastX = pt.x; lastY = pt.y; lastW = pressureWidth(e, i)
        dirty.setEmpty()
        segment(lastX, lastY, lastW, lastX, lastY, lastW)
    }

    /** Stroke started on an outline: use the region under the nearest non-line pixel. */
    private fun nearestRegion(x: Int, y: Int): RegionFinder.Region? {
        for (rad in 1..6) for (dy in -rad..rad) for (dx in -rad..rad) {
            if (max(abs(dx), abs(dy)) != rad) continue
            regions!!.find(x + dx, y + dy)?.let { return it }
        }
        return null
    }

    private fun moveStroke(e: MotionEvent) {
        val i = e.findPointerIndex(strokePointerId)
        if (i < 0) return
        for (h in 0 until e.historySize) {
            val p = toImage(e.getHistoricalX(i, h), e.getHistoricalY(i, h))
            val w = pressureWidth(e, i, h)
            segment(lastX, lastY, lastW, p.x, p.y, w)
            lastX = p.x; lastY = p.y; lastW = w
        }
        val p = toImage(e.getX(i), e.getY(i))
        val w = pressureWidth(e, i)
        segment(lastX, lastY, lastW, p.x, p.y, w)
        lastX = p.x; lastY = p.y; lastW = w
        invalidate()
    }

    /** Draws one segment with width interpolated from w0 to w1. */
    private fun segment(x0: Float, y0: Float, w0: Float, x1: Float, y1: Float, w1: Float) {
        val pl = paintLayer ?: return
        val len = hypot(x1 - x0, y1 - y0)
        val steps = max(1, ceil(abs(w1 - w0) / 0.75f).toInt().coerceAtMost(max(1, (len / 0.5f).toInt())))
        val maxW = max(w0, w1)
        val r = Rect(
            floor(min(x0, x1) - maxW).toInt(), floor(min(y0, y1) - maxW).toInt(),
            ceil(max(x0, x1) + maxW).toInt() + 1, ceil(max(y0, y1) + maxW).toInt() + 1
        )
        if (!r.intersect(0, 0, pl.width, pl.height)) return

        val c = clip
        val target = if (c != null) scratchCanvas!! else paintCanvas!!
        if (c != null) {
            if (!r.intersect(c.left, c.top, c.right, c.bottom)) return
            target.save(); target.clipRect(r)
        }
        for (s in 0 until steps) {
            val t0 = s / steps.toFloat(); val t1 = (s + 1) / steps.toFloat()
            strokePaint.strokeWidth = w0 + (w1 - w0) * (t0 + t1) / 2f
            target.drawLine(x0 + (x1 - x0) * t0, y0 + (y1 - y0) * t0, x0 + (x1 - x0) * t1, y0 + (y1 - y0) * t1, strokePaint)
        }
        if (c != null) {
            target.drawBitmap(clipBitmap!!, c.left.toFloat(), c.top.toFloat(), dstInPaint)
            target.restore()
            paintCanvas!!.drawBitmap(scratch!!, r, r, null)
            target.drawRect(r, clearPaint)
        }
        dirty.union(r)
    }

    private fun endStroke() {
        if (!dirty.isEmpty) commit(Rect(dirty))
        clip = null; clipBitmap = null
        invalidate()
    }

    private fun cancelStroke() {
        if (!dirty.isEmpty) history.revert(paintLayer!!, dirty)
        dirty.setEmpty(); clip = null; clipBitmap = null; mode = Mode.NONE
        invalidate()
    }

    fun fillAt(x: Int, y: Int, fillColor: Int = color): Boolean {
        val pl = paintLayer ?: return false
        val r = regions!!.find(x, y) ?: return false
        val px = IntArray(r.w * r.h)
        pl.getPixels(px, 0, r.w, r.left, r.top, r.w, r.h)
        for (i in px.indices) if (r.mask[i].toInt() != 0) px[i] = fillColor
        pl.setPixels(px, 0, r.w, r.left, r.top, r.w, r.h)
        commit(Rect(r.left, r.top, r.right, r.bottom))
        invalidate()
        return true
    }

    private fun pickAt(x: Int, y: Int) {
        val pl = paintLayer ?: return
        if (x !in 0 until pl.width || y !in 0 until pl.height) return
        val c = pl.getPixel(x, y) or 0xFF000000.toInt()
        color = c
        listener?.onColorPicked(c)
    }

    /** Programmatic stroke for tests: points in image coordinates, constant width. */
    fun strokeForTest(points: List<PointF>, widthPx: Float) {
        val e = points.first()
        val r = if (stayInside) regions!!.find(e.x.toInt(), e.y.toInt()) else null
        clip = r
        clipBitmap = r?.let { reg ->
            Bitmap.createBitmap(IntArray(reg.mask.size) { if (reg.mask[it].toInt() != 0) Color.BLACK else 0 }, reg.w, reg.h, Bitmap.Config.ARGB_8888)
        }
        strokePaint.color = if (tool == Tool.ERASER) Color.WHITE else color
        dirty.setEmpty()
        var prev = e
        for (p in points) { segment(prev.x, prev.y, widthPx, p.x, p.y, widthPx); prev = p }
        endStroke()
    }
}
