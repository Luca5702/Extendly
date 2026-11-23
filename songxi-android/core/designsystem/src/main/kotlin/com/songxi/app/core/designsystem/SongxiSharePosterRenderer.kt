package com.songxi.app.core.designsystem

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.io.File
import java.io.FileOutputStream
import java.time.YearMonth
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-resolution offline Xuan-paper poster renderer ("松息日笺 · 宣纸水纹海报").
 * Renders a crisp 720x1040 ARGB_8888 Bitmap suitable for WeChat / Xiaohongshu / system image sharing
 * and MediaStore album saving — strictly preserving zero #FFFFFF and zero #000000 pixels.
 */
object SongxiSharePosterRenderer {

    const val POSTER_WIDTH_PX: Int = 720
    const val POSTER_HEIGHT_PX: Int = 1040

    fun renderPosterBitmap(
        skin: SongxiSkin,
        month: YearMonth,
        totalSessions: Int,
        totalMinutes: Int,
        practicedDaysCount: Int,
        daySessionCounts: Map<Int, Int>,
        topCaredRegionsLabel: String? = null
    ): Bitmap {
        val w = POSTER_WIDTH_PX.toFloat()
        val h = POSTER_HEIGHT_PX.toFloat()
        val bitmap = Bitmap.createBitmap(POSTER_WIDTH_PX, POSTER_HEIGHT_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val p = skin.palette
        val skyTop = safePosterArgb(p.canvasSkyWashTop)
        val canvasBase = safePosterArgb(p.canvasPrimary)
        val cardFill = safePosterArgb(p.surfaceCard)
        val borderArgb = safePosterArgb(p.borderHairline)
        val inkPrimary = safePosterArgb(p.inkPrimary)
        val inkSecondary = safePosterArgb(p.inkSecondary)
        val inkTertiary = safePosterArgb(p.inkTertiary)
        val accentArgb = safePosterArgb(p.accentPrimary)
        val accentSoft = safePosterArgb(p.accentPrimarySoft)
        val sealArgb = safePosterArgb(p.sealVermilion)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

        // 1. Atmospheric Xuan / Celadon vertical wash
        fillPaint.shader = LinearGradient(0f, 0f, 0f, h, skyTop, canvasBase, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, fillPaint)
        fillPaint.shader = null

        // 2. Subtle organic Xuan paper fibers
        strokePaint.color = p.inkSecondary.copy(alpha = 0.08f).toArgb()
        strokePaint.strokeWidth = 1.4f
        val seedBase = (skin.id.ordinal + 1) * 7919
        for (i in 0 until 64) {
            val s = seedBase + i * 37
            val fx = posterRand(s) * w
            val fy = posterRand(s + 13) * h
            val len = 10f + 18f * posterRand(s + 29)
            val ang = (posterRand(s + 53) - 0.5f) * 1.4f
            val dx = cos(ang) * len * 0.5f
            val dy = sin(ang) * len * 0.5f
            canvas.drawLine(fx - dx, fy - dy, fx + dx, fy + dy, strokePaint)
        }

        // 3. Top Shan-Shui distant mountain header inside folio card
        val outerRect = RectF(36f, 36f, w - 36f, h - 36f)
        fillPaint.color = cardFill
        canvas.drawRoundRect(outerRect, 36f, 36f, fillPaint)

        // Header landscape banner
        val bannerRect = RectF(64f, 64f, w - 64f, 290f)
        canvas.save()
        val clipPath = Path().apply { addRoundRect(bannerRect, 24f, 24f, Path.Direction.CW) }
        canvas.clipPath(clipPath)
        fillPaint.shader = LinearGradient(
            0f, bannerRect.top, 0f, bannerRect.bottom,
            skyTop, safePosterArgb(p.surfaceCardSubtle), Shader.TileMode.CLAMP
        )
        canvas.drawRect(bannerRect, fillPaint)
        fillPaint.shader = null

        // Celestial sun/moon orb
        fillPaint.color = skin.landscape.celestialColor.copy(alpha = 0.62f).toArgb()
        canvas.drawCircle(bannerRect.right - 110f, bannerRect.top + 66f, 32f, fillPaint)

        // Far + mid mountain silhouettes
        val farRidge = Path().apply {
            moveTo(bannerRect.left, bannerRect.bottom)
            lineTo(bannerRect.left, bannerRect.top + 148f)
            quadTo(bannerRect.left + 160f, bannerRect.top + 88f, bannerRect.left + 320f, bannerRect.top + 138f)
            quadTo(bannerRect.left + 470f, bannerRect.top + 96f, bannerRect.right, bannerRect.top + 142f)
            lineTo(bannerRect.right, bannerRect.bottom)
            close()
        }
        fillPaint.color = skin.landscape.farRidgeColor.copy(alpha = 0.55f).toArgb()
        canvas.drawPath(farRidge, fillPaint)

        val nearRidge = Path().apply {
            moveTo(bannerRect.left, bannerRect.bottom)
            lineTo(bannerRect.left, bannerRect.top + 184f)
            quadTo(bannerRect.left + 210f, bannerRect.top + 128f, bannerRect.left + 410f, bannerRect.top + 178f)
            quadTo(bannerRect.left + 520f, bannerRect.top + 146f, bannerRect.right, bannerRect.top + 172f)
            lineTo(bannerRect.right, bannerRect.bottom)
            close()
        }
        fillPaint.color = skin.landscape.midRidgeColor.copy(alpha = 0.72f).toArgb()
        canvas.drawPath(nearRidge, fillPaint)
        canvas.restore()

        strokePaint.color = borderArgb
        strokePaint.strokeWidth = 2f
        canvas.drawRoundRect(outerRect, 36f, 36f, strokePaint)
        canvas.drawRoundRect(bannerRect, 24f, 24f, strokePaint)

        // 4. Classical Vermilion Seal ("松息") overlapping banner bottom center
        val sealSize = 68f
        val sealRect = RectF(w * 0.5f - sealSize * 0.5f, 256f, w * 0.5f + sealSize * 0.5f, 256f + sealSize)
        fillPaint.color = sealArgb
        canvas.drawRoundRect(sealRect, 12f, 12f, fillPaint)
        strokePaint.color = safePosterArgb(Color(0xFFF3EFE6))
        strokePaint.strokeWidth = 2.4f
        val innerSeal = RectF(sealRect.left + 6f, sealRect.top + 6f, sealRect.right - 6f, sealRect.bottom - 6f)
        canvas.drawRoundRect(innerSeal, 8f, 8f, strokePaint)

        val sealTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = safePosterArgb(Color(0xFFF5F1E8))
            textSize = 24f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("松息", w * 0.5f, sealRect.centerY() + 9f, sealTextPaint)

        // 5. Title & Active Skin Verse
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkPrimary
            textSize = 34f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("松息日笺 · ${skin.meta.titleCn}", w * 0.5f, 376f, titlePaint)

        val versePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentArgb
            textSize = 24f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(skin.meta.completionVerse, w * 0.5f, 422f, versePaint)

        // 6. Calm Monthly Summary Pill
        val summaryRect = RectF(76f, 452f, w - 76f, 544f)
        fillPaint.color = accentSoft
        canvas.drawRoundRect(summaryRect, 22f, 22f, fillPaint)

        val summaryMainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkPrimary
            textSize = 25f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "${month.year}年${month.monthValue}月 · 舒展 $totalSessions 息 · 共 $totalMinutes 分钟",
            w * 0.5f,
            494f,
            summaryMainPaint
        )

        val subLine = if (!topCaredRegionsLabel.isNullOrBlank()) {
            "落笔 $practicedDaysCount 日 · 重点温养：$topCaredRegionsLabel"
        } else {
            "落笔 $practicedDaysCount 日 · 留白亦是休息，从不计算连续天数"
        }
        val summarySubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkSecondary
            textSize = 19f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(subLine, w * 0.5f, 526f, summarySubPaint)

        // 7. Miniature Concentric Water-Ripple Month Grid
        val gridLeft = 94f
        val gridRight = w - 94f
        val colWidth = (gridRight - gridLeft) / 7f
        val headerY = 586f
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkTertiary
            textSize = 18f
            textAlign = Paint.Align.CENTER
        }
        val weekdays = arrayOf("一", "二", "三", "四", "五", "六", "日")
        for (c in 0 until 7) {
            canvas.drawText(weekdays[c], gridLeft + colWidth * (c + 0.5f), headerY, headerPaint)
        }

