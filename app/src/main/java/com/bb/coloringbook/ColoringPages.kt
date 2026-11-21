package com.bb.coloringbook

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color

/** Built-in coloring pages, grouped for stress-relief coloring (kids' pages last). */
object ColoringPages {
    enum class Category(val title: String, val lineWidth: Float) {
        MANDALA("Mandalas", 0.45f),
        GEOMETRIC("Geometric & tiles", 0.5f),
        INDIAN("Indian motifs", 0.45f),
        ZEN("Zen patterns", 0.45f),
        NATURE("Nature & botanicals", 0.5f),
        OCEAN("Ocean & waves", 0.5f),
        GLASS("Stained glass", 1.1f),
        ANIMALS("Patterned animals", 0.5f),
        SCENES("Calm scenes", 0.55f),
        KIDS("For kids", 0.7f),
    }

    class Page(val id: String, val name: String, val category: Category, val draw: (Pen) -> Unit)

    private val sources: Map<Category, List<Pair<String, (Pen) -> Unit>>> by lazy {
        linkedMapOf(
            Category.MANDALA to Mandalas.all,
            Category.GEOMETRIC to Designs.geometric,
            Category.INDIAN to Designs.indian,
            Category.ZEN to Designs.zen,
            Category.NATURE to Designs.nature,
            Category.OCEAN to Designs.ocean,
            Category.GLASS to Designs.glass,
            Category.ANIMALS to Designs.animals,
            Category.SCENES to Designs.scenes,
            Category.KIDS to KidsArt.all,
        )
    }

    val all: List<Page> by lazy {
        Category.values().flatMap { cat ->
            sources.getValue(cat).map { (n, d) -> Page(cat.name.lowercase() + "_" + n.lowercase().replace(' ', '_'), n, cat, d) }
        }
    }

    fun render(page: Page, size: Int = 1600): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        page.draw(Pen(c, size / 100f, page.category.lineWidth))
        return bmp
    }
}
