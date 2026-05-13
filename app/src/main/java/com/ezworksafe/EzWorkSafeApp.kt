// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe

import android.app.Application
import com.ezworksafe.data.repository.SensorRepository
import com.ezworksafe.data.repository.SystemSensorRepository

class EzWorkSafeApp : Application() {

    lateinit var sensorRepository: SensorRepository

    override fun onCreate() {
        super.onCreate()
        sensorRepository = SystemSensorRepository(this)
    }
}
