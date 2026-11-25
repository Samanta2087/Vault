package com.pixelvault.app.security

class BruteForceGuard {
    companion object {
        const val MAX_ATTEMPTS_BEFORE_DELAY = 5
        const val MAX_ATTEMPTS_BEFORE_WIPE = 10

        private val LOCKOUT_DURATIONS_MS = listOf(
            30_000L,   // 30 seconds after 5 failed attempts
            60_000L,   // 1 minute after 6 failed attempts
            300_000L,  // 5 minutes after 7 failed attempts
            900_000L   // 15 minutes after 8+ failed attempts
        )
    }

    private val failedAttempts = mutableMapOf<String, Int>()
    private val lockoutTimers = mutableMapOf<String, Long>()

    fun recordFailure(target: String): Int {
        val attempts = (failedAttempts[target] ?: 0) + 1
        failedAttempts[target] = attempts

        if (attempts >= MAX_ATTEMPTS_BEFORE_DELAY) {
            val tier = (attempts - MAX_ATTEMPTS_BEFORE_DELAY).coerceAtMost(LOCKOUT_DURATIONS_MS.size - 1)
            val duration = LOCKOUT_DURATIONS_MS[tier]
            lockoutTimers[target] = System.currentTimeMillis() + duration
        }
        return attempts
    }

    fun isLockedOut(target: String): Boolean {
        val until = lockoutTimers[target] ?: return false
        return System.currentTimeMillis() < until
    }

    fun getRemainingLockoutSeconds(target: String): Int {
        val until = lockoutTimers[target] ?: return 0
        val remaining = (until - System.currentTimeMillis()) / 1000
        return remaining.coerceAtLeast(0).toInt()
    }

    fun shouldWipeKeys(target: String): Boolean {
        return (failedAttempts[target] ?: 0) >= MAX_ATTEMPTS_BEFORE_WIPE
    }

    fun recordSuccess(target: String) {
        failedAttempts[target] = 0
        lockoutTimers.remove(target)
    }
}
