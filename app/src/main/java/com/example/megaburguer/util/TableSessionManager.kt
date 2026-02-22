package com.example.megaburguer.util

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TableSessionManager @Inject constructor() {
    private var lastLoginTimestamp: Long = 0

    fun isSessionValid(): Boolean {
        if (lastLoginTimestamp == 0L) return false
        val currentTime = System.currentTimeMillis()
        val elapsedMinutes = (currentTime - lastLoginTimestamp) / (1000 * 60)
        return elapsedMinutes < 5
    }

    fun startSession() {
        lastLoginTimestamp = System.currentTimeMillis()
    }

    fun clearSession() {
        lastLoginTimestamp = 0
    }
}
