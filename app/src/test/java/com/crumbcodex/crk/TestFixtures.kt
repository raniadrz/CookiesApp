package com.crumbcodex.crk

import com.crumbcodex.crk.domain.model.BeascuitGrade
import com.crumbcodex.crk.domain.model.BeascuitRecommendation
import com.crumbcodex.crk.domain.model.Build
import com.crumbcodex.crk.domain.model.CandyKind
import com.crumbcodex.crk.domain.model.Cookie
import com.crumbcodex.crk.domain.model.CookieClass
import com.crumbcodex.crk.domain.model.Element
import com.crumbcodex.crk.domain.model.GameMode
import com.crumbcodex.crk.domain.model.MagicCandyInfo
import com.crumbcodex.crk.domain.model.Position
import com.crumbcodex.crk.domain.model.Priority
import com.crumbcodex.crk.domain.model.Rarity
import com.crumbcodex.crk.domain.model.Skill
import com.crumbcodex.crk.domain.model.Stat
import com.crumbcodex.crk.domain.model.SubstatTarget
import com.crumbcodex.crk.domain.model.ToppingSlot
import com.crumbcodex.crk.domain.model.ToppingType

fun cookie(
    id: String,
    cls: CookieClass = CookieClass.MAGIC,
    pos: Position = Position.MIDDLE,
    rarity: Rarity = Rarity.EPIC,
    elements: List<Element> = listOf(Element.FIRE),
    owned: Boolean = false,
    candyPriority: Priority = Priority.SKIP,
    builds: List<Build> = emptyList(),
) = Cookie(
    id = id, name = "${id.replaceFirstChar { it.uppercase() }} Cookie", title = "", rarity = rarity,
    cookieClass = cls, position = pos, elements = elements, palette = listOf(0xFFFFFFFF),
    imageUrl = null, skill = Skill("Skill of $id", "", 10),
    magicCandy = MagicCandyInfo(if (candyPriority == Priority.SKIP) CandyKind.NONE else CandyKind.MAGIC_CANDY, candyPriority, "", emptyList(), emptyList()),
    builds = builds, isOwned = owned,
)

fun arenaBuild() = Build(
    mode = GameMode.ARENA, title = "t", summary = "",
    toppings = listOf(ToppingSlot(ToppingType.SWIFT_CHOCOLATE, 5)),
    tart = null,
    substats = listOf(SubstatTarget(Stat.COOLDOWN, 25.0, 29.5, 1, null), SubstatTarget(Stat.ATK, 10.0, null, 2, null)),
    beascuit = BeascuitRecommendation(BeascuitGrade.LEGENDARY, Element.FIRE, listOf(Stat.ATK), ""),
    notes = emptyList(),
)
