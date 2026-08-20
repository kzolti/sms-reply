package hu.kadatsoft.smsreply

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsManager

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
        private const val PREFS_NAME = "SmsReplyPrefs"
        private const val KEY_LAST_SENT_PREFIX = "last_sent_sms_"
        private const val COOLDOWN_MS = 60 * 1000L // 1 perc cooldown
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        // Szinkronizálás a perzisztált állapottal
        ServiceState.syncFromPrefs(context)

        if (!ServiceState.isServiceRunning) {
            AppLogger.d(TAG, "Service is not running, ignoring incoming SMS.", context)
            return
        }

        if (!ServiceState.isSmsReplyEnabled(context)) {
            AppLogger.d(TAG, "SMS reply disabled, ignoring incoming SMS.", context)
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // Minden üzenet ugyanattól a feladótól érkezhet több részben
        val sender = messages[0].originatingAddress ?: run {
            AppLogger.d(TAG, "Incoming SMS: no sender address, skipping.", context)
            return
        }

        val fullBody = messages.joinToString("") { it.messageBody ?: "" }
        AppLogger.d(TAG, "Incoming SMS from $sender: \"${fullBody.take(30)}...\"", context)

        // Cooldown ellenőrzés – ne spammeljük ugyanazt a számot
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSentTime = prefs.getLong(KEY_LAST_SENT_PREFIX + sender, 0L)
        val now = System.currentTimeMillis()

        if (now - lastSentTime < COOLDOWN_MS) {
            AppLogger.d(TAG, "SMS reply to $sender skipped: cooldown active (${(now - lastSentTime) / 1000}s ago)", context)
            return
        }

        // Válasz küldése
        val replyMessage = MessageRepository.getSelectedMessage(context)
        sendSms(context, sender, replyMessage)
        prefs.edit().putLong(KEY_LAST_SENT_PREFIX + sender, now).apply()
    }

    private fun sendSms(context: Context, phoneNumber: String, message: String) {
        try {
            if (android.content.pm.PackageManager.PERMISSION_GRANTED !=
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS)
            ) {
                AppLogger.e(TAG, "SMS permission not granted", context = context)
                return
            }

            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            if (smsManager == null) {
                AppLogger.e(TAG, "SMS Manager not available", context = context)
                return
            }

            val parts = smsManager.divideMessage(message)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
                AppLogger.d(TAG, "Multipart SMS reply sent to $phoneNumber (${parts.size} parts)", context)
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                AppLogger.d(TAG, "SMS reply sent to $phoneNumber", context)
            }
        } catch (e: SecurityException) {
            AppLogger.e(TAG, "SMS permission denied", e, context)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to send SMS reply", e, context)
        }
    }
}
