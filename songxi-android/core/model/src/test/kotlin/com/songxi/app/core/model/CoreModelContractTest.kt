package com.songxi.app.core.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreModelContractTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun clockProviderSamAndLocalDateTimeConversion() {
        // 2026-11-16T02:00:00Z -> 2026-11-16T10:00:00 in Asia/Shanghai (UTC+8)
        val fixedEpochMs = 1_794_794_400_000L
        val samClock = ClockProvider { fixedEpochMs }
        assertEquals(fixedEpochMs, samClock.nowEpochMillis())

        val shanghaiClock = object : ClockProvider {
            override fun nowEpochMillis(): Long = fixedEpochMs
            override fun currentZoneId(): ZoneId = ZoneId.of("Asia/Shanghai")
        }
        assertEquals(LocalDateTime.of(2026, 11, 16, 10, 0, 0), shanghaiClock.currentLocalDateTime())
        assertEquals(LocalDate.of(2026, 11, 16), shanghaiClock.currentLocalDate())
        assertEquals("2026-11-16", shanghaiClock.currentDateIso())

        assertEquals(4_000L, BreathRhythm.INHALE_DURATION_MS)
        assertEquals(6_000L, BreathRhythm.EXHALE_DURATION_MS)
        assertEquals(10_000L, BreathRhythm.CYCLE_DURATION_MS)
        assertEquals(10, BreathRhythm.CYCLE_DURATION_SECONDS)
    }

    @Test
    fun enumLookupFallbacksAndDefaultSoundMapping() {
        assertEquals(SkinId.INK_MIST, SkinId.fromStorageKey("ink_mist"))
        assertEquals(SkinId.QINGLV_CELADON, SkinId.fromStorageKey("qinglv_celadon"))
        assertEquals(SkinId.DUSK_PINE, SkinId.fromStorageKey("dusk_pine"))
        assertEquals(SkinId.SPRING_BLOSSOM, SkinId.fromStorageKey("spring_blossom"))
        assertEquals(SkinId.INK_MIST, SkinId.fromStorageKey(null))
        assertEquals(SkinId.INK_MIST, SkinId.fromStorageKey("unknown_skin"))

        assertEquals(PostureScene.DESK_SEATED, PostureScene.fromKey("desk"))
        assertEquals(PostureScene.TEA_BREAK_STANDING, PostureScene.fromKey("tea"))
        assertEquals(PostureScene.EVENING_BEDSIDE, PostureScene.fromKey("bed"))
        assertEquals(PostureScene.DESK_SEATED, PostureScene.fromKey("missing"))

        assertEquals(BodyRegion.POSTERIOR_NECK, BodyRegion.fromKey("neck"))
        assertEquals(BodyRegion.TRAPEZIUS_SHOULDER, BodyRegion.fromKey("shoulder"))
        assertEquals(BodyRegion.THORACIC_CHEST, BodyRegion.fromKey("thoracic"))
        assertEquals(BodyRegion.LOWER_LUMBAR, BodyRegion.fromKey("lumbar"))
        assertEquals(BodyRegion.MOUSE_WRIST, BodyRegion.fromKey("wrist"))
        assertEquals(BodyRegion.EYE_BROW, BodyRegion.fromKey("eye"))
        assertNull(BodyRegion.fromKey("invalid"))

        assertEquals(SoundProfileId.SINGING_BOWL_432, SoundProfileId.defaultForSkin(SkinId.INK_MIST))
        assertEquals(SoundProfileId.BAMBOO_CHIME_528, SoundProfileId.defaultForSkin(SkinId.QINGLV_CELADON))
        assertEquals(SoundProfileId.GUQIN_PINE_396, SoundProfileId.defaultForSkin(SkinId.DUSK_PINE))
        assertEquals(SoundProfileId.SPRING_RAIN_440, SoundProfileId.defaultForSkin(SkinId.SPRING_BLOSSOM))
        assertEquals(SoundProfileId.SILENT_HAPTIC_ONLY, SoundProfileId.fromKey("silent"))

        assertEquals(SomaticFeeling.LIGHT_NECK_SHOULDER, SomaticFeeling.fromKey("light"))
        assertEquals(SomaticFeeling.WARM_OPEN, SomaticFeeling.fromKey("warm"))
        assertEquals(SomaticFeeling.CALM_PEACEFUL, SomaticFeeling.fromKey("calm"))
        assertNull(SomaticFeeling.fromKey("none"))
    }

    @Test
    fun poseKeyframeLerpAndBilateralMirroring() {
        val start = PoseKeyframe(
            headTiltDeg = 10f,
            chinTuckRatio = 0.2f,
            leftShoulderDropRatio = 0.4f,
            rightShoulderDropRatio = 0.6f,
            spineArcRatio = 0.5f,
            leftArmAngleDeg = 20f,
            rightArmAngleDeg = 40f,
            wristFlexRatio = 0.2f,
            eyesClosed = false
        )
        val end = PoseKeyframe(
            headTiltDeg = 20f,
            chinTuckRatio = 0.8f,
            leftShoulderDropRatio = 1.0f,
            rightShoulderDropRatio = 0.8f,
            spineArcRatio = -0.5f,
            leftArmAngleDeg = 10f,
            rightArmAngleDeg = 30f,
            wristFlexRatio = 0.8f,
            eyesClosed = true
        )

        assertEquals(start, start.lerp(end, -0.5f))
        assertEquals(start, start.lerp(end, 0f))
        assertEquals(end, start.lerp(end, 1f))
        assertEquals(end, start.lerp(end, 1.5f))

        val mid = start.lerp(end, 0.5f)
        assertEquals(15f, mid.headTiltDeg, 1e-4f)
        assertEquals(0.5f, mid.chinTuckRatio, 1e-4f)
        assertEquals(0f, mid.spineArcRatio, 1e-4f)
        assertTrue(mid.eyesClosed)

        val mirrored = start.mirroredBilaterally()
        assertEquals(-10f, mirrored.headTiltDeg, 1e-4f)
        assertEquals(0.6f, mirrored.leftShoulderDropRatio, 1e-4f)
        assertEquals(0.4f, mirrored.rightShoulderDropRatio, 1e-4f)
        assertEquals(40f, mirrored.leftArmAngleDeg, 1e-4f)
        assertEquals(20f, mirrored.rightArmAngleDeg, 1e-4f)
    }

    @Test
    fun stretchPoseEnforcesMax36CharGentleCueInvariant() {
        val validCue = "一".repeat(36)
        val pose = StretchPose(
            id = "valid_pose",
            title = "测试动作",
            gentleCue = validCue,
            targetRegions = setOf(BodyRegion.POSTERIOR_NECK),
            durationSeconds = 45,
            inhaleKeyframe = PoseKeyframe(),
            exhaleKeyframe = PoseKeyframe(chinTuckRatio = 0.6f)
        )
        assertEquals(36, pose.gentleCue.length)

        var threwFor37 = false
        try {
            StretchPose(
                id = "too_long_pose",
                title = "超长提示",
                gentleCue = "一".repeat(37),
                targetRegions = setOf(BodyRegion.POSTERIOR_NECK),
                durationSeconds = 45,
                inhaleKeyframe = PoseKeyframe(),
                exhaleKeyframe = PoseKeyframe(chinTuckRatio = 0.6f)
            )
        } catch (_: IllegalArgumentException) {
            threwFor37 = true
        }
        assertTrue("Expected IllegalArgumentException for >36-char gentleCue", threwFor37)
    }

    @Test
    fun kotlinxSerializationRoundTripForAllSerializableModels() {
        val poseA = StretchPose(
            id = "p1",
            title = "收颌微点头",
            gentleCue = "视线平视，下颌水平微收半寸，随呼气让后颈柔和变长。",
            targetRegions = setOf(BodyRegion.POSTERIOR_NECK),
            durationSeconds = 40,
            inhaleKeyframe = PoseKeyframe(eyesClosed = false),
            exhaleKeyframe = PoseKeyframe(chinTuckRatio = 0.8f, eyesClosed = true)
        )
        val poseB = poseA.copy(id = "p2", durationSeconds = 50)
        val routine = StretchRoutine(
            id = "r1",
            title = "工位微展",
            subtitle = "轻柔舒展",
            scene = PostureScene.DESK_SEATED,
            poses = listOf(poseA, poseB)
        )
        assertEquals(90, routine.totalDurationSeconds)
        assertEquals(routine, json.decodeFromString<StretchRoutine>(json.encodeToString(routine)))

        val booklet = BookletSeries(
            id = "b1",
            title = "《伏案颈胸开阔记》",
            subtitle = "温和舒展",
            totalDays = 2,
            dailyMinutes = 2,
            routines = listOf(routine, routine.copy(id = "r2"))
        )
        assertEquals(booklet, json.decodeFromString<BookletSeries>(json.encodeToString(booklet)))
        assertEquals("r2", booklet.routineForDay(99).id)

        val recordWithFeeling = PracticeSessionRecord(
            id = "rec_1",
            completedAtEpochMs = 1_794_794_400_000L,
            dateIso = "2026-11-16",
            routineId = "r1",
            routineTitle = "工位微展",
            durationSeconds = 90,
            skinId = SkinId.QINGLV_CELADON,
            feeling = SomaticFeeling.LIGHT_NECK_SHOULDER
        )
        val recordWithoutFeeling = recordWithFeeling.copy(id = "rec_2", feeling = null)
        assertEquals(
            recordWithFeeling,
            json.decodeFromString<PracticeSessionRecord>(json.encodeToString(recordWithFeeling))
        )
        assertEquals(
            recordWithoutFeeling,
            json.decodeFromString<PracticeSessionRecord>(json.encodeToString(recordWithoutFeeling))
        )

        val reminder = ReminderPreferences(
            enabled = true,
            intervalMinutes = 90,
            dailyMaxCount = 3,
            muteTodayDateIso = "2026-11-16"
        )
        assertEquals(reminder, json.decodeFromString<ReminderPreferences>(json.encodeToString(reminder)))

        val splash = SplashAdCreative(
            id = "creative_celadon",
            sponsorName = "松烟茶器",
            title = "一盏汝瓷 · 案头清欢",
            subtitle = "温润开片，陪你静度午后茶歇",
            ctaLabel = "了解详情",
            landingSummary = "东方案头茶器雅集",
            landingUrl = "https://songxi.example/tea",
            artworkTheme = SplashArtworkStyle.CELADON_TEA,
            durationSeconds = 3
        )
        assertEquals(splash, json.decodeFromString<SplashAdCreative>(json.encodeToString(splash)))
    }

    @Test
    fun reminderPreferencesQuietHoursEvaluation() {
        val prefs = ReminderPreferences()
        // Monday 2026-11-16
        assertFalse(prefs.isMutedOrQuietAt(LocalDateTime.of(2026, 11, 16, 10, 30)))
        // Lunch 12:00..13:30
        assertTrue(prefs.isMutedOrQuietAt(LocalDateTime.of(2026, 11, 16, 12, 0)))
        assertTrue(prefs.isMutedOrQuietAt(LocalDateTime.of(2026, 11, 16, 13, 29)))
        assertFalse(prefs.isMutedOrQuietAt(LocalDateTime.of(2026, 11, 16, 13, 30)))
        // Night 20:00..09:00
        assertTrue(prefs.isMutedOrQuietAt(LocalDateTime.of(2026, 11, 16, 20, 0)))
        assertTrue(prefs.isMutedOrQuietAt(LocalDateTime.of(2026, 11, 17, 2, 30)))
        assertFalse(prefs.isMutedOrQuietAt(LocalDateTime.of(2026, 11, 17, 9, 0)))
        // Saturday 2026-11-14
        assertTrue(prefs.isMutedOrQuietAt(LocalDateTime.of(2026, 11, 14, 15, 0)))
        // Mute today
        assertTrue(
            prefs.copy(muteTodayDateIso = "2026-11-16")
                .isMutedOrQuietAt(LocalDateTime.of(2026, 11, 16, 10, 30))
        )
        // Disabled
        assertTrue(
            prefs.copy(enabled = false)
                .isMutedOrQuietAt(LocalDateTime.of(2026, 11, 16, 10, 30))
        )
    }

    @Test
    fun clinicalPoseTimelineAndAsymmetryContract() {
        val bilateralPose = StretchPose(
            id = "bilateral_neck",
            title = "左右风池微倾",
            gentleCue = "随绵长呼气让颈侧缓缓松开。",
            targetRegions = setOf(BodyRegion.POSTERIOR_NECK),
            durationSeconds = 60,
            inhaleKeyframe = PoseKeyframe(),
            exhaleKeyframe = PoseKeyframe(headTiltDeg = 20f),
            bilateralSwitchAtHalfway = true
        )

        // 1. Balanced 50:50 -> switch at 30_000ms, primary side -1 (left) then +1 (right)
        val setup = bilateralPose.resolveTimelineGuidance(2_000L, LateralAsymmetry.BALANCED)
        assertEquals(PoseTimelineStage.SETUP, setup.stage)
        assertEquals(-1, setup.activeBilateralSide)
        assertFalse(setup.isSecondSegment)
        assertEquals(30_000L, setup.switchPointMs)

        val deepenFirst = bilateralPose.resolveTimelineGuidance(15_000L, LateralAsymmetry.BALANCED)
        assertEquals(PoseTimelineStage.DEEPEN_HOLD, deepenFirst.stage)
        assertEquals(bilateralPose.gentleCue, deepenFirst.activeCueCn)
        assertEquals(-1, deepenFirst.activeBilateralSide)

        val halfway = bilateralPose.resolveTimelineGuidance(30_000L, LateralAsymmetry.BALANCED)
        assertEquals(PoseTimelineStage.HALFWAY_TRANSITION, halfway.stage)
        assertTrue(halfway.isSecondSegment)
        assertEquals(1, halfway.activeBilateralSide)

        val safeReturn = bilateralPose.resolveTimelineGuidance(57_000L, LateralAsymmetry.BALANCED)
        assertEquals(PoseTimelineStage.SAFE_RETURN, safeReturn.stage)
        assertEquals(bilateralPose.clinical.safeReturnCueCn, safeReturn.activeCueCn)

        // 2. Right tighter 60:40 -> switch at 36_000ms, starts on +1 (right) for 36s then -1 (left) for 24s
        val rightEarly = bilateralPose.resolveTimelineGuidance(32_000L, LateralAsymmetry.RIGHT_TIGHTER)
        assertEquals(36_000L, rightEarly.switchPointMs)
        assertFalse(rightEarly.isSecondSegment)
        assertEquals(1, rightEarly.activeBilateralSide)

        val rightLate = bilateralPose.resolveTimelineGuidance(42_000L, LateralAsymmetry.RIGHT_TIGHTER)
        assertTrue(rightLate.isSecondSegment)
        assertEquals(-1, rightLate.activeBilateralSide)

        // 3. Left tighter 60:40 -> switch at 36_000ms, starts on -1 (left) for 36s then +1 (right) for 24s
        val leftEarly = bilateralPose.resolveTimelineGuidance(32_000L, LateralAsymmetry.LEFT_TIGHTER)
        assertEquals(36_000L, leftEarly.switchPointMs)
        assertFalse(leftEarly.isSecondSegment)
        assertEquals(-1, leftEarly.activeBilateralSide)
    }
}
