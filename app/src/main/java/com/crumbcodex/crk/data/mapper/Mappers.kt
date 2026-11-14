package com.crumbcodex.crk.data.mapper

import com.crumbcodex.crk.data.local.entity.CookieBuildEntity
import com.crumbcodex.crk.data.local.entity.CookieEntity
import com.crumbcodex.crk.data.local.entity.CookieWithBuilds
import com.crumbcodex.crk.data.local.entity.CouponEntity
import com.crumbcodex.crk.data.local.entity.DropItemEntity
import com.crumbcodex.crk.data.local.entity.OwnedCookieEntity
import com.crumbcodex.crk.data.local.entity.TeamEntity
import com.crumbcodex.crk.data.local.entity.TreasureEntity
import com.crumbcodex.crk.data.seed.BeascuitDto
import com.crumbcodex.crk.data.seed.BuildDto
import com.crumbcodex.crk.data.seed.CookieDto
import com.crumbcodex.crk.data.seed.DropItemDto
import com.crumbcodex.crk.data.seed.DropSourceDto
import com.crumbcodex.crk.data.seed.MagicCandyDto
import com.crumbcodex.crk.data.seed.SubstatDto
import com.crumbcodex.crk.data.seed.TartDto
import com.crumbcodex.crk.data.seed.ToppingSlotDto
import com.crumbcodex.crk.data.seed.TreasureDto
import com.crumbcodex.crk.domain.model.BeascuitGrade
import com.crumbcodex.crk.domain.model.BeascuitRecommendation
import com.crumbcodex.crk.domain.model.Build
import com.crumbcodex.crk.domain.model.CandyKind
import com.crumbcodex.crk.domain.model.Cookie
import com.crumbcodex.crk.domain.model.CookieClass
import com.crumbcodex.crk.domain.model.Coupon
import com.crumbcodex.crk.domain.model.DropCategory
import com.crumbcodex.crk.domain.model.DropItem
import com.crumbcodex.crk.domain.model.DropSource
import com.crumbcodex.crk.domain.model.DropSourceType
import com.crumbcodex.crk.domain.model.Efficiency
import com.crumbcodex.crk.domain.model.Element
import com.crumbcodex.crk.domain.model.GameMode
import com.crumbcodex.crk.domain.model.MagicCandyInfo
import com.crumbcodex.crk.domain.model.OwnedCookie
import com.crumbcodex.crk.domain.model.Position
import com.crumbcodex.crk.domain.model.Priority
import com.crumbcodex.crk.domain.model.Rarity
import com.crumbcodex.crk.domain.model.Skill
import com.crumbcodex.crk.domain.model.Stat
import com.crumbcodex.crk.domain.model.SubstatTarget
import com.crumbcodex.crk.domain.model.SynergyTag
import com.crumbcodex.crk.domain.model.TartRecommendation
import com.crumbcodex.crk.domain.model.Team
import com.crumbcodex.crk.domain.model.ToppingSlot
import com.crumbcodex.crk.domain.model.ToppingType
import com.crumbcodex.crk.domain.model.Treasure
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** Converts between seed DTOs, Room entities and domain models. */
class Mappers(private val json: Json) {

    private val toppingList = ListSerializer(ToppingSlotDto.serializer())
    private val substatList = ListSerializer(SubstatDto.serializer())
    private val sourceList = ListSerializer(DropSourceDto.serializer())
    private val statMap = MapSerializer(String.serializer(), Double.serializer())
    private val checklistMap = MapSerializer(String.serializer(), Boolean.serializer())

    // ---------- seed -> entity ----------

    fun toEntity(dto: CookieDto) = CookieEntity(
        id = dto.id,
        name = dto.name,
        title = dto.title,
        rarity = dto.rarity,
        cookieClass = dto.cookieClass,
        position = dto.position,
        elements = dto.elements,
        palette = dto.palette,
        imageUrl = dto.imageUrl?.takeIf { it.isNotBlank() },
        wikiUrl = dto.wikiUrl?.takeIf { it.isNotBlank() },
        skillName = dto.skill.name,
        skillDescription = dto.skill.description,
        skillCooldown = dto.skill.cooldown,
        magicCandyJson = json.encodeToString(MagicCandyDto.serializer(), dto.magicCandy),
    )

    fun toEntity(cookieId: String, dto: BuildDto) = CookieBuildEntity(
        id = "$cookieId:${dto.mode}",
        cookieId = cookieId,
        mode = dto.mode,
        title = dto.title,
        summary = dto.summary,
        toppingsJson = json.encodeToString(toppingList, dto.toppings),
        tartJson = dto.tart?.let { json.encodeToString(TartDto.serializer(), it) },
        substatsJson = json.encodeToString(substatList, dto.substats),
        beascuitJson = json.encodeToString(BeascuitDto.serializer(), dto.beascuit),
        notes = dto.notes,
    )

    fun toEntity(dto: TreasureDto) = TreasureEntity(dto.id, dto.name, dto.effect, dto.tags, dto.modes)

    fun toEntity(dto: DropItemDto) = DropItemEntity(
        id = dto.id,
        category = dto.category,
        name = dto.name,
        element = dto.element,
        description = dto.description,
        sourcesJson = json.encodeToString(sourceList, dto.sources),
    )

    // ---------- entity -> domain ----------

