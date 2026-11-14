package com.crumbcodex.crk.data.seed

data class SeedPlan(val importCookies: Boolean, val importGameData: Boolean) {
    val isNoop: Boolean get() = !importCookies && !importGameData
}

/**
 * Decides which seed files to import. Each file has its own version; a file is imported only when it is
 * newer than what is stored (or nothing is stored yet), so an older bundled file never overwrites newer
 * data that was downloaded over the air.
 */
object SeedSyncPlanner {
    fun plan(
        storedCookies: Int?,
        storedGameData: Int?,
        newCookies: Int,
        newGameData: Int,
        hasCookies: Boolean,
    ) = SeedPlan(
        importCookies = !hasCookies || storedCookies == null || newCookies > storedCookies,
        importGameData = storedGameData == null || newGameData > storedGameData,
    )
}