        val firstOffset = month.atDay(1).dayOfWeek.value - 1
        val daysInMonth = month.lengthOfMonth()
        val dayNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 18f
            textAlign = Paint.Align.CENTER
        }

        for (day in 1..daysInMonth) {
            val slot = firstOffset + day - 1
            val row = slot / 7
            val col = slot % 7
            val cx = gridLeft + colWidth * (col + 0.5f)
            val cy = 626f + row * 52f
            val count = daySessionCounts[day] ?: 0
            if (count > 0) {
                fillPaint.color = accentSoft
                canvas.drawCircle(cx, cy, 20f, fillPaint)
                strokePaint.color = accentArgb
                val rings = count.coerceIn(1, 3)
                for (r in 1..rings) {
                    strokePaint.strokeWidth = if (r == 1) 2.2f else 1.5f
                    canvas.drawCircle(cx, cy, 11f + r * 4.2f, strokePaint)
                }
                dayNumPaint.color = accentArgb
                dayNumPaint.typeface = Typeface.DEFAULT_BOLD
            } else {
                dayNumPaint.color = inkSecondary
                dayNumPaint.typeface = Typeface.DEFAULT
            }
            canvas.drawText(day.toString(), cx, cy + 6f, dayNumPaint)
        }

        // 8. Poetic Colophon Footer
        strokePaint.color = borderArgb
        strokePaint.strokeWidth = 1.5f
        canvas.drawLine(96f, h - 118f, w - 96f, h - 118f, strokePaint)

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkSecondary
            textSize = 20f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("一松一息，身自舒展 · 松息 Sōngxī 独立静心微拉伸", w * 0.5f, h - 76f, footerPaint)

        return bitmap
    }

    /**
     * Writes the rendered PNG into app-private `context.cacheDir/share_cards/` (never `/tmp`).
     */
    fun writePosterToAppCache(
        context: Context,
        bitmap: Bitmap,
        month: YearMonth
    ): File {
        val dir = File(context.cacheDir, "share_cards").apply { mkdirs() }
        val outFile = File(dir, "songxi_rijian_${month.year}_${month.monthValue}.png")
        FileOutputStream(outFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
        }
        return outFile
    }

    private fun safePosterArgb(color: Color): Int {
        val raw = color.toArgb()
        val rgb = raw and 0x00FFFFFF
        return when (rgb) {
            0xFFFFFF -> (raw and -0x1000000) or 0xF7F4EC
            0x000000 -> (raw and -0x1000000) or 0x141A18
            else -> raw
        }
    }

    private fun posterRand(seed: Int): Float {
        var x = seed
        x = (x xor (x ushr 16)) * -0x7a143595
        x = (x xor (x ushr 13)) * -0x3d4d51cb
        x = x xor (x ushr 16)
        return ((x ushr 8) and 0xFFFFFF) / 16777215f
    }
}
