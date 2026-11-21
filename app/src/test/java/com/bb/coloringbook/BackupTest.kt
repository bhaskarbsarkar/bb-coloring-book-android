package com.bb.coloringbook

import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class BackupTest {
    private val ctx get() = RuntimeEnvironment.getApplication()

    @Test fun backupSurvivesUninstallAndRestores() {
        val a = ProjectStore(File(ctx.filesDir, "a"))
        val id = a.create(SampleArt.mandala(300), "Peacock")
        val paint = android.graphics.Bitmap.createBitmap(300, 300, android.graphics.Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE); setPixel(150, 150, Color.RED) }
        a.saveProgress(id, paint, paint)
        val zip = ByteArrayOutputStream().also { assertEquals(1, a.exportZip(it)) }.toByteArray()

        // "Reinstall": a fresh, empty store.
        val b = ProjectStore(File(ctx.filesDir, "b"))
        assertEquals(1, b.importZip(ByteArrayInputStream(zip)))
        val p = b.list().single()
        assertEquals("Peacock", p.name)
        assertEquals(Color.RED, b.load(p.id)!!.second!!.getPixel(150, 150))
        assertEquals(Color.RED, b.render(p.id)!!.getPixel(150, 150))

        // Restoring the same backup twice doesn't duplicate.
        assertEquals(0, b.importZip(ByteArrayInputStream(zip)))
        assertEquals(1, b.list().size)
    }

    @Test fun maliciousZipEntriesAreIgnored() {
        val bytes = ByteArrayOutputStream().also { bo ->
            ZipOutputStream(bo).use { z ->
                z.putNextEntry(ZipEntry("../../evil/art.png")); z.write(1); z.closeEntry()
                z.putNextEntry(ZipEntry("x/../../art.png")); z.write(1); z.closeEntry()
            }
        }.toByteArray()
        val s = ProjectStore(File(ctx.filesDir, "c"))
        assertEquals(0, s.importZip(ByteArrayInputStream(bytes)))
        assertFalse(File(ctx.filesDir, "evil").exists())
    }

    @Test fun notAZipRestoresNothing() {
        val s = ProjectStore(File(ctx.filesDir, "d"))
        assertEquals(0, s.importZip(ByteArrayInputStream("hello".toByteArray())))
    }
}
