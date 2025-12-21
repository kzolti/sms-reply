package com.example.smsreply

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
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (!ServiceState.isServiceRunning) {
            Log.d(TAG, "Service is not running, ignoring call.")
            return
        }

        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            Log.d(TAG, "Phone State: $stateStr, Number: $number")

            when (stateStr) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    // Save number only if present and valid
                    if (!number.isNullOrBlank()) {
                        prefs.edit()
                            .putString(KEY_INCOMING_NUMBER, number)
                            .putBoolean(KEY_WAS_RINGING, true)
                            .apply()
                        Log.d(TAG, "Number received and saved: $number")
                    } else {
                        prefs.edit().putBoolean(KEY_WAS_RINGING, true).apply()
                        Log.d(TAG, "Ringing but no number available")
                    }
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    // Answered call - don't send SMS
                    prefs.edit().putBoolean(KEY_WAS_RINGING, false).apply()
                    Log.d(TAG, "State: OFFHOOK (Answered)")
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    val wasRinging = prefs.getBoolean(KEY_WAS_RINGING, false)
                    val savedNumber = prefs.getString(KEY_INCOMING_NUMBER, null)
                    
                    Log.d(TAG, "State: IDLE. WasRinging: $wasRinging, SavedNumber: $savedNumber")
                    
                    if (wasRinging && !savedNumber.isNullOrBlank()) {
                        Log.d(TAG, "Missed call triggered. Sending SMS.")
                        val messageToUse = MessageRepository.getSelectedMessage(context)
                        sendSms(context, savedNumber, messageToUse)
                    } else if (wasRinging) {
                        Log.d(TAG, "Missed call but no valid number to send SMS to")
                    }
                    
                    // Reset state
                    prefs.edit()
                        .putBoolean(KEY_WAS_RINGING, false)
                        .remove(KEY_INCOMING_NUMBER)
                        .apply()
                }
            }
        }
    }

    private fun sendSms(context: Context, phoneNumber: String, message: String) {
        try {
            // Check SMS permission at runtime
            if (android.content.pm.PackageManager.PERMISSION_GRANTED != 
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS)) {
                Log.e(TAG, "SMS permission not granted")
                return
            }
            
            // Validate phone number format
            if (phoneNumber.isBlank() || message.isBlank()) {
                Log.e(TAG, "Invalid phone number or message")
                return
            }
            
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            
            if (smsManager == null) {
                Log.e(TAG, "SMS Manager not available")
                return
            }
            
            val parts = smsManager.divideMessage(message)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
                Log.d(TAG, "Multipart SMS sent to $phoneNumber (${parts.size} parts)")
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                Log.d(TAG, "SMS sent to $phoneNumber")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SMS permission denied", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid SMS parameters", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS", e)
        }
    }
}
