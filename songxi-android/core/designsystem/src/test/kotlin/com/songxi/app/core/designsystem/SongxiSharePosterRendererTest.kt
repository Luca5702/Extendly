package com.songxi.app.core.designsystem

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SongxiSharePosterRendererTest {

    @Test
    fun renderPosterBitmap_produces720x1040XuanPosterWithZeroPureWhiteOrBlackAcrossAllSkins() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val month = YearMonth.of(2026, 11)
        val dayCounts = mapOf(2 to 1, 9 to 2, 16 to 3, 23 to 1)

        for (skin in BuiltInSkinRegistry.allSkins) {
            val bitmap = SongxiSharePosterRenderer.renderPosterBitmap(
                skin = skin,
                month = month,
                totalSessions = 7,
                totalMinutes = 18,
                practicedDaysCount = 4,
                daySessionCounts = dayCounts,
                topCaredRegionsLabel = "后颈风池 · 双肩斜方"
            )
            assertEquals(SongxiSharePosterRenderer.POSTER_WIDTH_PX, bitmap.width)
            assertEquals(SongxiSharePosterRenderer.POSTER_HEIGHT_PX, bitmap.height)

            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val distinctRgb = HashSet<Int>(256)
            for (argb in pixels) {
                val rgb = argb and 0x00FFFFFF
                assertFalse(
                    "Poster for ${skin.id} must not contain pure #FFFFFF",
                    rgb == 0xFFFFFF
                )
                assertFalse(
                    "Poster for ${skin.id} must not contain pure #000000",
                    rgb == 0x000000
                )
                distinctRgb.add(rgb)
            }
            assertTrue(
                "Expected rich multi-layered Shan-Shui poster colors for ${skin.id}, got ${distinctRgb.size}",
                distinctRgb.size >= 12
            )

            val outPng = SongxiSharePosterRenderer.writePosterToAppCache(context, bitmap, month)
            assertTrue(outPng.exists() && outPng.length() > 4_000L)
            assertFalse("Poster cache path must never point into /tmp: ${outPng.absolutePath}", outPng.absolutePath.startsWith("/tmp"))
            val header = outPng.inputStream().use { inp -> ByteArray(4).also { inp.read(it) } }
            assertEquals(0x89.toByte(), header[0])
            assertEquals(0x50.toByte(), header[1])
            assertEquals(0x4E.toByte(), header[2])
            assertEquals(0x47.toByte(), header[3])
            bitmap.recycle()
        }
    }
}
