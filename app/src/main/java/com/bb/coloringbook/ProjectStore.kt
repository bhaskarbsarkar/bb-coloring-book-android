package com.bb.coloringbook

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/**
 * Saved drawings. Each project is a folder: art.png (original picture), paint.png (colors),
 * thumb.jpg (preview) and name.txt.
 */
class ProjectStore(root: File) {
    private val dir = File(root, "projects").apply { mkdirs() }

    data class Project(val id: String, val name: String, val updated: Long, val thumb: File)

    private fun folder(id: String) = File(dir, id)

    fun list(): List<Project> = (dir.listFiles() ?: emptyArray())
        .filter { File(it, "art.png").exists() }
        .map { f ->
            val paint = File(f, "paint.png")
            Project(
                f.name,
                File(f, "name.txt").takeIf { it.exists() }?.readText()?.trim().orEmpty().ifEmpty { "Drawing" },
                if (paint.exists()) paint.lastModified() else f.lastModified(),
                File(f, "thumb.jpg"),
            )
        }
        .sortedByDescending { it.updated }

    fun exists(id: String?) = id != null && File(folder(id), "art.png").exists()

    /** Creates a project from a picture and returns its id. */
    fun create(art: Bitmap, name: String): String {
        val id = System.currentTimeMillis().toString()
        val f = folder(id).apply { mkdirs() }
        writePng(art, File(f, "art.png"))
        File(f, "name.txt").writeText(name)
        return id
    }

    fun saveProgress(id: String, paint: Bitmap, preview: Bitmap) {
        val f = folder(id)
        if (!f.exists()) return
        writePng(paint, File(f, "paint.png"))
        val s = 400f / maxOf(preview.width, preview.height)
        val thumb = if (s < 1f) Bitmap.createScaledBitmap(preview, (preview.width * s).toInt().coerceAtLeast(1), (preview.height * s).toInt().coerceAtLeast(1), true) else preview
        atomicWrite(File(f, "thumb.jpg")) { thumb.compress(Bitmap.CompressFormat.JPEG, 85, it) }
    }

    /** Returns (art, paint or null). */
    fun load(id: String): Pair<Bitmap, Bitmap?>? {
        val f = folder(id)
        val art = BitmapFactory.decodeFile(File(f, "art.png").path) ?: return null
        val paint = File(f, "paint.png").takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }
        return art to paint
    }

    fun rename(id: String, name: String) { if (folder(id).exists()) File(folder(id), "name.txt").writeText(name) }

    fun delete(id: String) { folder(id).deleteRecursively() }

    private fun writePng(b: Bitmap, f: File) = atomicWrite(f) { b.compress(Bitmap.CompressFormat.PNG, 100, it) }

    private fun atomicWrite(f: File, block: (java.io.OutputStream) -> Unit) {
        val tmp = File(f.path + ".tmp")
        tmp.outputStream().use(block)
        tmp.renameTo(f)
    }
}
