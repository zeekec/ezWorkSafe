// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.data.repository

import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.data.model.SensorType
import kotlinx.coroutines.flow.Flow

interface SensorRepository {
    fun observeSensor(type: SensorType): Flow<SensorStatus>
    fun refresh()
}
