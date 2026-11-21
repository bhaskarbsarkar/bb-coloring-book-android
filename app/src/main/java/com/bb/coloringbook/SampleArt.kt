package com.bb.coloringbook

import android.graphics.*
import kotlin.math.cos
import kotlin.math.sin

/** Generates a built-in mandala so the app is usable before importing anything. */
object SampleArt {
    fun mandala(size: Int = 1600): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; color = Color.BLACK; strokeWidth = size / 240f
            strokeJoin = Paint.Join.ROUND
        }
        val cx = size / 2f; val cy = size / 2f; val R = size * 0.46f

        listOf(1.0f, 0.82f, 0.62f, 0.40f, 0.22f, 0.10f).forEach { c.drawCircle(cx, cy, R * it, p) }

        fun petals(n: Int, r0: Float, r1: Float, width: Float, phase: Float) {
            for (i in 0 until n) {
                val a = (2 * Math.PI * i / n + phase).toFloat()
                val path = Path()
                val bx = cx + cos(a) * r0; val by = cy + sin(a) * r0
                val tx = cx + cos(a) * r1; val ty = cy + sin(a) * r1
                val na = a + Math.PI.toFloat() / 2
                val mr = (r0 + r1) / 2
                path.moveTo(bx, by)
                path.quadTo(cx + cos(a) * mr + cos(na) * width, cy + sin(a) * mr + sin(na) * width, tx, ty)
                path.quadTo(cx + cos(a) * mr - cos(na) * width, cy + sin(a) * mr - sin(na) * width, bx, by)
                c.drawPath(path, p)
            }
        }
        petals(8, R * 0.10f, R * 0.40f, R * 0.12f, 0f)
        petals(8, R * 0.10f, R * 0.22f, R * 0.05f, (Math.PI / 8).toFloat())
        petals(16, R * 0.40f, R * 0.62f, R * 0.07f, 0f)
        petals(16, R * 0.62f, R * 0.82f, R * 0.06f, (Math.PI / 16).toFloat())
        petals(32, R * 0.82f, R * 1.0f, R * 0.04f, 0f)
        for (i in 0 until 16) {
            val a = (2 * Math.PI * i / 16 + Math.PI / 16).toFloat()
            c.drawCircle(cx + cos(a) * R * 0.51f, cy + sin(a) * R * 0.51f, R * 0.035f, p)
        }
        return bmp
    }
}
