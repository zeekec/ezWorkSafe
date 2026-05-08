package com.ezworksafe.ui.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ezworksafe.service.MonitoringService
import com.ezworksafe.util.PermissionHelper

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshSensorFlows()
            }
        })

        requestRuntimePermissionsIfNeeded()
        startMonitoringService()

        setContent {
            EzWorkSafeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: com.ezworksafe.ui.viewmodel.SensorViewModel = viewModel()
                    StatusDashboard(viewModel = viewModel)
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            refreshSensorFlows()
        }
    }

    private fun refreshSensorFlows() {
        val viewModel = androidx.lifecycle.ViewModelProvider(this)[com.ezworksafe.ui.viewmodel.SensorViewModel::class.java]
        viewModel.refresh()
    }

    private fun requestRuntimePermissionsIfNeeded() {
        if (!PermissionHelper.areRuntimePermissionsGranted(this)) {
            requestPermissionLauncher.launch(PermissionHelper.REQUIRED_RUNTIME_PERMISSIONS)
        }
    }

    private fun startMonitoringService() {
        val intent = Intent(this, MonitoringService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }
}
