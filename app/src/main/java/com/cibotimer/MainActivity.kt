package com.cibotimer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.cibotimer.ui.CiboTimerNavigation
import com.cibotimer.ui.theme.CiboTimerTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Notification permission result handled silently
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        com.cibotimer.widget.FastingWidgetUpdater.updateAll(this)
        askNotificationPermission()

        val repository = (application as CiboTimerApp).repository

        setContent {
            CiboTimerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CiboTimerNavigation(
                        repository = repository,
                        updateManager = (application as CiboTimerApp).updateManager
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        com.cibotimer.widget.FastingWidgetUpdater.updateAll(this)
        val updateMgr = (application as CiboTimerApp).updateManager
        if (updateMgr.autoCheckEnabled) {
            updateMgr.checkForUpdates(manual = false)
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
