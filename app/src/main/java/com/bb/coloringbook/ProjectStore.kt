package com.bb.coloringbook

import android.graphics.*
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

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

    /** Finished picture of a project: colors with outlines multiplied on top. */
    fun render(id: String): Bitmap? {
        val (art, paint) = load(id) ?: return null
        val out = Bitmap.createBitmap(art.width, art.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawColor(Color.WHITE)
        paint?.let { c.drawBitmap(it, 0f, 0f, null) }
        c.drawBitmap(art, 0f, 0f, Paint().apply { blendMode = BlendMode.MULTIPLY })
        return out
    }

    /** Writes every project into one zip that [importZip] can restore. */
    fun exportZip(out: OutputStream): Int {
        val projects = list()
        ZipOutputStream(out.buffered()).use { zip ->
            for (p in projects) for (name in FILES) {
                val f = File(folder(p.id), name)
                if (!f.exists()) continue
                zip.putNextEntry(ZipEntry("${p.id}/$name"))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return projects.size
    }

    /** Restores projects from a backup zip. Existing drawings are kept. Returns count added. */
    fun importZip(input: InputStream): Int {
        val staged = HashMap<String, File>() // zip id -> temp folder
        val tmpRoot = File(dir.parentFile, "import_tmp").apply { deleteRecursively(); mkdirs() }
        try {
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val e = zip.nextEntry ?: break
                    val m = ENTRY.matchEntire(e.name) ?: continue // ignores anything unexpected
                    val (zid, name) = m.destructured
                    val f = staged.getOrPut(zid) { File(tmpRoot, zid).apply { mkdirs() } }
                    File(f, name).outputStream().use { zip.copyTo(it) }
                }
            }
            var added = 0
            for ((zid, f) in staged) {
                if (!File(f, "art.png").exists()) continue
                // Same drawing already here? Keep both only if it differs; skip exact duplicates.
                val target = if (!folder(zid).exists()) folder(zid) else {
                    val same = File(folder(zid), "paint.png").let { it.exists() && File(f, "paint.png").exists() && it.length() == File(f, "paint.png").length() }
                    if (same) continue
                    folder("${System.currentTimeMillis()}${added}")
                }
                f.copyRecursively(target, overwrite = true)
                added++
            }
            return added
        } finally {
            tmpRoot.deleteRecursively()
        }
    }

    /** Latest change time over all projects (0 if none). */
    fun lastChange(): Long = list().maxOfOrNull { it.updated } ?: 0L

    companion object {
        private val FILES = listOf("art.png", "paint.png", "thumb.jpg", "name.txt")
        private val ENTRY = Regex("""([A-Za-z0-9_-]{1,40})/(art\.png|paint\.png|thumb\.jpg|name\.txt)""")
    }

    private fun writePng(b: Bitmap, f: File) = atomicWrite(f) { b.compress(Bitmap.CompressFormat.PNG, 100, it) }

    private fun atomicWrite(f: File, block: (java.io.OutputStream) -> Unit) {
        val tmp = File(f.path + ".tmp")
        tmp.outputStream().use(block)
        tmp.renameTo(f)
    }
}
