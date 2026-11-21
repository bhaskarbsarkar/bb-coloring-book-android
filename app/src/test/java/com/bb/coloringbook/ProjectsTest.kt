package com.bb.coloringbook

import android.graphics.Color
import android.os.Looper
import android.view.View
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1280dp-h800dp-land-xhdpi")
class ProjectsTest {
    private fun settle() { repeat(6) { Thread.sleep(150); shadowOf(Looper.getMainLooper()).idle() } }

    private fun find(v: View): ColoringView? {
        if (v is ColoringView) return v
        if (v is android.view.ViewGroup) for (i in 0 until v.childCount) find(v.getChildAt(i))?.let { return it }
        return null
    }

    @Test fun multipleDrawingsSaveAndResume() {
        val ctrl = Robolectric.buildActivity(MainActivity::class.java).setup()
        val a = ctrl.get(); settle()
        val view = find(a.window.decorView)!!

        // First launch creates one drawing.
        assertEquals(1, a.projectStore.list().size)
        val first = a.openProjectId!!
        assertTrue(view.fillAt(800, 800, Color.BLUE))

        // Start a second drawing; the first is saved.
        a.newProjectForTest(SampleArt.mandala(800), "Second"); settle()
        val second = a.openProjectId!!
        assertNotEquals(first, second)
        assertEquals(Color.WHITE, view.paintLayer!!.getPixel(400, 400))
        assertTrue(view.fillAt(400, 400, Color.RED))
        assertEquals(2, a.projectStore.list().size)

        // Resume the first: its color is back.
        a.openProjectForTest(first); settle()
        assertEquals(Color.BLUE, view.paintLayer!!.getPixel(800, 800))

        // Back to the second: its color is back too.
        a.openProjectForTest(second); settle()
        assertEquals(Color.RED, view.paintLayer!!.getPixel(400, 400))

        // Restart the app: last opened drawing is restored with its colors.
        ctrl.pause().stop().destroy(); settle()
        val b = Robolectric.buildActivity(MainActivity::class.java).setup().get(); settle()
        assertEquals(second, b.openProjectId)
        assertEquals(Color.RED, find(b.window.decorView)!!.paintLayer!!.getPixel(400, 400))
        assertTrue(b.projectStore.list().all { it.thumb.exists() })
    }

    @Test fun renameAndDelete() {
        val a = Robolectric.buildActivity(MainActivity::class.java).setup().get(); settle()
        val s = a.projectStore
        val id = s.create(SampleArt.mandala(200), "X")
        s.rename(id, "Peacock")
        assertEquals("Peacock", s.list().first { it.id == id }.name)
        s.delete(id)
        assertFalse(s.exists(id))
    }
}
