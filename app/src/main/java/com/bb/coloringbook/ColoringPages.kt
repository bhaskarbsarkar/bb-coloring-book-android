package com.bb.coloringbook

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color

/** Built-in coloring pages: mandalas and original kids' cartoons. */
object ColoringPages {
    enum class Category(val title: String) { MANDALA("Mandalas"), KIDS("Kids") }

    class Page(val id: String, val name: String, val category: Category, val draw: (Pen) -> Unit)

    val all: List<Page> by lazy {
        Mandalas.all.map { (n, d) -> Page("m_" + n.lowercase().replace(' ', '_'), n, Category.MANDALA, d) } +
            KidsArt.all.map { (n, d) -> Page("k_" + n.lowercase().replace(' ', '_'), n, Category.KIDS, d) }
    }

    fun render(page: Page, size: Int = 1600): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        val lw = if (page.category == Category.MANDALA) 0.45f else 0.7f
        page.draw(Pen(c, size / 100f, lw))
        return bmp
    }
}
