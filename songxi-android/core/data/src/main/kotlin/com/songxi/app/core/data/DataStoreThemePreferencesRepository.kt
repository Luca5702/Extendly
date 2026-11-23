package com.songxi.app.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.songxi.app.core.model.SkinId
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class DataStoreThemePreferencesRepository(
    private val dataStore: DataStore<Preferences>
) : ThemePreferencesRepository {

    private val safePreferences: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) {
            emit(emptyPreferences())
        } else {
            throw error
        }
    }

    override val selectedSkinId: Flow<SkinId> = safePreferences.map { prefs ->
        SkinId.fromStorageKey(prefs[KEY_SELECTED_SKIN_ID])
    }

    override val autoNightSkinEnabled: Flow<Boolean> = safePreferences.map { prefs ->
        prefs[KEY_AUTO_NIGHT_SKIN_ENABLED] ?: false
    }

    override val onboardingSeen: Flow<Boolean> = safePreferences.map { prefs ->
        prefs[KEY_ONBOARDING_SEEN] ?: false
    }

    override val voiceGuidanceEnabled: Flow<Boolean> = safePreferences.map { prefs ->
        prefs[KEY_VOICE_GUIDANCE_ENABLED] ?: false
    }

    override val prepBufferEnabled: Flow<Boolean> = safePreferences.map { prefs ->
        prefs[KEY_PREP_BUFFER_ENABLED] ?: false
    }

    override suspend fun setSelectedSkin(skinId: SkinId) {
        dataStore.edit { prefs ->
            prefs[KEY_SELECTED_SKIN_ID] = skinId.storageKey
        }
    }

    override suspend fun setAutoNightSkin(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_AUTO_NIGHT_SKIN_ENABLED] = enabled
        }
    }

    override suspend fun markOnboardingSeen(seen: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_SEEN] = seen
        }
    }

    override suspend fun setVoiceGuidanceEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_VOICE_GUIDANCE_ENABLED] = enabled
        }
    }

    override suspend fun setPrepBufferEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_PREP_BUFFER_ENABLED] = enabled
        }
    }

    override suspend fun resetAll() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_SELECTED_SKIN_ID)
            prefs.remove(KEY_AUTO_NIGHT_SKIN_ENABLED)
            prefs.remove(KEY_ONBOARDING_SEEN)
            prefs.remove(KEY_VOICE_GUIDANCE_ENABLED)
            prefs.remove(KEY_PREP_BUFFER_ENABLED)
        }
    }

    private companion object {
        val KEY_SELECTED_SKIN_ID = stringPreferencesKey("theme.selected_skin_id")
        val KEY_AUTO_NIGHT_SKIN_ENABLED = booleanPreferencesKey("theme.auto_night_skin_enabled")
        val KEY_ONBOARDING_SEEN = booleanPreferencesKey("theme.onboarding_seen")
        val KEY_VOICE_GUIDANCE_ENABLED = booleanPreferencesKey("theme.voice_guidance_enabled")
        val KEY_PREP_BUFFER_ENABLED = booleanPreferencesKey("theme.prep_buffer_enabled")
    }
}