    fun toDomain(entity: CookieWithBuilds, owned: Boolean = false): Cookie {
        val c = entity.cookie
        val candy = json.decodeFromString(MagicCandyDto.serializer(), c.magicCandyJson)
        return Cookie(
            id = c.id,
            name = c.name,
            title = c.title,
            rarity = Rarity.valueOf(c.rarity),
            cookieClass = CookieClass.valueOf(c.cookieClass),
            position = Position.valueOf(c.position),
            elements = c.elements.map { Element.valueOf(it) },
            palette = c.palette.map { parseColor(it) },
            imageUrl = c.imageUrl,
            skill = Skill(c.skillName, c.skillDescription, c.skillCooldown),
            magicCandy = MagicCandyInfo(
                kind = CandyKind.valueOf(candy.kind),
                priority = Priority.valueOf(candy.priority),
                summary = candy.summary,
                effects = candy.effects,
                ingredients = candy.ingredients,
            ),
            builds = entity.builds.map { toDomain(it) }.sortedBy { it.mode.ordinal },
            isOwned = owned,
            wikiUrl = c.wikiUrl,
        )
    }

    private fun toDomain(b: CookieBuildEntity): Build {
        val beascuit = json.decodeFromString(BeascuitDto.serializer(), b.beascuitJson)
        return Build(
            mode = GameMode.valueOf(b.mode),
            title = b.title,
            summary = b.summary,
            toppings = decodeToppings(b.toppingsJson),
            tart = b.tartJson?.let { json.decodeFromString(TartDto.serializer(), it) }
                ?.let { TartRecommendation(it.name, Stat.valueOf(it.stat), it.note) },
            substats = json.decodeFromString(substatList, b.substatsJson).map {
                SubstatTarget(Stat.valueOf(it.stat), it.min, it.max, it.priority, it.note)
            },
            beascuit = BeascuitRecommendation(
                grade = BeascuitGrade.valueOf(beascuit.grade),
                element = beascuit.element?.let { Element.valueOf(it) },
                attunements = beascuit.attunements.map { Stat.valueOf(it) },
                note = beascuit.note,
            ),
            notes = b.notes,
        )
    }

    fun toDomain(e: OwnedCookieEntity) = OwnedCookie(
        cookieId = e.cookieId,
        level = e.level,
        stars = e.stars,
        ascension = e.ascension,
        power = e.power,
        skillLevel = e.skillLevel,
        candyLevel = e.candyLevel,
        toppings = decodeToppings(e.toppingsJson),
        tart = e.tart,
        beascuitGrade = e.beascuitGrade?.let { runCatching { BeascuitGrade.valueOf(it) }.getOrNull() },
        beascuitElement = e.beascuitElement?.let { runCatching { Element.valueOf(it) }.getOrNull() },
        currentStats = json.decodeFromString(statMap, e.currentStatsJson)
            .mapNotNull { (k, v) -> runCatching { Stat.valueOf(k) }.getOrNull()?.let { it to v } }
            .toMap(),
        checklist = json.decodeFromString(checklistMap, e.checklistJson),
        notes = e.notes,
        updatedAt = e.updatedAt,
    )

    fun toEntity(o: OwnedCookie) = OwnedCookieEntity(
        cookieId = o.cookieId,
        level = o.level,
        stars = o.stars,
        ascension = o.ascension,
        power = o.power,
        skillLevel = o.skillLevel,
        candyLevel = o.candyLevel,
        toppingsJson = json.encodeToString(toppingList, o.toppings.map { ToppingSlotDto(it.type.name, it.count) }),
        tart = o.tart,
        beascuitGrade = o.beascuitGrade?.name,
        beascuitElement = o.beascuitElement?.name,
        currentStatsJson = json.encodeToString(statMap, o.currentStats.mapKeys { it.key.name }),
        checklistJson = json.encodeToString(checklistMap, o.checklist),
        notes = o.notes,
        updatedAt = o.updatedAt,
    )

    fun toDomain(e: TeamEntity) = Team(
        id = e.id,
        name = e.name,
        mode = GameMode.valueOf(e.mode),
        slots = List(Team.SIZE) { i -> e.slots.getOrNull(i)?.takeIf { it.isNotEmpty() } },
        treasureIds = e.treasureIds,
        notes = e.notes,
        updatedAt = e.updatedAt,
    )

    fun toEntity(t: Team) = TeamEntity(
        id = t.id,
        name = t.name,
        mode = t.mode.name,
        slots = List(Team.SIZE) { i -> t.slots.getOrNull(i).orEmpty() },
        treasureIds = t.treasureIds,
        notes = t.notes,
        updatedAt = t.updatedAt,
    )

    fun toDomain(e: TreasureEntity) = Treasure(
        id = e.id,
        name = e.name,
        effect = e.effect,
        tags = e.tags.map { SynergyTag.valueOf(it) },
        modes = e.modes.map { GameMode.valueOf(it) },
    )

    fun toDomain(e: DropItemEntity) = DropItem(
        id = e.id,
        category = DropCategory.valueOf(e.category),
        name = e.name,
        element = e.element?.let { Element.valueOf(it) },
        description = e.description,
        sources = json.decodeFromString(sourceList, e.sourcesJson).map {
            DropSource(it.name, DropSourceType.valueOf(it.type), it.detail, Efficiency.valueOf(it.efficiency))
        },
    )

    fun toDomain(e: CouponEntity) = Coupon(
        code = e.code,
        rewards = e.rewards,
        expiresAt = e.expiresAt,
        isVerified = e.verified,
        isUserAdded = e.userAdded,
        isRedeemed = e.redeemed,
        addedAt = e.addedAt,
    )

    private fun decodeToppings(raw: String): List<ToppingSlot> =
        json.decodeFromString(toppingList, raw).mapNotNull { dto ->
            runCatching { ToppingType.valueOf(dto.type) }.getOrNull()?.let { ToppingSlot(it, dto.count) }
        }

    companion object {
        /** Parses "#RRGGBB" or "#AARRGGBB" into an ARGB long. */
        fun parseColor(hex: String): Long {
            val clean = hex.removePrefix("#")
            val value = clean.toLong(16)
            return if (clean.length == 6) value or 0xFF000000 else value
        }
    }
}
