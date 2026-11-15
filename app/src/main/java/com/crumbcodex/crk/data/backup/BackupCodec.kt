package com.crumbcodex.crk.data.backup

import com.crumbcodex.crk.data.local.entity.CouponEntity
import com.crumbcodex.crk.data.local.entity.OwnedCookieEntity
import com.crumbcodex.crk.data.local.entity.TeamEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BackupFile(
    val app: String = APP_ID,
    val format: Int = FORMAT,
    val exportedAt: Long,
    val owned: List<OwnedCookieEntity> = emptyList(),
    val teams: List<TeamEntity> = emptyList(),
    val coupons: List<CouponEntity> = emptyList(),
) {
    companion object {
        const val APP_ID = "crumb-codex"
        const val FORMAT = 1
    }
}

class BackupFormatException(message: String) : IllegalArgumentException(message)

/** Encodes and validates backup files. Pure JVM; unit-tested. */
class BackupCodec(
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true },
) {
    fun encode(file: BackupFile): String = json.encodeToString(BackupFile.serializer(), file)

    fun decode(raw: String): BackupFile {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), raw)
        } catch (e: Exception) {
            throw BackupFormatException("This isn't a Crumb Codex backup file")
        }
        if (file.app != BackupFile.APP_ID) throw BackupFormatException("This isn't a Crumb Codex backup file")
        if (file.format > BackupFile.FORMAT) throw BackupFormatException("Backup is from a newer app version, so please update the app first")
        file.teams.forEach { t ->
            if (t.slots.size != 5) throw BackupFormatException("Backup contains a malformed team (${t.name})")
        }
        file.owned.forEach { o ->
            if (o.cookieId.isBlank() || o.level !in 1..999) throw BackupFormatException("Backup contains a malformed cookie entry")
        }
        return file
    }
}
