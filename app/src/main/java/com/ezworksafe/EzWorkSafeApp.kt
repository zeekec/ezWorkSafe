// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe

import android.app.Application
import com.ezworksafe.data.repository.SensorRepository
import com.ezworksafe.data.repository.SystemSensorRepository

/** Application entry point. Initializes the shared [SensorRepository] singleton. */
class EzWorkSafeApp : Application() {

    /** Shared repository instance. Accessible from ViewModels and [com.ezworksafe.service.MonitoringService]. */
    lateinit var sensorRepository: SensorRepository

    override fun onCreate() {
        super.onCreate()
        sensorRepository = SystemSensorRepository(this)
    }
}
