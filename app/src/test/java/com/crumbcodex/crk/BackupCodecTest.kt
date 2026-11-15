package com.crumbcodex.crk

import com.crumbcodex.crk.data.backup.BackupCodec
import com.crumbcodex.crk.data.backup.BackupFile
import com.crumbcodex.crk.data.backup.BackupFormatException
import com.crumbcodex.crk.data.local.entity.CouponEntity
import com.crumbcodex.crk.data.local.entity.OwnedCookieEntity
import com.crumbcodex.crk.data.local.entity.TeamEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupCodecTest {
    private val codec = BackupCodec()
    private val sample = BackupFile(
        exportedAt = 123L,
        owned = listOf(
            OwnedCookieEntity("shadow_milk", 70, 3, 1, 250000, 60, 10, "[]", "Chocolate Tart", "LEGENDARY", "DARKNESS", "{\"COOLDOWN\":24.5}", "{\"Reroll\":false}", "notes", 9L),
        ),
        teams = listOf(TeamEntity(7, "Arena main", "ARENA", listOf("a", "b", "", "", "c"), listOf("watch"), "", 5L)),
        coupons = listOf(CouponEntity("ABC123", "Crystals", null, false, true, true, 1L)),
    )

    @Test fun `round trip keeps everything`() {
        assertEquals(sample, codec.decode(codec.encode(sample)))
    }

    @Test(expected = BackupFormatException::class)
    fun `rejects random json`() { codec.decode("""{"hello":"world"}""") }

    @Test(expected = BackupFormatException::class)
    fun `rejects other apps`() { codec.decode(codec.encode(sample.copy(app = "something-else"))) }

    @Test(expected = BackupFormatException::class)
    fun `rejects newer format`() { codec.decode(codec.encode(sample.copy(format = 99))) }

    @Test(expected = BackupFormatException::class)
    fun `rejects malformed team`() {
        codec.decode(codec.encode(sample.copy(teams = listOf(sample.teams[0].copy(slots = listOf("a"))))))
    }

    @Test(expected = BackupFormatException::class)
    fun `rejects non json`() { codec.decode("not json at all") }
}
