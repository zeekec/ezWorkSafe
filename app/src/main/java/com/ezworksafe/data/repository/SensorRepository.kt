// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.data.repository

import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.data.model.SensorType
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over sensor status observation.
 *
 * Implementations determine sensor access (permission + AppOps), not hardware usage.
 * See [SystemSensorRepository] for the production implementation.
 */
interface SensorRepository {
    /** Returns a [Flow] that emits [SensorStatus] changes for [type]. */
    fun observeSensor(type: SensorType): Flow<SensorStatus>

    /** Triggers re-emission of all sensor flows (e.g., on app resume). */
    fun refresh()
}
