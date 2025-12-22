package hu.kadatsoft.smsreply

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    private const val LOG_FILE_NAME = "app_logs.txt"
    private const val TAG = "SmsReplyApp"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun d(tag: String, message: String, context: Context? = null) {
        val logMessage = "D/$tag: $message"
        Log.d(TAG, logMessage)
        context?.let { writeToFile(it, logMessage) }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null, context: Context? = null) {
        val logMessage = "E/$tag: $message" + (throwable?.let { "\n${Log.getStackTraceString(it)}" } ?: "")
        Log.e(TAG, logMessage)
        context?.let { writeToFile(it, logMessage) }
    }

    fun i(tag: String, message: String, context: Context? = null) {
        val logMessage = "I/$tag: $message"
        Log.i(TAG, logMessage)
        context?.let { writeToFile(it, logMessage) }
    }

    private fun writeToFile(context: Context, message: String) {
        try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            val timestamp = dateFormat.format(Date())
            val entry = "[$timestamp] $message\n"
            
            // Limit file size (optional but recommended)
            if (file.exists() && file.length() > 500 * 1024) { // 500 KB limit
                file.delete()
            }
            
            val writer = FileWriter(file, true)
            writer.append(entry)
            writer.flush()
            writer.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write log to file", e)
        }
    }

    fun getLogs(context: Context): String {
        return try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            if (file.exists()) {
                file.readText()
            } else {
                "No logs found."
            }
        } catch (e: Exception) {
            "Error reading logs: ${e.message}"
        }
    }

    fun clearLogs(context: Context) {
        try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear logs", e)
        }
    }
}
