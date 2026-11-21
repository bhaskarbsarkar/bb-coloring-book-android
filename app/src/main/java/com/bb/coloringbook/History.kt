package com.bb.coloringbook

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect

/**
 * Undo/redo storing only the changed rectangle of each edit (before + after pixels).
 * Keeps one full copy of the last committed state to know the "before" pixels.
 */
class History(private val maxBytes: Long = 160L * 1024 * 1024) {
    private class Entry(val rect: Rect, val before: Bitmap, val after: Bitmap) {
        val bytes get() = before.byteCount.toLong() + after.byteCount
    }

    private var committed: Bitmap? = null
    private var committedCanvas: Canvas? = null
    private val undo = ArrayDeque<Entry>()
    private val redo = ArrayDeque<Entry>()
    private var used = 0L

    fun reset(current: Bitmap) {
        committed = current.copy(Bitmap.Config.ARGB_8888, true).also { committedCanvas = Canvas(it) }
        undo.clear(); redo.clear(); used = 0
    }

    fun canUndo() = undo.isNotEmpty()
    fun canRedo() = redo.isNotEmpty()

    /** Record that [rect] of [current] changed since the last commit. */
    fun commit(current: Bitmap, rect: Rect) {
        val base = committed ?: return
        val r = Rect(rect)
        if (!r.intersect(0, 0, current.width, current.height)) return
        val before = Bitmap.createBitmap(base, r.left, r.top, r.width(), r.height())
        val after = Bitmap.createBitmap(current, r.left, r.top, r.width(), r.height())
        committedCanvas!!.drawBitmap(after, null, r, null)
        val e = Entry(r, before, after)
        undo.addLast(e); used += e.bytes
        redo.clear()
        while (used > maxBytes && undo.size > 1) used -= undo.removeFirst().bytes
    }

    /** Throw away uncommitted changes in [rect]. */
    fun revert(current: Bitmap, rect: Rect) {
        val base = committed ?: return
        val r = Rect(rect)
        if (!r.intersect(0, 0, current.width, current.height)) return
        Canvas(current).drawBitmap(base, r, r, null)
    }

    fun undo(current: Bitmap): Boolean {
        val e = undo.removeLastOrNull() ?: return false
        Canvas(current).drawBitmap(e.before, null, e.rect, null)
        committedCanvas!!.drawBitmap(e.before, null, e.rect, null)
        redo.addLast(e)
        return true
    }

    fun redo(current: Bitmap): Boolean {
        val e = redo.removeLastOrNull() ?: return false
        Canvas(current).drawBitmap(e.after, null, e.rect, null)
        committedCanvas!!.drawBitmap(e.after, null, e.rect, null)
        undo.addLast(e)
        return true
    }
}
