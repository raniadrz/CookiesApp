package com.crumbcodex.crk.data.seed

import com.crumbcodex.crk.domain.model.BeascuitGrade
import com.crumbcodex.crk.domain.model.CandyKind
import com.crumbcodex.crk.domain.model.CookieClass
import com.crumbcodex.crk.domain.model.DropCategory
import com.crumbcodex.crk.domain.model.DropSourceType
import com.crumbcodex.crk.domain.model.Efficiency
import com.crumbcodex.crk.domain.model.Element
import com.crumbcodex.crk.domain.model.GameMode
import com.crumbcodex.crk.domain.model.Position
import com.crumbcodex.crk.domain.model.Priority
import com.crumbcodex.crk.domain.model.Rarity
import com.crumbcodex.crk.domain.model.Stat
import com.crumbcodex.crk.domain.model.SynergyTag
import com.crumbcodex.crk.domain.model.ToppingType
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.ZoneOffset

class SeedValidationException(message: String) : IllegalArgumentException(message)

/**
 * Parses and validates the bundled JSON seed files. Pure JVM code (takes raw strings) so it
 * can be unit-tested against the real asset files.
 */
class SeedDataParser(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    },
) {
    fun parseCookies(raw: String): CookieSeedFile {
        val file = json.decodeFromString(CookieSeedFile.serializer(), raw)
        val ids = HashSet<String>()
        file.cookies.forEach { c ->
            require(ids.add(c.id)) { "Duplicate cookie id '${c.id}'" }
            enumCheck<Rarity>(c.rarity, c.id, "rarity")
            enumCheck<CookieClass>(c.cookieClass, c.id, "class")
            enumCheck<Position>(c.position, c.id, "position")
            c.elements.forEach { enumCheck<Element>(it, c.id, "element") }
            enumCheck<CandyKind>(c.magicCandy.kind, c.id, "magicCandy.kind")
            enumCheck<Priority>(c.magicCandy.priority, c.id, "magicCandy.priority")
            c.palette.forEach { hex -> require(HEX.matches(hex)) { "${c.id}: invalid palette color '$hex'" } }
            val modes = HashSet<String>()
            c.builds.forEach { b ->
                enumCheck<GameMode>(b.mode, c.id, "build.mode")
                require(modes.add(b.mode)) { "${c.id}: duplicate build for ${b.mode}" }
                val pieces = b.toppings.sumOf { it.count }
                require(pieces == 5) { "${c.id}/${b.mode}: toppings must total 5 pieces (got $pieces)" }
                b.toppings.forEach { enumCheck<ToppingType>(it.type, c.id, "topping") }
                b.tart?.let { enumCheck<Stat>(it.stat, c.id, "tart.stat") }
                b.substats.forEach { enumCheck<Stat>(it.stat, c.id, "substat") }
                enumCheck<BeascuitGrade>(b.beascuit.grade, c.id, "beascuit.grade")
                b.beascuit.element?.let { enumCheck<Element>(it, c.id, "beascuit.element") }
                b.beascuit.attunements.forEach { enumCheck<Stat>(it, c.id, "beascuit.attunement") }
            }
        }
        return file
    }

    fun parseGameData(raw: String, knownCookieIds: Set<String>): GameDataSeedFile {
        val file = json.decodeFromString(GameDataSeedFile.serializer(), raw)
        file.treasures.forEach { t ->
            t.tags.forEach { enumCheck<SynergyTag>(it, t.id, "treasure.tag") }
            t.modes.forEach { enumCheck<GameMode>(it, t.id, "treasure.mode") }
        }
        file.drops.forEach { d ->
            enumCheck<DropCategory>(d.category, d.id, "drop.category")
            d.element?.let { enumCheck<Element>(it, d.id, "drop.element") }
            d.sources.forEach {
                enumCheck<DropSourceType>(it.type, d.id, "source.type")
                enumCheck<Efficiency>(it.efficiency, d.id, "source.efficiency")
            }
        }
        file.tierLists.forEach { (mode, groups) ->
            enumCheck<GameMode>(mode, "tierLists", "mode")
            groups.flatMap { it.cookies }.forEach { id ->
                require(id in knownCookieIds) { "Tier list $mode references unknown cookie '$id'" }
            }
        }
        file.coupons.forEach { c -> c.expires?.let { parseExpiry(it) } }
        return file
    }

    companion object {
        private val HEX = Regex("^#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$")

        /** End of the given day in UTC, in epoch millis. */
        fun parseExpiry(date: String): Long =
            LocalDate.parse(date).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() - 1

        inline fun <reified T : Enum<T>> enumCheck(value: String, owner: String, field: String) {
            if (enumValues<T>().none { it.name == value }) {
                throw SeedValidationException("$owner: invalid $field '$value' (allowed: ${enumValues<T>().joinToString { it.name }})")
            }
        }
    }
}
