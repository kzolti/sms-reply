package hu.kadatsoft.smsreply

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log

class CallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReplyReceiver"
        private const val PREFS_NAME = "SmsReplyPrefs"
        private const val KEY_WAS_RINGING = "was_ringing"
        private const val KEY_INCOMING_NUMBER = "incoming_number"
        private const val KEY_LAST_SENT_PREFIX = "last_sent_"
        private const val COOLDOWN_MS = 60 * 1000L // 1 minute
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (!ServiceState.isServiceRunning) {
            AppLogger.d(TAG, "Service is not running, ignoring call.", context)
            return
        }

        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            AppLogger.d(TAG, "Phone State: $stateStr, Number: $number", context)

            when (stateStr) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    // Save number only if present and valid
                    if (!number.isNullOrBlank()) {
                        prefs.edit()
                            .putString(KEY_INCOMING_NUMBER, number)
                            .putBoolean(KEY_WAS_RINGING, true)
                            .commit() // Synchronous for better race condition protection
                        AppLogger.d(TAG, "Number received and saved: $number", context)
                    } else {
                        prefs.edit().putBoolean(KEY_WAS_RINGING, true).commit()
                        AppLogger.d(TAG, "Ringing but no number available", context)
                    }
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    // Answered call - don't send SMS
                    prefs.edit().putBoolean(KEY_WAS_RINGING, false).commit()
                    AppLogger.d(TAG, "State: OFFHOOK (Answered)", context)
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    val wasRinging = prefs.getBoolean(KEY_WAS_RINGING, false)
                    val savedNumber = prefs.getString(KEY_INCOMING_NUMBER, null)
                    
                    AppLogger.d(TAG, "State: IDLE. WasRinging: $wasRinging, SavedNumber: $savedNumber", context)
                    
                    if (wasRinging && !savedNumber.isNullOrBlank()) {
                        val lastSentTime = prefs.getLong(KEY_LAST_SENT_PREFIX + savedNumber, 0L)
                        val currentTime = System.currentTimeMillis()
                        
                        if (currentTime - lastSentTime < COOLDOWN_MS) {
                            AppLogger.d(TAG, "SMS to $savedNumber skipped: Cooldown active (last sent ${ (currentTime - lastSentTime) / 1000 }s ago)", context)
                        } else {
                            AppLogger.d(TAG, "Missed call triggered. Sending SMS.", context)
                            val messageToUse = MessageRepository.getSelectedMessage(context)
                            
                            // Update last sent time BEFORE sending to be extra safe against overlaps
                            prefs.edit().putLong(KEY_LAST_SENT_PREFIX + savedNumber, currentTime).commit()
                            
                            sendSms(context, savedNumber, messageToUse)
                        }
                    } else if (wasRinging) {
                        AppLogger.d(TAG, "Missed call but no valid number to send SMS to", context)
                    }
                    
                    // Reset state
                    prefs.edit()
                        .putBoolean(KEY_WAS_RINGING, false)
                        .remove(KEY_INCOMING_NUMBER)
                        .commit()
                }
            }
        }
    }

    private fun sendSms(context: Context, phoneNumber: String, message: String) {
        try {
            // Check SMS permission at runtime
            if (android.content.pm.PackageManager.PERMISSION_GRANTED != 
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS)) {
                AppLogger.e(TAG, "SMS permission not granted", context = context)
                return
            }
            
            // Validate phone number format
            if (phoneNumber.isBlank() || message.isBlank()) {
                AppLogger.e(TAG, "Invalid phone number or message", context = context)
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
                AppLogger.d(TAG, "Multipart SMS sent to $phoneNumber (${parts.size} parts)", context)
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                AppLogger.d(TAG, "SMS sent to $phoneNumber", context)
            }
        } catch (e: SecurityException) {
            AppLogger.e(TAG, "SMS permission denied", e, context)
        } catch (e: IllegalArgumentException) {
            AppLogger.e(TAG, "Invalid SMS parameters", e, context)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to send SMS", e, context)
        }
    }
}
