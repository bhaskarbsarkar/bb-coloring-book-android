package com.bb.coloringbook

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import java.io.File
import java.time.Duration

/** Phone-sized screen (portrait, ~Galaxy S Ultra). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-port-xxhdpi")
class PhoneLayoutTest {
    private fun settle() { repeat(8) { Thread.sleep(120); shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(300)) } }

    private fun <T : View> all(v: View, cls: Class<T>, out: MutableList<T> = mutableListOf()): List<T> {
        if (cls.isInstance(v)) out.add(cls.cast(v)!!)
        if (v is ViewGroup) for (i in 0 until v.childCount) all(v.getChildAt(i), cls, out)
        return out
    }

    private fun shot(v: View, name: String) {
        val b = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888); v.draw(Canvas(b))
        File("build/screenshots").mkdirs()
        File("build/screenshots/$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun phoneLayoutIsUsableAndStylusWorks() {
        val a = Robolectric.buildActivity(MainActivity::class.java).setup().get(); settle()
        val root = a.window.decorView
        val view = all(root, ColoringView::class.java).single()
        val screenW = root.width

        // Palette gets (nearly) the full width on its own row.
        val palette = all(root, HorizontalScrollView::class.java).last()
        assertTrue("palette width ${palette.width} of $screenW", palette.width > screenW * 0.85)
        // Canvas keeps most of the screen.
        assertTrue(view.height > root.height * 0.6)

        // Stylus fill works on the phone layout too.
        view.tool = Tool.FILL; view.color = Color.MAGENTA
        val p = view.imageToView(800f, 800f); val t = SystemClock.uptimeMillis()
        val props = arrayOf(MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_STYLUS })
        val coords = arrayOf(MotionEvent.PointerCoords().apply { x = p.x; y = p.y; pressure = 0.7f })
        view.dispatchTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, 1, props, coords, 0, 0, 1f, 1f, 0, 0, 0, 0))
        view.dispatchTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_UP, 1, props, coords, 0, 0, 1f, 1f, 0, 0, 0, 0))
        assertEquals(Color.MAGENTA, view.paintLayer!!.getPixel(800, 800))
        settle()
        shot(root, "phone-main")

        val m = MainActivity::class.java.getDeclaredMethod("showDrawings"); m.isAccessible = true; m.invoke(a); settle()
        shot(ShadowDialog.getLatestDialog().window!!.decorView, "phone-drawings")
        ShadowDialog.getLatestDialog().dismiss()
        val c = MainActivity::class.java.getDeclaredMethod("showColorDialog"); c.isAccessible = true; c.invoke(a); settle()
        shot(ShadowDialog.getLatestDialog().window!!.decorView, "phone-color")
    }
}
