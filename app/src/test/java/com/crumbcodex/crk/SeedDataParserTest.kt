package com.crumbcodex.crk

import com.crumbcodex.crk.data.seed.SeedDataParser
import com.crumbcodex.crk.data.seed.SeedValidationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Validates the real bundled seed files, so a bad JSON edit fails the build's unit tests. */
class SeedDataParserTest {
    private val parser = SeedDataParser()
    private fun asset(name: String) = File("src/main/assets/seed/$name").readText()

    @Test fun `bundled cookies parse and validate`() {
        val file = parser.parseCookies(asset("cookies.json"))
        assertTrue(file.cookies.size >= 15)
        assertTrue(file.cookies.any { it.id == "shadow_milk" })
        file.cookies.forEach { c -> c.builds.forEach { b -> assertEquals(5, b.toppings.sumOf { it.count }) } }
    }

    @Test fun `bundled game data references known cookies`() {
        val cookies = parser.parseCookies(asset("cookies.json"))
        val game = parser.parseGameData(asset("game_data.json"), cookies.cookies.map { it.id }.toSet())
        assertEquals(3, game.tierLists.size)
        assertTrue(game.drops.isNotEmpty())
        assertTrue(game.treasures.isNotEmpty())
    }

    @Test(expected = SeedValidationException::class)
    fun `invalid enum is rejected`() {
        parser.parseCookies(
            """{"version":1,"cookies":[{"id":"a","name":"A","rarity":"MYTHIC","class":"MAGIC","position":"FRONT",
               "skill":{"name":"s","description":"d"}}]}""",
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `toppings must total five`() {
        parser.parseCookies(
            """{"version":1,"cookies":[{"id":"a","name":"A","rarity":"EPIC","class":"MAGIC","position":"FRONT",
               "skill":{"name":"s","description":"d"},
               "builds":[{"mode":"ARENA","title":"t","toppings":[{"type":"SWIFT_CHOCOLATE","count":4}],
               "beascuit":{"grade":"EPIC"}}]}]}""",
        )
    }

    @Test fun `expiry is end of day utc`() {
        assertEquals(1735689599999L, SeedDataParser.parseExpiry("2024-12-31"))
    }
}
