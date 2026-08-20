package hu.kadatsoft.smsreply

import android.content.Context

object ServiceState {
    // In-memory cache – csak az aktuális process-ben él
    var isServiceRunning: Boolean = false
        private set

    private const val PREFS_NAME = "SMS_REPLY_PREFS"
    private const val PREF_START_ON_BOOT = "start_on_boot"
    private const val PREF_SERVICE_RUNNING = "service_running"
    private const val PREF_SMS_REPLY_ENABLED = "sms_reply_enabled"

    private val listeners = mutableListOf<(Boolean) -> Unit>()

    fun addListener(listener: (Boolean) -> Unit) {
        listeners.add(listener)
        listener(isServiceRunning) // Initial state
    }

    fun removeListener(listener: (Boolean) -> Unit) {
        listeners.remove(listener)
    }

    /**
     * Állapot beállítása és perzisztálása.
     * Mindig context-tel kell hívni, hogy SharedPreferences-be is mentődjön.
     */
    fun setRunning(context: Context, running: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_SERVICE_RUNNING, running).apply()
        if (isServiceRunning != running) {
            isServiceRunning = running
            notifyListeners()
        }
    }

    /**
     * Állapot lekérdezése SharedPreferences-ből (process-határon átívelő).
     * Akkor érdemes hívni, ha a service esetleg más process-ben volt leállítva.
     */
    fun isRunningPersisted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_SERVICE_RUNNING, false)
    }

    /**
     * In-memory cache szinkronizálása SharedPreferences-ből.
     * Hívd az app indulásakor / onResume-ban.
     */
    fun syncFromPrefs(context: Context) {
        val persisted = isRunningPersisted(context)
        if (isServiceRunning != persisted) {
            isServiceRunning = persisted
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

    fun setSmsReplyEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_SMS_REPLY_ENABLED, enabled).apply()
    }

    fun isSmsReplyEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_SMS_REPLY_ENABLED, false)
    }
}
