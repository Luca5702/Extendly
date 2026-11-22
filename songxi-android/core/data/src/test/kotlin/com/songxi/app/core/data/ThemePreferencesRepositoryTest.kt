package com.songxi.app.core.data

import com.songxi.app.core.model.SkinId
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemePreferencesRepositoryTest {

    private fun newTempStoreFile(): File {
        val baseTmp = File(System.getProperty("java.io.tmpdir") ?: error("java.io.tmpdir unset"))
        return File(baseTmp, "songxi_theme_test/theme_${UUID.randomUUID()}.preferences_pb")
    }

    @Test
    fun allBuiltInSkinsAndSettingsPersistAndResetCleanly() = runTest(StandardTestDispatcher()) {
        val file = newTempStoreFile()
        val store = SongxiDataStoreFactory.createPreferenceDataStore(file, backgroundScope)
        val repo = DataStoreThemePreferencesRepository(store)

        assertEquals(SkinId.INK_MIST, repo.selectedSkinId.first())
        assertFalse(repo.autoNightSkinEnabled.first())
        assertFalse(repo.onboardingSeen.first())

        for (skin in SkinId.entries) {
            repo.setSelectedSkin(skin)
            assertEquals(skin, repo.selectedSkinId.first())
        }

        repo.setAutoNightSkin(true)
        repo.markOnboardingSeen(true)

        val reloadedRepo = DataStoreThemePreferencesRepository(store)
        assertEquals(SkinId.SPRING_BLOSSOM, reloadedRepo.selectedSkinId.first())
        assertTrue(reloadedRepo.autoNightSkinEnabled.first())
        assertTrue(reloadedRepo.onboardingSeen.first())

        reloadedRepo.resetAll()
        assertEquals(SkinId.INK_MIST, reloadedRepo.selectedSkinId.first())
        assertFalse(reloadedRepo.autoNightSkinEnabled.first())
        assertFalse(reloadedRepo.onboardingSeen.first())
    }
}
