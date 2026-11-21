package com.bb.coloringbook

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1280dp-h800dp-land-xhdpi")
class ColoringAppTest {
    private lateinit var activity: MainActivity
    private lateinit var view: ColoringView

    // Sample mandala is 1600x1600, centre (800,800). Inner circle radius ~73px.
    private val center = 800f to 800f
    private val outsideCenter = 800f to 700f   // just outside inner circle, inside a petal

    @Before fun setUp() {
        activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        Thread.sleep(300); shadowOf(Looper.getMainLooper()).idle()
        view = find(activity.window.decorView)!!
        view.setImage(SampleArt.mandala())
        view.layout(0, 0, view.width.coerceAtLeast(2000), view.height.coerceAtLeast(1100))
        view.fitToView()
    }

    private fun find(v: View): ColoringView? {
        if (v is ColoringView) return v
        if (v is android.view.ViewGroup) for (i in 0 until v.childCount) find(v.getChildAt(i))?.let { return it }
        return null
    }

    private fun px(p: Pair<Float, Float>) = view.paintLayer!!.getPixel(p.first.toInt(), p.second.toInt())

    private fun event(action: Int, pts: List<Pair<Float, Float>>, tool: Int, pressure: Float = 0.8f, buttons: Int = 0): MotionEvent {
        val props = Array(pts.size) { i -> MotionEvent.PointerProperties().apply { id = i; toolType = tool } }
        val coords = Array(pts.size) { i ->
            val s = view.imageToView(pts[i].first, pts[i].second)
            MotionEvent.PointerCoords().apply { x = s.x; y = s.y; this.pressure = pressure; size = 0.1f }
        }
        val t = SystemClock.uptimeMillis()
        return MotionEvent.obtain(t, t, action, pts.size, props, coords, 0, buttons, 1f, 1f, 0, 0, 0, 0)
    }

    private fun drag(path: List<Pair<Float, Float>>, tool: Int, buttons: Int = 0) {
        view.dispatchTouchEvent(event(MotionEvent.ACTION_DOWN, listOf(path.first()), tool, buttons = buttons))
        path.drop(1).forEach { view.dispatchTouchEvent(event(MotionEvent.ACTION_MOVE, listOf(it), tool, buttons = buttons)) }
        view.dispatchTouchEvent(event(MotionEvent.ACTION_UP, listOf(path.last()), tool, buttons = buttons))
    }

    private fun line(a: Pair<Float, Float>, b: Pair<Float, Float>, n: Int = 40) =
        (0..n).map { a.first + (b.first - a.first) * it / n to a.second + (b.second - a.second) * it / n }

    private fun screenshot(name: String) {
        val root = activity.window.decorView
        val b = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(b))
        File("build/screenshots").mkdirs()
        File("build/screenshots/$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
        view.renderResult()!!.let { r -> File("build/screenshots/$name-result.png").outputStream().use { r.compress(Bitmap.CompressFormat.PNG, 100, it) } }
    }

    @Test fun stylusFillColorsOnlyTheTappedRegion() {
        view.tool = Tool.FILL; view.color = Color.BLUE
        drag(listOf(center), MotionEvent.TOOL_TYPE_STYLUS)
        assertEquals(Color.BLUE, px(center))
        assertEquals(Color.WHITE, px(outsideCenter))
        assertTrue(view.penOnly) // stylus detected
        assertTrue(view.canUndo())
        view.undo(); assertEquals(Color.WHITE, px(center))
        view.redo(); assertEquals(Color.BLUE, px(center))
    }

    @Test fun stylusBrushStaysInsideLines() {
        view.tool = Tool.BRUSH; view.color = Color.RED; view.brushSize = 30f; view.stayInside = true
        drag(line(center, 800f to 1100f), MotionEvent.TOOL_TYPE_STYLUS)
        assertEquals(Color.RED, px(center))
        assertEquals(Color.WHITE, px(800f to 1000f))  // beyond the circle: clipped
    }

    @Test fun brushCrossesLinesWhenInsideModeOff() {
        view.tool = Tool.BRUSH; view.color = Color.RED; view.brushSize = 30f; view.stayInside = false
        drag(line(center, 800f to 1100f), MotionEvent.TOOL_TYPE_STYLUS)
        assertEquals(Color.RED, px(800f to 1000f))
    }

    @Test fun stylusButtonErases() {
        view.tool = Tool.FILL; view.color = Color.GREEN
        drag(listOf(center), MotionEvent.TOOL_TYPE_STYLUS)
        view.tool = Tool.BRUSH; view.brushSize = 30f
        drag(line(center, 810f to 800f), MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.BUTTON_STYLUS_PRIMARY)
        assertEquals(Color.WHITE, px(center))
    }

    @Test fun fingersDoNotDrawInPenOnlyMode() {
        view.penOnly = true; view.tool = Tool.BRUSH; view.color = Color.RED
        drag(line(center, 800f to 760f), MotionEvent.TOOL_TYPE_FINGER)
        assertEquals(Color.WHITE, px(center))
        assertFalse(view.canUndo())
    }

    @Test fun fingerDrawsWhenPenOnlyOff() {
        view.penOnly = false; view.tool = Tool.BRUSH; view.color = Color.RED; view.brushSize = 30f
        drag(line(center, 800f to 760f), MotionEvent.TOOL_TYPE_FINGER)
        assertEquals(Color.RED, px(center))
    }

    @Test fun pickerTakesColorFromCanvas() {
        view.tool = Tool.FILL; view.color = 0xFF123456.toInt()
        drag(listOf(center), MotionEvent.TOOL_TYPE_STYLUS)
        view.color = Color.BLACK; view.tool = Tool.PICKER
        drag(listOf(center), MotionEvent.TOOL_TYPE_STYLUS)
        assertEquals(0xFF123456.toInt(), view.color)
    }

    @Test fun renderedResultKeepsOutlinesOnTop() {
        view.tool = Tool.FILL; view.color = Color.YELLOW
        drag(listOf(center), MotionEvent.TOOL_TYPE_STYLUS)
        val out = view.renderResult()!!
        assertEquals(Color.YELLOW, out.getPixel(800, 800))
        // A pixel on the inner circle outline stays dark.
        val ring = (0..120).map { out.getPixel(800, 800 - it) }.any { Color.red(it) < 60 && Color.green(it) < 60 }
        assertTrue(ring)
    }

    @Test fun screenshotAfterColoring() {
        val colors = listOf(0xFFE53935, 0xFFFDD835, 0xFF1E88E5, 0xFF43A047, 0xFFAB47BC, 0xFFFB8C00).map { it.toInt() }
        view.tool = Tool.FILL
        view.color = colors[0]; drag(listOf(center), MotionEvent.TOOL_TYPE_STYLUS)
        view.color = colors[1]; drag(listOf(outsideCenter), MotionEvent.TOOL_TYPE_STYLUS)
        view.color = colors[2]; drag(listOf(800f to 80f), MotionEvent.TOOL_TYPE_STYLUS)
        view.color = colors[5]; drag(listOf(30f to 30f), MotionEvent.TOOL_TYPE_STYLUS)
        view.tool = Tool.BRUSH; view.color = colors[3]; view.brushSize = 40f
        drag(line(1050f to 800f, 1250f to 800f), MotionEvent.TOOL_TYPE_STYLUS)
        view.color = colors[4]; view.brushSize = 30f
        drag(line(800f to 560f, 1100f to 560f), MotionEvent.TOOL_TYPE_STYLUS)
        shadowOf(Looper.getMainLooper()).idle()
        screenshot("colored")
    }
}
