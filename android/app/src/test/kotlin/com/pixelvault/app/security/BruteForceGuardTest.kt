package com.pixelvault.app.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BruteForceGuardTest {

    private lateinit var guard: BruteForceGuard

    @Before
    fun setUp() {
        guard = BruteForceGuard()
    }

    @Test
    fun testInitialStateNotLockedOut() {
        assertFalse(guard.isLockedOut("test_user"))
        assertEquals(0, guard.getRemainingLockoutSeconds("test_user"))
    }

    @Test
    fun testLockoutTriggeredAfterMaxAttempts() {
        // First 4 failures do not lock out
        for (i in 1..4) {
            guard.recordFailure("test_user")
            assertFalse(guard.isLockedOut("test_user"))
        }

        // 5th failure triggers lockout
        val attempts = guard.recordFailure("test_user")
        assertEquals(5, attempts)
        assertTrue(guard.isLockedOut("test_user"))
        assertTrue(guard.getRemainingLockoutSeconds("test_user") > 0)
    }

    @Test
    fun testSuccessResetsCounter() {
        for (i in 1..5) {
            guard.recordFailure("test_user")
        }
        assertTrue(guard.isLockedOut("test_user"))

        guard.recordSuccess("test_user")
        assertFalse(guard.isLockedOut("test_user"))
        assertEquals(0, guard.getRemainingLockoutSeconds("test_user"))
    }

    @Test
    fun testWipeKeysThreshold() {
        for (i in 1..9) {
            guard.recordFailure("test_user")
            assertFalse(guard.shouldWipeKeys("test_user"))
        }

        guard.recordFailure("test_user") // 10th failure
        assertTrue(guard.shouldWipeKeys("test_user"))
    }
}
