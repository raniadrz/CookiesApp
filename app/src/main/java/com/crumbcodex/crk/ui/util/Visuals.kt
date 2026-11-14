package com.crumbcodex.crk.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowRight
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SportsMartialArts
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material.icons.rounded.Hardware
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.crumbcodex.crk.domain.model.CookieClass
import com.crumbcodex.crk.domain.model.DropSourceType
import com.crumbcodex.crk.domain.model.Efficiency
import com.crumbcodex.crk.domain.model.Element
import com.crumbcodex.crk.domain.model.GameMode
import com.crumbcodex.crk.domain.model.Position
import com.crumbcodex.crk.domain.model.Priority
import com.crumbcodex.crk.domain.model.Rarity
import com.crumbcodex.crk.ui.theme.StatusBad
import com.crumbcodex.crk.ui.theme.StatusGood
import com.crumbcodex.crk.ui.theme.StatusWarn

val Element.color: Color
    get() = when (this) {
        Element.POISON -> Color(0xFF9BCF53)
        Element.STEEL -> Color(0xFF9AA6B2)
        Element.FIRE -> Color(0xFFFF7A59)
        Element.WATER -> Color(0xFF4FA3F7)
        Element.EARTH -> Color(0xFFB08A5B)
        Element.LIGHT -> Color(0xFFFFCF4D)
        Element.DARKNESS -> Color(0xFF7A62B0)
        Element.ICE -> Color(0xFF7FD6F5)
        Element.ELECTRICITY -> Color(0xFFF5C542)
        Element.WIND -> Color(0xFF6FD3A8)
    }

val Element.icon: ImageVector
    get() = when (this) {
        Element.POISON -> Icons.Rounded.Science
        Element.STEEL -> Icons.Rounded.Hardware
        Element.FIRE -> Icons.Rounded.LocalFireDepartment
        Element.WATER -> Icons.Rounded.WaterDrop
        Element.EARTH -> Icons.Rounded.Terrain
        Element.LIGHT -> Icons.Rounded.WbSunny
        Element.DARKNESS -> Icons.Rounded.DarkMode
        Element.ICE -> Icons.Rounded.AcUnit
        Element.ELECTRICITY -> Icons.Rounded.Bolt
        Element.WIND -> Icons.Rounded.Air
    }

val CookieClass.icon: ImageVector
    get() = when (this) {
        CookieClass.BOMBER -> Icons.Rounded.Whatshot
        CookieClass.CHARGE -> Icons.Rounded.KeyboardDoubleArrowRight
        CookieClass.AMBUSH -> Icons.Rounded.VisibilityOff
        CookieClass.MAGIC -> Icons.Rounded.AutoFixHigh
        CookieClass.RANGED -> Icons.Rounded.GpsFixed
        CookieClass.DEFENSE -> Icons.Rounded.Shield
        CookieClass.SUPPORT -> Icons.Rounded.VolunteerActivism
        CookieClass.HEALING -> Icons.Rounded.Healing
    }

val CookieClass.color: Color
    get() = when (this) {
        CookieClass.BOMBER -> Color(0xFFFF8A65)
        CookieClass.CHARGE -> Color(0xFFE5486A)
        CookieClass.AMBUSH -> Color(0xFF8E7CC3)
        CookieClass.MAGIC -> Color(0xFFB07CFF)
        CookieClass.RANGED -> Color(0xFF4FA3F7)
        CookieClass.DEFENSE -> Color(0xFF6C8EA4)
        CookieClass.SUPPORT -> Color(0xFFF2C14E)
        CookieClass.HEALING -> Color(0xFF3DBE8B)
    }

val Rarity.color: Color
    get() = when (this) {
        Rarity.BEAST -> Color(0xFFE0457B)
        Rarity.ANCIENT -> Color(0xFFF2C14E)
        Rarity.LEGENDARY -> Color(0xFFA27BFF)
        Rarity.DRAGON -> Color(0xFFFF6B4A)
        Rarity.SUPER_EPIC -> Color(0xFFFF8FC7)
        Rarity.EPIC -> Color(0xFFC77DFF)
        Rarity.RARE -> Color(0xFF5EA8FF)
        Rarity.COMMON -> Color(0xFFA0A0A8)
        Rarity.SPECIAL -> Color(0xFF4ECDC4)
    }

val Position.color: Color
    get() = when (this) {
        Position.FRONT -> Color(0xFFE5486A)
        Position.MIDDLE -> Color(0xFFB07CFF)
        Position.REAR -> Color(0xFF4FA3F7)
    }

val GameMode.icon: ImageVector
    get() = when (this) {
        GameMode.ARENA -> Icons.Rounded.SportsMartialArts
        GameMode.WORLD_EXPLORATION -> Icons.Rounded.Explore
        GameMode.GUILD_BOSS -> Icons.Rounded.Groups
    }

val DropSourceType.icon: ImageVector
    get() = when (this) {
        DropSourceType.BEAST_YEAST -> Icons.Rounded.Explore
        DropSourceType.HARD_MODE -> Icons.Rounded.TrackChanges
        DropSourceType.MONSTER_MENACE -> Icons.Rounded.Pets
        DropSourceType.BEAST_RAID -> Icons.Rounded.EmojiEvents
        DropSourceType.GUILD_BATTLE -> Icons.Rounded.Groups
        DropSourceType.SHOP -> Icons.Rounded.Storefront
        DropSourceType.EVENT -> Icons.Rounded.CalendarMonth
        DropSourceType.BOUNTY -> Icons.Rounded.ShoppingBag
    }

val Efficiency.color: Color
    get() = when (this) {
        Efficiency.BEST -> StatusGood
        Efficiency.GOOD -> StatusWarn
        Efficiency.OK -> Color(0xFF9AA6B2)
    }

val Priority.color: Color
    get() = when (this) {
        Priority.HIGH -> StatusBad
        Priority.MEDIUM -> StatusWarn
        Priority.LOW -> StatusGood
        Priority.SKIP -> Color(0xFF9AA6B2)
    }

fun tierColor(tier: String): Color = when (tier.uppercase()) {
    "SS", "S+" -> Color(0xFFE0457B)
    "S" -> Color(0xFFFF8FC7)
    "A" -> Color(0xFFA27BFF)
    "B" -> Color(0xFF5EA8FF)
    "C" -> Color(0xFF3DBE8B)
    else -> Color(0xFF9AA6B2)
}

fun List<Long>.toColors(): List<Color> = map { Color(it) }
