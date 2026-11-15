package com.tobaccoscanner.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import com.tobaccoscanner.app.engine.ScanMode
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Procedural demo photos (best, medium, poor) so the scanner can be tried without any tobacco at hand. */
object SampleImages {
    fun make(mode: ScanMode, variant: Int): Bitmap {
        val w = 800
        val h = 600
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        if (mode == ScanMode.LEAF) leaf(c, w, h, variant) else powder(c, w, h, variant)
        return bmp
    }

    private fun rgba(a: Double, r: Int, g: Int, b: Int): Int = Color.argb((a * 255).toInt(), r, g, b)

    private fun leaf(c: Canvas, w: Int, h: Int, variant: Int) {
        val rnd = Random()
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        c.drawColor(Color.parseColor(if (variant == 0) "#1e293b" else if (variant == 1) "#64748b" else "#94a3b8"))

        c.save()
        c.translate(w / 2f, h / 2f)

        val outline = Path().apply {
            moveTo(-265f, 8f)
            cubicTo(-195f, -170f, 90f, -180f, 270f, -6f)
            cubicTo(90f, 180f, -195f, 170f, -265f, 8f)
            close()
        }
        c.clipPath(outline)

        val colors: IntArray
        val stops: FloatArray
        when (variant) {
            0 -> {
                colors = intArrayOf(Color.parseColor("#5a2c14"), Color.parseColor("#6b3619"), Color.parseColor("#522711"))
                stops = floatArrayOf(0f, 0.5f, 1f)
            }
            1 -> {
                colors = intArrayOf(Color.parseColor("#7c4c24"), Color.parseColor("#9b6b36"), Color.parseColor("#6f4723"))
                stops = floatArrayOf(0f, 0.55f, 1f)
            }
            else -> {
                colors = intArrayOf(Color.parseColor("#a38644"), Color.parseColor("#b69c56"), Color.parseColor("#587429"))
                stops = floatArrayOf(0f, 0.55f, 1f)
            }
        }
        fill.shader = LinearGradient(-255f, -135f, 255f, 135f, colors, stops, Shader.TileMode.CLAMP)
        c.drawRect(-275f, -195f, 275f, 195f, fill)
        fill.shader = null

        val count = if (variant == 0) 280 else if (variant == 1) 320 else 420
        for (i in 0 until count) {
            val px = ((rnd.nextDouble() - 0.5) * 510).toFloat()
            val py = ((rnd.nextDouble() - 0.5) * 330).toFloat()
            val r = (4 + rnd.nextDouble() * 18).toFloat()
            fill.color = when (variant) {
                0 -> if (i % 7 == 0) rgba(0.24, 165, 96, 46)
                else if (i % 2 == 0) rgba(0.14, 118, 60, 26) else rgba(0.14, 58, 26, 11)
                1 -> if (i % 6 == 0) rgba(0.26, 204, 168, 96)
                else if (i % 13 == 0) rgba(0.22, 84, 108, 42) else rgba(0.18, 92, 54, 24)
                else -> if (i % 3 == 0) rgba(0.38, 72, 112, 32)
                else if (i % 6 == 0) rgba(0.48, 30, 22, 18) else rgba(0.32, 222, 198, 124)
            }
            c.drawCircle(px, py, r, fill)
        }

        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            color = Color.parseColor(if (variant == 0) "#784626" else if (variant == 1) "#b79156" else "#dec68a")
            strokeWidth = if (variant == 0) 4.5f else if (variant == 1) 7.5f else 12f
        }
        c.drawPath(Path().apply { moveTo(-260f, 4f); quadTo(0f, -5f, 260f, -3f) }, stroke)

        stroke.strokeWidth = if (variant == 0) 1.8f else if (variant == 1) 2.6f else 3.8f
        val veinStep = if (variant == 0) 52 else if (variant == 1) 40 else 32
        var vx = -190
        while (vx <= 180) {
            c.drawPath(Path().apply { moveTo(vx.toFloat(), 0f); quadTo(vx + 28f, -58f, vx + 68f, -135f) }, stroke)
            c.drawPath(Path().apply { moveTo(vx.toFloat(), 1f); quadTo(vx + 28f, 58f, vx + 68f, 135f) }, stroke)
            vx += veinStep
        }
        c.restore()
    }

    private fun powder(c: Canvas, w: Int, h: Int, variant: Int) {
        val rnd = Random()
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.isAntiAlias = false
        c.drawColor(Color.parseColor("#cbd5e1"))

        val cx = w / 2f
        val cy = h / 2f
        val rad = 245f

        c.save()
        c.clipPath(Path().apply { addCircle(cx, cy, rad, Path.Direction.CW) })
        c.drawColor(Color.parseColor(if (variant == 0) "#492b15" else if (variant == 1) "#634324" else "#7b6340"))

        val granules = if (variant == 0) 14000 else if (variant == 1) 10500 else 9000
        val pal0 = listOf("#3d2210", "#543118", "#663c1e", "#4a2913", "#734523").map { Color.parseColor(it) }
        val pal1 = listOf("#54381e", "#6f4c2a", "#7f5832", "#472f19").map { Color.parseColor(it) }
        val pal2 = listOf("#6d5635", "#886e48", "#564227", "#9c8257").map { Color.parseColor(it) }

        fun rotRect(x: Float, y: Float, hw: Float, hh: Float, color: Int) {
            c.save()
            c.translate(x, y)
            c.rotate((rnd.nextDouble() * 180).toFloat())
            p.color = color
            c.drawRect(-hw, -hh, hw, hh, p)
            c.restore()
        }

        for (i in 0 until granules) {
            val ang = rnd.nextDouble() * PI * 2
            val d = sqrt(rnd.nextDouble()) * rad
            val gx = (cx + cos(ang) * d).toFloat()
            val gy = (cy + sin(ang) * d).toFloat()
            when (variant) {
                0 -> {
                    val sz = (1.3 + rnd.nextDouble() * 1.9).toFloat()
                    p.color = pal0[i % pal0.size]
                    c.drawRect(gx, gy, gx + sz, gy + sz, p)
                }
                1 -> {
                    if (i % 13 == 0) rotRect(gx, gy, 4f, 1.1f, Color.parseColor("#c8a86b"))
                    else {
                        p.color = pal1[i % pal1.size]
                        c.drawRect(gx, gy, gx + (2.2 + rnd.nextDouble() * 2.4).toFloat(), gy + 2.2f, p)
                    }
                }
                else -> {
                    if (i % 5 == 0) rotRect(gx, gy, 6.5f, 1.5f, Color.parseColor("#e0c688"))
                    else if (i % 10 == 0) { p.color = Color.parseColor("#ece9df"); c.drawRect(gx, gy, gx + 3.2f, gy + 3.2f, p) }
                    else if (i % 14 == 0) { p.color = Color.parseColor("#8b9496"); c.drawRect(gx, gy, gx + 4f, gy + 4f, p) }
                    else if (i % 9 == 0) { p.color = Color.parseColor("#628a30"); c.drawRect(gx, gy, gx + 3.5f, gy + 3.5f, p) }
                    else {
                        p.color = pal2[i % pal2.size]
                        c.drawRect(gx, gy, gx + (3.2 + rnd.nextDouble() * 3.2).toFloat(), gy + 3.2f, p)
                    }
                }
            }
        }
        c.restore()
    }
}
