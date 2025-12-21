package hu.kadatsoft.smsreply

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val REQUIRED_PERMISSIONS = mutableListOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.SEND_SMS
    ).apply {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private val PERMISSION_REQUEST_CODE = 123
    private lateinit var statusTextView: TextView
    private lateinit var startButton: android.widget.Button
    private lateinit var stopButton: android.widget.Button
    private lateinit var messageTextView: TextView

    private val stateListener: (Boolean) -> Unit = { isRunning ->
        runOnUiThread {
            updateUI(isRunning)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Layout Config
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            gravity = android.view.Gravity.CENTER_HORIZONTAL
        }

        // Title
        val titleView = TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 24f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 60 }
        }
        layout.addView(titleView)

        // Status
        statusTextView = TextView(this).apply {
            textSize = 18f
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 60 }
        }
        layout.addView(statusTextView)

        // Start Button
        startButton = android.widget.Button(this).apply {
            text = getString(R.string.start_service)
            setOnClickListener {
                startService()
            }
        }
        layout.addView(startButton)

        // Stop Button
        stopButton = android.widget.Button(this).apply {
            text = getString(R.string.stop_service)
            setOnClickListener {
                stopService()
            }
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 30 }
        }
        layout.addView(stopButton)

        // Manage Templates Button
        val manageButton = android.widget.Button(this).apply {
            text = getString(R.string.manage_messages)
            setOnClickListener {
                startActivity(android.content.Intent(this@MainActivity, TemplatesActivity::class.java))
            }
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 60 }
        }
        layout.addView(manageButton)

        // Exit Button
        val exitButton = android.widget.Button(this).apply {
            text = getString(R.string.exit_app)
            setOnClickListener {
                exitApp()
            }
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 30 }
        }
        layout.addView(exitButton)

        // Start on Boot Toggle
        val bootSwitch = android.widget.CheckBox(this).apply {
            text = getString(R.string.start_on_boot)
            isChecked = ServiceState.isStartOnBootEnabled(this@MainActivity)
            setOnCheckedChangeListener { _, isChecked ->
                ServiceState.setStartOnBoot(this@MainActivity, isChecked)
            }
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 60 }
        }
        layout.addView(bootSwitch)

        // Active Message Display
        messageTextView = TextView(this).apply {
            textSize = 14f
            gravity = android.view.Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.ITALIC)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 60 }
        }
        layout.addView(messageTextView)

        setContentView(layout)
        
        ServiceState.addListener(stateListener)

        if (allPermissionsGranted()) {
            checkBatteryOptimization()
            if (!ServiceState.isServiceRunning) {
                 startService() 
            }
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE)
            statusTextView.text = getString(R.string.checking_permissions)
        }
        updateActiveMessage()
    }

    override fun onResume() {
        super.onResume()
        runOnUiThread {
            updateActiveMessage()
            updateUI(ServiceState.isServiceRunning)
        }
    }

    private fun updateActiveMessage() {
        val currentMessage = MessageRepository.getSelectedMessage(this)
        messageTextView.text = "${getString(R.string.active_message_prefix)}$currentMessage"
    }

    override fun onDestroy() {
        super.onDestroy()
        ServiceState.removeListener(stateListener)
    }

    private fun updateUI(isRunning: Boolean) {
        if (isRunning) {
            statusTextView.text = getString(R.string.status_active)
            statusTextView.setTextColor(android.graphics.Color.parseColor("#4CAF50")) // Green
            startButton.isEnabled = false
            stopButton.isEnabled = true
        } else {
            statusTextView.text = getString(R.string.status_stopped)
            statusTextView.setTextColor(android.graphics.Color.parseColor("#F44336")) // Red
            startButton.isEnabled = true
            stopButton.isEnabled = false
        }
    }

    private fun startService() {
        if (ServiceState.isServiceRunning) return
        
        val serviceIntent = android.content.Intent(this, SmsForegroundService::class.java).apply {
            action = SmsForegroundService.ACTION_START
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun stopService() {
        val serviceIntent = android.content.Intent(this, SmsForegroundService::class.java).apply {
            action = SmsForegroundService.ACTION_STOP
        }
        startService(serviceIntent)
    }

    private fun exitApp() {
        stopService()
        
        // Clear notifications, finish and exit
        (getSystemService(android.app.NotificationManager::class.java))?.cancelAll()
        finishAffinity()
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (allPermissionsGranted()) {
                checkBatteryOptimization()
                startService()
            } else {
                statusTextView.text = getString(R.string.missing_permissions)
            }
        }
    }

    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                showBatteryOptimizationDialog()
            }
        }
    }

    private fun showBatteryOptimizationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.battery_opt_title)
            .setMessage(R.string.battery_opt_message)
            .setPositiveButton(R.string.battery_opt_settings) { _, _ ->
                requestBatteryOptimizationExemption()
            }
            .setNegativeButton(R.string.battery_opt_skip) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                // Fallback to general battery optimization settings
                try {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    startActivity(intent)
                } catch (e2: Exception) {
                    // Show manual instructions
                    AlertDialog.Builder(this)
                        .setTitle(R.string.battery_opt_manual_title)
                        .setMessage(R.string.battery_opt_manual_message)
                        .setPositiveButton(R.string.ok_button) { dialog, _ -> dialog.dismiss() }
                        .show()
                }
            }
        }
    }
}
