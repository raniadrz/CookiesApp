package com.crumbcodex.crk

import com.crumbcodex.crk.data.seed.SeedSyncPlanner
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedSyncPlannerTest {
    @Test fun `first launch imports everything`() {
        val p = SeedSyncPlanner.plan(null, null, 3, 1, hasCookies = false)
        assertTrue(p.importCookies); assertTrue(p.importGameData)
    }

    @Test fun `same versions do nothing`() {
        assertTrue(SeedSyncPlanner.plan(3, 1, 3, 1, hasCookies = true).isNoop)
    }

    @Test fun `older bundled data never overwrites newer downloaded data`() {
        assertTrue(SeedSyncPlanner.plan(5, 2, 3, 1, hasCookies = true).isNoop)
    }

    @Test fun `files update independently`() {
        val p = SeedSyncPlanner.plan(3, 1, 3, 2, hasCookies = true)
        assertFalse(p.importCookies); assertTrue(p.importGameData)
        val q = SeedSyncPlanner.plan(3, 1, 4, 1, hasCookies = true)
        assertTrue(q.importCookies); assertFalse(q.importGameData)
    }

    @Test fun `empty cookie table is repaired`() {
        assertTrue(SeedSyncPlanner.plan(3, 1, 3, 1, hasCookies = false).importCookies)
    }
}
