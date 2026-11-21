package com.bb.coloringbook

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1280dp-h800dp-land-xhdpi")
class ColoringPagesTest {
    private fun settle() { repeat(10) { Thread.sleep(150); shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(300)) } }

    @Test fun twentyTwoPagesInTwoGroups() {
        val pages = ColoringPages.all
        assertEquals(10, pages.count { it.category == ColoringPages.Category.MANDALA })
        assertEquals(12, pages.count { it.category == ColoringPages.Category.KIDS })
        assertEquals(pages.size, pages.map { it.id }.toSet().size)
    }

    /** Every page must be colorable: many separate regions, and the main area must not leak to the border. */
    @Test fun everyPageHasManyFillableRegions() {
        for (page in ColoringPages.all) {
            val bmp = ColoringPages.render(page, 800)
            val px = IntArray(800 * 800); bmp.getPixels(px, 0, 800, 0, 0, 800, 800)
            val finder = RegionFinder(800, 800, RegionFinder.linesFromArgb(px))
            // Count regions on a coarse grid of sample points.
            val seen = mutableListOf<RegionFinder.Region>()
            for (y in 10 until 800 step 20) for (x in 10 until 800 step 20) {
                if (seen.any { it.contains(x, y) }) continue
                finder.find(x, y, grow = 0)?.let { seen.add(it) }
            }
            assertTrue("${page.name}: only ${seen.size} regions", seen.size >= 12)
        }
    }

    @Test fun pickingAPageStartsANewDrawing() {
        val a = Robolectric.buildActivity(MainActivity::class.java).setup().get(); settle()
        val before = a.projectStore.list().size
        a.showPagesPickerForTest(); settle()
        val root = ShadowDialog.getLatestDialog().window!!.decorView
        // screenshot for review
        val b = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888); root.draw(Canvas(b))
        File("build/screenshots").mkdirs(); File("build/screenshots/pages-picker.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }

        fun find(v: View): View? {
            if (v.contentDescription == "Puppy" && v.hasOnClickListeners()) return v
            if (v is ViewGroup) for (i in 0 until v.childCount) find(v.getChildAt(i))?.let { return it }
            return null
        }
        find(root)!!.performClick(); settle()
        assertEquals(before + 1, a.projectStore.list().size)
        assertEquals("Puppy", a.projectStore.list().first { it.id == a.openProjectId }.name)
    }
}
