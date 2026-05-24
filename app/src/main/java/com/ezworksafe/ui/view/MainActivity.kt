// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.ui.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ezworksafe.service.MonitoringService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.ezworksafe.ui.viewmodel.SensorViewModel
import com.ezworksafe.util.PermissionHelper

class MainActivity : ComponentActivity() {

    private val viewModel: SensorViewModel by viewModels()
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val anyDenied = result.values.any { !it }
        if (anyDenied) {
            Toast.makeText(
                this,
                "Camera and microphone permissions were denied. Status for these sensors may be unavailable.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshSensorFlows()
            }
        })

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(2_000L)
                    viewModel.refresh()
                }
            }
        }

        requestRuntimePermissionsIfNeeded()
        startMonitoringService()

        setContent {
            EzWorkSafeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StatusDashboard(viewModel = viewModel)
                }
            }
        }
    }

    private fun refreshSensorFlows() {
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
