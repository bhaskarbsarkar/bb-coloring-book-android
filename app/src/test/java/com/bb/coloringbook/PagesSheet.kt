package com.bb.coloringbook
import android.graphics.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import java.io.File
@RunWith(RobolectricTestRunner::class) @GraphicsMode(GraphicsMode.Mode.NATIVE) @Config(sdk = [34])
class PagesSheet {
    @Test fun sheet() {
        File("build/screenshots").mkdirs()
        for (cat in ColoringPages.Category.values()) {
            val pages = ColoringPages.all.filter { it.category == cat }
            val cell = 400; val cols = 3; val rows = (pages.size + cols - 1) / cols
            val out = Bitmap.createBitmap(cols * cell, rows * cell, Bitmap.Config.ARGB_8888)
            val c = Canvas(out); c.drawColor(Color.LTGRAY)
            pages.forEachIndexed { i, pg -> c.drawBitmap(ColoringPages.render(pg, cell - 8), (i % cols * cell + 4).toFloat(), (i / cols * cell + 4).toFloat(), null) }
            File("build/screenshots/sheet-${cat.name}.png").outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
