package com.example.smsreply

import android.content.Context

object ServiceState {
    var isServiceRunning: Boolean = false
    
    private const val PREFS_NAME = "SMS_REPLY_PREFS"
    private const val PREF_START_ON_BOOT = "start_on_boot"

    private val listeners = mutableListOf<(Boolean) -> Unit>()

    fun addListener(listener: (Boolean) -> Unit) {
        listeners.add(listener)
        listener(isServiceRunning) // Initial state
    }

    fun removeListener(listener: (Boolean) -> Unit) {
        listeners.remove(listener)
    }

    fun setRunning(running: Boolean) {
        if (isServiceRunning != running) {
            isServiceRunning = running
            notifyListeners()
        }
    }

    private fun notifyListeners() {
        listeners.forEach { it(isServiceRunning) }
    }

    fun setStartOnBoot(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_START_ON_BOOT, enabled).apply()
    }

    fun isStartOnBootEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_START_ON_BOOT, false)
    }
}
