package com.crumbcodex.crk.domain.model

enum class Rarity(val label: String, val sortOrder: Int) {
    BEAST("Beast", 0),
    ANCIENT("Ancient", 1),
    LEGENDARY("Legendary", 2),
    DRAGON("Dragon", 3),
    SUPER_EPIC("Super Epic", 4),
    EPIC("Epic", 5),
    RARE("Rare", 6),
    COMMON("Common", 7),
    SPECIAL("Special", 8),
}

enum class CookieClass(val label: String) {
    BOMBER("Bomber"),
    CHARGE("Charge"),
    AMBUSH("Ambush"),
    MAGIC("Magic"),
    RANGED("Ranged"),
    DEFENSE("Defense"),
    SUPPORT("Support"),
    HEALING("Healing");

    val isDamageDealer: Boolean
        get() = this in setOf(BOMBER, CHARGE, AMBUSH, MAGIC, RANGED)
}

enum class Position(val label: String, val sortOrder: Int) {
    FRONT("Front", 0),
    MIDDLE("Middle", 1),
    REAR("Rear", 2),
}

enum class Element(val label: String) {
    POISON("Poison"),
    STEEL("Steel"),
    FIRE("Fire"),
    WATER("Water"),
    EARTH("Earth"),
    LIGHT("Light"),
    DARKNESS("Darkness"),
    ICE("Ice"),
    ELECTRICITY("Electricity"),
    WIND("Wind"),
}

enum class GameMode(val label: String, val shortLabel: String) {
    ARENA("Arena (PvP)", "PvP"),
    WORLD_EXPLORATION("World Exploration (PvE)", "PvE"),
    GUILD_BOSS("Guild Boss", "Guild"),
}

enum class Stat(val label: String, val isPercent: Boolean = true) {
    ATK("ATK"),
    ATK_SPD("ATK SPD"),
    CRIT("CRIT%"),
    COOLDOWN("Cooldown"),
    DMG_RESIST("DMG Resist"),
    HP("HP"),
    DEF("DEF"),
    DEBUFF_RESIST("Debuff Resist"),
    AMPLIFY_BUFF("Amplify Buff"),
    CRIT_RESIST("CRIT Resist"),
    DMG_RESIST_BYPASS("DMG Resist Bypass"),
    ELEMENTAL_DMG("Elemental DMG"),
    HEALING("Healing"),
}

/** Topping sets. Each maps to the main stat it rolls. */
enum class ToppingType(val label: String, val mainStat: Stat, val colorArgb: Long) {
    SEARING_RASPBERRY("Searing Raspberry", Stat.ATK, 0xFFE5486A),
    SWIFT_CHOCOLATE("Swift Chocolate", Stat.COOLDOWN, 0xFF8A5A44),
    SOLID_ALMOND("Solid Almond", Stat.DMG_RESIST, 0xFFE2C29F),
    JUICY_APPLE_JELLY("Juicy Apple Jelly", Stat.CRIT, 0xFFF4C542),
    BOUNCY_CARAMEL("Bouncy Caramel", Stat.ATK_SPD, 0xFFF29E4C),
    HEALTHY_PEANUT("Healthy Peanut", Stat.HP, 0xFFC9925A),
    HARD_WALNUT("Hard Walnut", Stat.DEF, 0xFF8C6A4F),
}

enum class BeascuitGrade(val label: String, val sortOrder: Int) {
    LEGENDARY("Legendary Beascuit", 0),
    EPIC("Epic Beascuit", 1),
    RARE("Rare Beascuit", 2),
    COMMON("Common Beascuit", 3),
}

enum class CandyKind(val label: String) {
    MAGIC_CANDY("Magic Candy"),
    CRYSTAL_JAM("Crystal Jam"),
    NONE("Not available"),
}

enum class Priority(val label: String, val weight: Int) {
    HIGH("High", 3),
    MEDIUM("Medium", 2),
    LOW("Low", 1),
    SKIP("Skip for now", 0),
}

enum class DropCategory(val label: String) {
    BEASCUIT("Beascuits"),
    MAGIC_CANDY("Magic Candy"),
    CRYSTAL_JAM("Crystal Jam"),
    TOPPING("Toppings"),
}

enum class DropSourceType(val label: String) {
    BEAST_YEAST("Beast-Yeast"),
    HARD_MODE("Hard Mode"),
    MONSTER_MENACE("Monster Menace"),
    BEAST_RAID("Beast Raid"),
    GUILD_BATTLE("Guild Battle"),
    SHOP("Shop / Exchange"),
    EVENT("Event"),
    BOUNTY("Bounty"),
}

enum class Efficiency(val label: String) {
    BEST("Best"),
    GOOD("Good"),
    OK("OK"),
}

/** Tags used to match treasures to a team's needs. */
enum class SynergyTag(val label: String) {
    BURST("Burst damage"),
    COOLDOWN("Cooldown"),
    SUSTAIN("Healing / sustain"),
    TANK("Frontline survival"),
    CRIT("Critical hits"),
    REVIVE("Revive"),
    DEBUFF("Debuff / control"),
    SHIELD("Shields"),
}
