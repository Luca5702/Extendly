package com.songxi.app.feature.splashad

import com.songxi.app.core.data.SplashAdLedgerRepository
import com.songxi.app.core.model.ClockProvider
import com.songxi.app.core.model.SplashAdCreative
import com.songxi.app.core.model.SplashArtworkStyle
import com.songxi.app.core.model.SystemClockProvider
import java.util.concurrent.atomic.AtomicInteger

class CuratedHouseSplashAdProvider(
    private val ledgerRepository: SplashAdLedgerRepository? = null,
    private val clockProvider: ClockProvider = SystemClockProvider
) : SplashAdProvider {

    private val roundRobinCursor = AtomicInteger(0)
    private val morningCursor = AtomicInteger(0)
    private val afternoonCursor = AtomicInteger(0)
    private val eveningCursor = AtomicInteger(0)
    private val impressionCounter = AtomicInteger(0)

    @Volatile
    var lastRecordedImpressionCreativeId: String? = null
        private set

    val recordedImpressionCount: Int
        get() = impressionCounter.get()

    override suspend fun fetchCreative(personalized: Boolean): SplashAdCreative {
        val size = BUILT_IN_CREATIVES.size
        return if (!personalized) {
            val index = Math.floorMod(roundRobinCursor.getAndIncrement(), size)
            BUILT_IN_CREATIVES[index]
        } else {
            val hour = Math.floorMod(clockProvider.currentLocalDateTime().hour, 24)
            val (primaryIndex, bucketCursor) = when (hour) {
                in 8..11 -> INDEX_WOOD_DESK_STAND to morningCursor
                in 12..17 -> INDEX_CELADON_TEA to afternoonCursor
                else -> INDEX_PINE_INCENSE to eveningCursor
            }
            val offset = Math.floorMod(bucketCursor.getAndIncrement(), size)
            BUILT_IN_CREATIVES[(primaryIndex + offset) % size]
        }
    }

    override suspend fun recordImpression(creativeId: String, epochMs: Long) {
        // Ledger impression persistence is already recorded once by SplashAdPolicyEngine;
        // track provider-side telemetry here without double-counting in SplashAdLedgerRepository.
        impressionCounter.incrementAndGet()
        lastRecordedImpressionCreativeId = creativeId
    }

    override suspend fun recordSkip(creativeId: String, elapsedMs: Long) {
        ledgerRepository?.recordSkip(creativeId, elapsedMs)
    }

    override suspend fun recordCtaClick(creativeId: String) {
        ledgerRepository?.recordCtaClick(creativeId)
    }

    companion object {
        private const val INDEX_CELADON_TEA: Int = 0
        private const val INDEX_PINE_INCENSE: Int = 1
        private const val INDEX_WOOD_DESK_STAND: Int = 2

        val BUILT_IN_CREATIVES: List<SplashAdCreative> = listOf(
            SplashAdCreative(
                id = "house_celadon_tea",
                sponsorName = "山茗器物 · 东方茶席",
                title = "案头一盏汝瓷温茶，陪你度过伏案午后",
                subtitle = "手作原矿天青釉主人杯 · 温润握感不烫手",
                ctaLabel = "了解器物详情",
                landingSummary = "山茗器物春季茶席展：原矿天青釉案头杯与茶则细节一览（外部浏览器打开）。",
                landingUrl = "https://example.com/songxi-showcase/celadon-tea",
                artworkTheme = SplashArtworkStyle.CELADON_TEA,
                durationSeconds = 3
            ),
            SplashAdCreative(
                id = "house_pine_incense",
                sponsorName = "松间闻香 · 草木线香",
                title = "一缕山松冷香，为书房留出片刻清净",
                subtitle = "古法柏木与冷杉合香 · 无化学助燃剂",
                ctaLabel = "探索草木香谱",
                landingSummary = "松间闻香节气合香志：天然冷杉、柏木与艾草线香配方介绍（外部浏览器打开）。",
                landingUrl = "https://example.com/songxi-showcase/pine-incense",
                artworkTheme = SplashArtworkStyle.PINE_INCENSE,
                durationSeconds = 3
            ),
            SplashAdCreative(
                id = "house_wood_stand",
                sponsorName = "案头清风 · 木作工坊",
                title = "把屏幕抬高半寸，让低垂的后颈自然归位",
                subtitle = "整块北美黑胡桃木榫卯桌面架 · 稳固温润",
                ctaLabel = "查看木作尺寸",
                landingSummary = "案头清风人体工学木作：黑胡桃木显示器增高架与护腕掌托说明（外部浏览器打开）。",
                landingUrl = "https://example.com/songxi-showcase/wood-stand",
                artworkTheme = SplashArtworkStyle.WOOD_DESK_STAND,
                durationSeconds = 3
            )
        )
    }
}
