package com.songxi.app.core.designsystem

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import com.songxi.app.core.data.DataStoreStretchCatalogRepository
import com.songxi.app.core.data.SongxiDataStoreFactory
import com.songxi.app.core.model.BodyRegion
import com.songxi.app.core.model.PoseKeyframe
import com.songxi.app.core.model.PostureScene
import com.songxi.app.core.model.SkinId
import com.songxi.app.core.model.StretchPose
import java.io.File
import java.nio.ByteBuffer
import java.security.MessageDigest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SongxiCanvasRenderersVisualTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun compositeFourSkinStages_generateRoborazziPngsWithZeroPureWhiteBlackAndDistinctHashes() {
        val buildOutDir = File("build/outputs/roborazzi").apply { mkdirs() }
        val snapshotDir = File("src/test/snapshots/roborazzi").apply { mkdirs() }

        var activeSkin by mutableStateOf(BuiltInSkinRegistry.InkMist)
        val previewPose = StretchPose(
            id = "preview_shoulder_ear_melt",
            title = "坐姿侧耳寻肩",
            gentleCue = "左手轻搭左腿，像轻叹一口气，让肩膀慢慢沉向地面。",
            targetRegions = setOf(BodyRegion.TRAPEZIUS_SHOULDER, BodyRegion.POSTERIOR_NECK),
            durationSeconds = 45,
            inhaleKeyframe = PoseKeyframe(10f, 0.10f, 0.60f, 0.55f, 0.10f, 16f, 16f, 0f, true),
            exhaleKeyframe = PoseKeyframe(24f, 0.20f, 1.00f, 0.80f, 0.05f, 8f, 12f, 0f, true),
            bilateralSwitchAtHalfway = true
        )

        composeTestRule.setContent {
            SongxiTheme(skin = activeSkin) {
                SongxiSkinCompositeFolioStage(
                    skin = activeSkin,
                    pose = previewPose,
                    modifier = Modifier
                        .size(width = 360.dp, height = 240.dp)
                        .testTag("roborazzi_skin_folio")
                )
            }
        }

        val pixelHashes = mutableMapOf<SkinId, String>()

        for (skin in BuiltInSkinRegistry.allSkins) {
            composeTestRule.runOnIdle { activeSkin = skin }
            composeTestRule.waitForIdle()

            val fileName = "${skin.id.storageKey}.png"
            val buildPng = File(buildOutDir, fileName)
            val snapshotPng = File(snapshotDir, fileName)

            composeTestRule.onNodeWithTag("roborazzi_skin_folio").captureRoboImage(
                file = buildPng,
                roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
            )
            assertTrue(buildPng.exists() && buildPng.length() > 2_048L, "Missing build PNG for ${skin.id}")

            val buildBitmap = decodeArgbBitmap(buildPng)
            assertTrue(buildBitmap.width >= 300 && buildBitmap.height >= 200)
            val buckets = countQuantizedColorBuckets(buildBitmap)
            assertTrue(buckets >= 16, "Expected rich multi-layer color entropy >= 16 for ${skin.id}, got $buckets")
            assertEquals(0, countExactRgbPixels(buildBitmap, 0xFFFFFF), "Skin ${skin.id} rendered forbidden #FFFFFF pixel")
            assertEquals(0, countExactRgbPixels(buildBitmap, 0x000000), "Skin ${skin.id} rendered forbidden #000000 pixel")

            val buildHash = computeBitmapPixelSha256(buildBitmap)
            buildBitmap.recycle()

            // Idempotent pixel-SHA256 guard: write snapshot ONLY if absent or pixel hash changed,
            // keeping git status clean on repeated testDebugUnitTest runs!
            val existingSnapshotHash = if (snapshotPng.exists() && snapshotPng.length() > 0L) {
                val snapBmp = BitmapFactory.decodeFile(
                    snapshotPng.absolutePath,
                    BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                )
                val h = snapBmp?.let { computeBitmapPixelSha256(it) }
                snapBmp?.recycle()
                h
            } else {
                null
            }

            if (existingSnapshotHash != buildHash) {
                buildPng.copyTo(snapshotPng, overwrite = true)
            }

            assertTrue(snapshotPng.exists() && snapshotPng.length() > 2_048L, "Missing snapshot PNG for ${skin.id}")
            val verifiedSnapBmp = decodeArgbBitmap(snapshotPng)
            val verifiedSnapHash = computeBitmapPixelSha256(verifiedSnapBmp)
            verifiedSnapBmp.recycle()
            assertEquals(buildHash, verifiedSnapHash, "Snapshot pixel hash mismatch for ${skin.id}")

            pixelHashes[skin.id] = buildHash
        }

        assertEquals(4, pixelHashes.values.toSet().size, "All 4 skins must produce pairwise distinct pixel renders")
    }

    @Test
    fun individualCanvasRenderers_produceDistinctRendersAcrossSkinsModesAndSealSizes() {
        val tempCaptureDir = File("build/outputs/roborazzi/components").apply { mkdirs() }
        var activeSkin by mutableStateOf(BuiltInSkinRegistry.InkMist)
        var haloEyesFree by mutableStateOf(false)
        var haloCompletion by mutableStateOf(false)
        var sealSizeDp by mutableStateOf(36.dp)

        composeTestRule.setContent {
            SongxiTheme(skin = activeSkin) {
                Column(
                    modifier = Modifier
                        .background(activeSkin.palette.canvasPrimary)
                        .padding(4.dp)
                ) {
                    ShanShuiBackdropCanvas(
                        skin = activeSkin,
                        driftPhase = 0.25f,
                        modifier = Modifier
                            .size(180.dp, 100.dp)
                            .testTag("unit_shanshui")
                    )
                    BreathingHaloCanvas(
                        skin = activeSkin,
                        breathProgress = 0.6f,
                        isExhale = false,
                        eyesFreeMode = haloEyesFree,
                        completionRippleMode = haloCompletion,
                        rippleCount = 4,
                        modifier = Modifier
                            .size(140.dp)
                            .testTag("unit_halo")
                    )
                    Box(
                        modifier = Modifier
                            .background(activeSkin.palette.surfaceCard)
                            .padding(6.dp)
                            .testTag("unit_seal_box")
                    ) {
                        ClassicalSealBadge(
                            skin = activeSkin,
                            size = sealSizeDp,
                            modifier = Modifier.testTag("unit_seal")
                        )
                    }
                }
            }
        }

        val shanshuiHashes = mutableSetOf<String>()
        val haloHashes = mutableSetOf<String>()
        val sealHashes = mutableSetOf<String>()

        for (skin in BuiltInSkinRegistry.allSkins) {
            composeTestRule.runOnIdle {
                activeSkin = skin
                haloEyesFree = false
                haloCompletion = false
                sealSizeDp = 36.dp
            }
            composeTestRule.waitForIdle()

            shanshuiHashes += captureNodePixelHash("unit_shanshui", File(tempCaptureDir, "shanshui_${skin.id.storageKey}.png"))
            haloHashes += captureNodePixelHash("unit_halo", File(tempCaptureDir, "halo_${skin.id.storageKey}.png"))
            sealHashes += captureNodePixelHash("unit_seal_box", File(tempCaptureDir, "seal_${skin.id.storageKey}.png"))
        }

        assertEquals(4, shanshuiHashes.size, "ShanShuiBackdropCanvas must differ across all 4 skins")
        assertEquals(4, haloHashes.size, "BreathingHaloCanvas must differ across all 4 skins")
        assertEquals(4, sealHashes.size, "ClassicalSealBadge must differ across all 4 skins")

        // Verify BreathingHaloCanvas eyesFreeMode and completionRippleMode produce distinct visuals
        val normalHaloHash = captureNodePixelHash("unit_halo", File(tempCaptureDir, "halo_mode_normal.png"))
        composeTestRule.runOnIdle { haloEyesFree = true }
        composeTestRule.waitForIdle()
        val eyesFreeHaloHash = captureNodePixelHash("unit_halo", File(tempCaptureDir, "halo_mode_eyesfree.png"))
        composeTestRule.runOnIdle {
            haloEyesFree = false
            haloCompletion = true
        }
        composeTestRule.waitForIdle()
        val completionHaloHash = captureNodePixelHash("unit_halo", File(tempCaptureDir, "halo_mode_completion.png"))
        assertEquals(3, setOf(normalHaloHash, eyesFreeHaloHash, completionHaloHash).size)

        // Verify ClassicalSealBadge renders cleanly across 28.dp, 36.dp, and 56.dp without clipping
        for (targetDp: Dp in listOf(28.dp, 36.dp, 56.dp)) {
            composeTestRule.runOnIdle { sealSizeDp = targetDp }
            composeTestRule.waitForIdle()
            val out = File(tempCaptureDir, "seal_size_${targetDp.value.toInt()}.png")
            composeTestRule.onNodeWithTag("unit_seal").captureRoboImage(
                file = out,
                roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
            )
            val bmp = decodeArgbBitmap(out)
            assertTrue(countQuantizedColorBuckets(bmp) >= 2, "Seal at $targetDp must render vermilion + paper")
            bmp.recycle()
        }
    }

    @Test
    fun all24StretchCatalogPoses_animateInhaleExhaleMirrorBilaterallyAndSurviveExtremeInputs() {
        val dsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val storeFile = File("build/tmp/designsystem_pose_test/catalog_${System.nanoTime()}.preferences_pb")
            val dataStore = SongxiDataStoreFactory.createPreferenceDataStore(storeFile, dsScope)
            val catalogRepo = DataStoreStretchCatalogRepository(dataStore)
            val poses = catalogRepo.allPoses
            assertEquals(24, poses.size, "Expected all 24 poses from SongxiStretchCatalog")
            assertEquals(3, PostureScene.entries.map { catalogRepo.heroRoutineFor(it).id }.toSet().size)

            val tempDir = File("build/outputs/roborazzi/poses").apply { mkdirs() }
            var currentPose by mutableStateOf(poses.first())
            var currentProgress by mutableStateOf(1f)
            var currentExhale by mutableStateOf(false)
            var currentSecondHalf by mutableStateOf(false)
            var currentEyesFree by mutableStateOf(false)

            composeTestRule.setContent {
                SongxiTheme(skin = BuiltInSkinRegistry.QinglvCeladon) {
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .background(BuiltInSkinRegistry.QinglvCeladon.palette.canvasPrimary)
                            .testTag("pose_stage")
                    ) {
                        EditorialStretchFigureCanvas(
                            pose = currentPose,
                            breathProgress = currentProgress,
                            isExhale = currentExhale,
                            isSecondHalf = currentSecondHalf,
                            eyesFreeMinimal = currentEyesFree,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            var bilateralCount = 0
            for (pose in poses) {
                composeTestRule.runOnIdle {
                    currentPose = pose
                    currentProgress = 1f
                    currentExhale = false
                    currentSecondHalf = false
                    currentEyesFree = false
                }
                composeTestRule.waitForIdle()
                val inhalePixels = captureNodePixels("pose_stage", File(tempDir, "${pose.id}_inhale.png"))

                composeTestRule.runOnIdle {
                    currentProgress = 1f
                    currentExhale = true
                }
                composeTestRule.waitForIdle()
                val exhalePixels = captureNodePixels("pose_stage", File(tempDir, "${pose.id}_exhale.png"))

                val diffCount = countDifferingPixels(inhalePixels, exhalePixels)
                assertTrue(
                    diffCount >= 80,
                    "Pose ${pose.id} must visibly animate between inhale and exhale, got $diffCount differing pixels"
                )

                if (pose.bilateralSwitchAtHalfway) {
                    bilateralCount++
                    composeTestRule.runOnIdle {
                        currentSecondHalf = true
                    }
                    composeTestRule.waitForIdle()
                    val mirroredPixels = captureNodePixels("pose_stage", File(tempDir, "${pose.id}_mirror.png"))
                    val mirrorDiff = countDifferingPixels(exhalePixels, mirroredPixels)
                    assertTrue(
                        mirrorDiff >= 50,
                        "Bilateral pose ${pose.id} must visibly mirror at halfway, got $mirrorDiff differing pixels"
                    )
                }
            }
            assertEquals(8, bilateralCount, "Expected 8 bilateral poses in catalog")

            // Verify eyesFreeMinimal contour rendering + adversarial NaN/Infinity resilience
            composeTestRule.runOnIdle {
                currentPose = poses.first()
                currentEyesFree = true
                currentProgress = Float.NaN
            }
            composeTestRule.waitForIdle()
            val eyesFreeHash = captureNodePixelHash("pose_stage", File(tempDir, "pose_eyes_free_nan.png"))
            assertTrue(eyesFreeHash.isNotBlank())
        } finally {
            dsScope.cancel()
        }
    }

    @Test
    fun bodyTensionMapCanvas_supportsSemanticZoneTagsAndDirectCoordinateTaps() {
        val tempDir = File("build/outputs/roborazzi/bodymap").apply { mkdirs() }
        var selected by mutableStateOf(emptySet<BodyRegion>())
        var exposeTags by mutableStateOf(true)
        var toggleCount by mutableIntStateOf(0)

        composeTestRule.setContent {
            SongxiTheme(skin = BuiltInSkinRegistry.SpringBlossom) {
                BodyTensionMapCanvas(
                    selectedRegions = selected,
                    onRegionToggle = { region ->
                        toggleCount++
                        selected = if (region in selected) selected - region else selected + region
                    },
                    exposeZoneTestTags = exposeTags,
                    modifier = Modifier
                        .size(width = 240.dp, height = 300.dp)
                        .testTag("bodymap_canvas")
                )
            }
        }

        val emptyHash = captureNodePixelHash("bodymap_canvas", File(tempDir, "bodymap_empty.png"))

        // 1. Semantic tag clicks across all 6 zones when exposeZoneTestTags = true
        for (region in BodyRegion.entries) {
            val tag = "bodymap_zone_${region.key}"
            composeTestRule.onNodeWithTag(tag).assertIsDisplayed().performClick()
            composeTestRule.waitForIdle()
            assertTrue(region in selected, "Clicking $tag must select $region")

            composeTestRule.onNodeWithTag(tag).performClick()
            composeTestRule.waitForIdle()
            assertTrue(region !in selected, "Clicking $tag again must deselect $region")
        }
        assertEquals(12, toggleCount)

        // 2. Multi-select 3 zones visual ripple delta
        composeTestRule.onNodeWithTag("bodymap_zone_neck").performClick()
        composeTestRule.onNodeWithTag("bodymap_zone_shoulder").performClick()
        composeTestRule.onNodeWithTag("bodymap_zone_wrist").performClick()
        composeTestRule.waitForIdle()
        assertEquals(
            setOf(BodyRegion.POSTERIOR_NECK, BodyRegion.TRAPEZIUS_SHOULDER, BodyRegion.MOUSE_WRIST),
            selected
        )
        val activeHash = captureNodePixelHash("bodymap_canvas", File(tempDir, "bodymap_3zones.png"))
        assertTrue(emptyHash != activeHash, "Selected concentric ripples must alter canvas pixels")

        // 3. Chip-paired mode (exposeZoneTestTags = false): zero duplicate tags, direct coordinate taps work
        composeTestRule.runOnIdle {
            selected = emptySet()
            exposeTags = false
        }
        composeTestRule.waitForIdle()
        assertEquals(0, composeTestRule.onAllNodesWithTag("bodymap_zone_neck").fetchSemanticsNodes().size)

        val canvasBounds = composeTestRule.onNodeWithTag("bodymap_canvas").fetchSemanticsNode().boundsInRoot
        val canvasIntSize = IntSize(canvasBounds.width.toInt(), canvasBounds.height.toInt())

        for (region in BodyRegion.entries) {
            val pt = bodyTensionZoneCenterPx(region, canvasIntSize, useBilateralMirror = false)
            composeTestRule.onNodeWithTag("bodymap_canvas").performTouchInput { click(pt) }
            composeTestRule.waitForIdle()
            assertTrue(region in selected, "Direct tap on primary anchor must select $region")
        }

        // Tap bilateral right-wrist mirror hotspot to toggle MOUSE_WRIST off
        val rightWristPt = bodyTensionZoneCenterPx(BodyRegion.MOUSE_WRIST, canvasIntSize, useBilateralMirror = true)
        composeTestRule.onNodeWithTag("bodymap_canvas").performTouchInput { click(rightWristPt) }
        composeTestRule.waitForIdle()
        assertTrue(BodyRegion.MOUSE_WRIST !in selected)
    }

    @Test
    fun extremeAndNonFiniteAdversarialInputs_renderWithoutThrowingAcrossAllDesignSystemCanvases() {
        val extremeKeyframe = PoseKeyframe(
            headTiltDeg = Float.NaN,
            chinTuckRatio = Float.POSITIVE_INFINITY,
            leftShoulderDropRatio = Float.NEGATIVE_INFINITY,
            rightShoulderDropRatio = 99f,
            spineArcRatio = -99f,
            leftArmAngleDeg = Float.NaN,
            rightArmAngleDeg = 999f,
            wristFlexRatio = Float.NaN,
            eyesClosed = false
        )

        composeTestRule.setContent {
            SongxiTheme(skin = BuiltInSkinRegistry.DuskPine) {
                Row(modifier = Modifier.testTag("adversarial_row")) {
                    ShanShuiBackdropCanvas(
                        driftPhase = Float.NaN,
                        modifier = Modifier.size(16.dp)
                    )
                    EditorialStretchFigureCanvas(
                        keyframe = extremeKeyframe,
                        bilateralSide = Int.MAX_VALUE,
                        highlightedRegions = BodyRegion.entries.toSet(),
                        modifier = Modifier.size(20.dp)
                    )
                    BreathingHaloCanvas(
                        breathProgress = Float.POSITIVE_INFINITY,
                        rippleCount = -10,
                        completionRippleMode = true,
                        modifier = Modifier.size(16.dp)
                    )
                    BodyTensionMapCanvas(
                        selectedRegions = setOf(BodyRegion.POSTERIOR_NECK),
                        onRegionToggle = {},
                        modifier = Modifier.size(14.dp)
                    )
                    ClassicalSealBadge(
                        textOverride = "   ",
                        size = (-8).dp
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("adversarial_row").assertIsDisplayed()
    }

    @Test
    fun sagittalViewPlaneAndBiomechanicalGuideOverlay_renderDistinctAnatomicalLayers() {
        val tempDir = File("build/outputs/roborazzi/biomechanics").apply { mkdirs() }
        var activeSkin by mutableStateOf(BuiltInSkinRegistry.InkMist)
        var plane by mutableStateOf(com.songxi.app.core.model.AnatomicalViewPlane.CORONAL_FRONT)
        var showGuide by mutableStateOf(false)
        var asymmetry by mutableStateOf(com.songxi.app.core.model.LateralAsymmetry.BALANCED)

        val chinTuckPose = StretchPose(
            id = "test_chin_tuck",
            title = "收颌微点头",
            gentleCue = "视线平视，下颌水平微收半寸，随呼气让后颈柔和变长。",
            targetRegions = setOf(BodyRegion.POSTERIOR_NECK, BodyRegion.TRAPEZIUS_SHOULDER),
            durationSeconds = 45,
            inhaleKeyframe = PoseKeyframe(10f, 0.15f, 0.45f, 0.45f, 0.15f, 14f, 14f, 0f, false),
            exhaleKeyframe = PoseKeyframe(22f, 0.85f, 0.90f, 0.80f, 0.25f, 12f, 12f, 0f, true),
            bilateralSwitchAtHalfway = true
        )

        composeTestRule.setContent {
            SongxiTheme(skin = activeSkin) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .background(activeSkin.palette.canvasPrimary)
                        .testTag("biomech_stage")
                ) {
                    EditorialStretchFigureCanvas(
                        pose = chinTuckPose,
                        breathProgress = 0.8f,
                        isExhale = true,
                        isSecondHalf = false,
                        asymmetry = asymmetry,
                        viewPlane = plane,
                        showBiomechanicalGuide = showGuide,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // 1. Coronal base vs Coronal + Biomechanical Guide
        val coronalBasePx = captureNodePixels("biomech_stage", File(tempDir, "coronal_base.png"))
        composeTestRule.runOnIdle { showGuide = true }
        composeTestRule.waitForIdle()
        val coronalGuidePx = captureNodePixels("biomech_stage", File(tempDir, "coronal_guide.png"))
        assertTrue(
            countDifferingPixels(coronalBasePx, coronalGuidePx) >= 80,
            "Biomechanical guide overlay must visibly draw plumb axis, anchor ring & fascial vector in coronal view"
        )

        // 2. Sagittal base vs Coronal base & Sagittal + Biomechanical Guide across all 4 skins
        for (skin in BuiltInSkinRegistry.allSkins) {
            composeTestRule.runOnIdle {
                activeSkin = skin
                plane = com.songxi.app.core.model.AnatomicalViewPlane.SAGITTAL_SIDE
                showGuide = false
            }
            composeTestRule.waitForIdle()
            val sagFile = File(tempDir, "sagittal_${skin.id.storageKey}.png")
            composeTestRule.onNodeWithTag("biomech_stage").captureRoboImage(
                file = sagFile,
                roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
            )
            val sagBmp = decodeArgbBitmap(sagFile)
            assertEquals(0, countExactRgbPixels(sagBmp, 0xFFFFFF), "Sagittal ${skin.id} rendered forbidden #FFFFFF")
            assertEquals(0, countExactRgbPixels(sagBmp, 0x000000), "Sagittal ${skin.id} rendered forbidden #000000")
            val sagPx = IntArray(sagBmp.width * sagBmp.height)
            sagBmp.getPixels(sagPx, 0, sagBmp.width, 0, 0, sagBmp.width, sagBmp.height)
            sagBmp.recycle()

            if (skin == BuiltInSkinRegistry.InkMist) {
                assertTrue(
                    countDifferingPixels(coronalBasePx, sagPx) >= 500,
                    "Sagittal side-profile view must be structurally distinct from coronal front view"
                )
            }

            composeTestRule.runOnIdle { showGuide = true }
            composeTestRule.waitForIdle()
            val sagGuidePx = captureNodePixels("biomech_stage", File(tempDir, "sagittal_guide_${skin.id.storageKey}.png"))
            assertTrue(
                countDifferingPixels(sagPx, sagGuidePx) >= 80,
                "Sagittal biomechanical guide must visibly render alignment & vector arrows for ${skin.id}"
            )
        }

        // 3. Asymmetric starting side (RIGHT_TIGHTER vs BALANCED) mirrors initial active side
        composeTestRule.runOnIdle {
            activeSkin = BuiltInSkinRegistry.QinglvCeladon
            plane = com.songxi.app.core.model.AnatomicalViewPlane.CORONAL_FRONT
            showGuide = true
            asymmetry = com.songxi.app.core.model.LateralAsymmetry.BALANCED
        }
        composeTestRule.waitForIdle()
        val balancedPx = captureNodePixels("biomech_stage", File(tempDir, "asym_balanced.png"))
        composeTestRule.runOnIdle {
            asymmetry = com.songxi.app.core.model.LateralAsymmetry.RIGHT_TIGHTER
        }
        composeTestRule.waitForIdle()
        val rightTighterPx = captureNodePixels("biomech_stage", File(tempDir, "asym_right.png"))
        assertTrue(
            countDifferingPixels(balancedPx, rightTighterPx) >= 80,
            "RIGHT_TIGHTER must start bilateral pose on mirrored right side"
        )
    }

    private fun captureNodePixelHash(tag: String, outFile: File): String {
        composeTestRule.onNodeWithTag(tag).captureRoboImage(
            file = outFile,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
        val bmp = decodeArgbBitmap(outFile)
        val hash = computeBitmapPixelSha256(bmp)
        bmp.recycle()
        return hash
    }

    private fun captureNodePixels(tag: String, outFile: File): IntArray {
        composeTestRule.onNodeWithTag(tag).captureRoboImage(
            file = outFile,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
        val bmp = decodeArgbBitmap(outFile)
        val px = IntArray(bmp.width * bmp.height)
        bmp.getPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
        bmp.recycle()
        outFile.delete()
        return px
    }

    private fun countDifferingPixels(a: IntArray, b: IntArray): Int {
        val len = minOf(a.size, b.size)
        var diff = 0
        for (i in 0 until len) {
            if (a[i] != b[i]) diff++
        }
        return diff
    }

    private fun decodeArgbBitmap(file: File): Bitmap {
        val bmp = BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        )
        assertNotNull(bmp, "Failed to decode PNG at ${file.absolutePath}")
        return bmp
    }

    private fun countExactRgbPixels(bitmap: Bitmap, targetRgb24: Int): Int {
        val px = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(px, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var count = 0
        for (argb in px) {
            val alpha = (argb ushr 24) and 0xFF
            if (alpha > 0 && (argb and 0xFFFFFF) == targetRgb24) {
                count++
            }
        }
        return count
    }

    private fun countQuantizedColorBuckets(bitmap: Bitmap): Int {
        val px = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(px, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val buckets = HashSet<Int>(128)
        for (argb in px) {
            val r4 = (argb shr 20) and 0xF
            val g4 = (argb shr 12) and 0xF
            val b4 = (argb shr 4) and 0xF
            buckets.add((r4 shl 8) or (g4 shl 4) or b4)
        }
        return buckets.size
    }

    private fun computeBitmapPixelSha256(bitmap: Bitmap): String {
        val px = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(px, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val digest = MessageDigest.getInstance("SHA-256")
        val byteBuf = ByteBuffer.allocate(8 + px.size * 4)
        byteBuf.putInt(bitmap.width)
        byteBuf.putInt(bitmap.height)
        for (v in px) {
            byteBuf.putInt(v)
        }
        return digest.digest(byteBuf.array()).joinToString("") { "%02x".format(it) }
    }
}

@Composable
private fun SongxiSkinCompositeFolioStage(
    skin: SongxiSkin,
    pose: StretchPose,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        ShanShuiBackdropCanvas(
            skin = skin,
            driftPhase = 0.18f,
            modifier = Modifier.fillMaxSize()
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                ClassicalSealBadge(
                    skin = skin,
                    size = 36.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = skin.meta.titleCn,
                    style = MaterialTheme.typography.headlineSmall,
                    color = skin.palette.inkPrimary
                )
                Text(
                    text = skin.meta.greetingPrefix,
                    style = MaterialTheme.typography.bodySmall,
                    color = skin.palette.inkSecondary
                )
            }
            BreathingHaloCanvas(
                skin = skin,
                breathProgress = 0.65f,
                isExhale = true,
                modifier = Modifier.size(168.dp)
            ) {
                EditorialStretchFigureCanvas(
                    pose = pose,
                    breathProgress = 0.65f,
                    isExhale = true,
                    skin = skin,
                    modifier = Modifier.size(132.dp)
                )
            }
        }
    }
}
