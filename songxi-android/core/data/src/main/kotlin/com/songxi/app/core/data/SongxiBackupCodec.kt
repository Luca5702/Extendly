package com.songxi.app.core.data

import com.songxi.app.core.model.PracticeSessionRecord
import com.songxi.app.core.model.ReminderPreferences
import com.songxi.app.core.model.SavedPrescriptionPreset
import com.songxi.app.core.model.SkinId
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Portable, 100% offline local sanctuary backup archive ("松息本地手札归档").
 * Zero cloud accounts, zero network telemetry — empowers device migration safely.
 */
@Serializable
data class SongxiSanctuaryBackupSnapshot(
    val appIdentifier: String = APP_IDENTIFIER,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAtIso: String,
    val selectedSkinId: SkinId = SkinId.DEFAULT,
    val autoNightSkinEnabled: Boolean = true,
    val voiceGuidanceEnabled: Boolean = false,
    val prepBufferEnabled: Boolean = false,
    val reminderPreferences: ReminderPreferences = ReminderPreferences(),
    val bookletCompletedDays: Map<String, Set<Int>> = emptyMap(),
    val savedPresets: List<SavedPrescriptionPreset> = emptyList(),
    val journalRecords: List<PracticeSessionRecord> = emptyList()
) {
    companion object {
        const val APP_IDENTIFIER: String = "com.songxi.app.backup"
        const val CURRENT_SCHEMA_VERSION: Int = 1
    }
}

object SongxiBackupCodec {

    private val prettyBackupJson: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun encodeSnapshot(snapshot: SongxiSanctuaryBackupSnapshot): String =
        prettyBackupJson.encodeToString(snapshot)

    fun decodeSnapshot(rawJson: String): Result<SongxiSanctuaryBackupSnapshot> {
        val trimmed = rawJson.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("备份内容为空，请粘贴或选择有效的松息备份文件。"))
        }
        return runCatching {
            val parsed = prettyBackupJson.decodeFromString<SongxiSanctuaryBackupSnapshot>(trimmed)
            require(parsed.appIdentifier == SongxiSanctuaryBackupSnapshot.APP_IDENTIFIER) {
                "非松息手札归档格式（${parsed.appIdentifier}）。"
            }
            require(parsed.schemaVersion in 1..SongxiSanctuaryBackupSnapshot.CURRENT_SCHEMA_VERSION) {
                "备份版本暂不支持（v${parsed.schemaVersion}）。"
            }
            parsed
        }
    }
}
