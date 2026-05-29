// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.util

import android.Manifest
import android.os.Build
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S])
class PermissionHelperTest {

    @Test
    fun `required runtime permissions include BLUETOOTH_CONNECT on API 31+`() {
        val result = PermissionHelper.getRequiredRuntimePermissions(Build.VERSION_CODES.S)
        assertTrue(result.contains(Manifest.permission.BLUETOOTH_CONNECT))
        assertTrue(result.contains(Manifest.permission.CAMERA))
        assertTrue(result.contains(Manifest.permission.RECORD_AUDIO))
        assertTrue(result.size == 3)
    }

    @Test
    fun `required runtime permissions are CAMERA and RECORD_AUDIO pre-31`() {
        val expected = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        assertArrayEquals(expected, PermissionHelper.getRequiredRuntimePermissions(Build.VERSION_CODES.R))
    }

    @Test
    fun `areRuntimePermissionsGranted returns true when all permissions granted`() {
        val app = RuntimeEnvironment.getApplication()
        shadowOf(app).grantPermissions(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        assertTrue(PermissionHelper.areRuntimePermissionsGranted(app))
    }

    @Test
    fun `areRuntimePermissionsGranted returns false when camera denied`() {
        val app = RuntimeEnvironment.getApplication()
        shadowOf(app).grantPermissions(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        assertFalse(PermissionHelper.areRuntimePermissionsGranted(app))
    }

    @Test
    fun `areRuntimePermissionsGranted returns false when audio denied`() {
        val app = RuntimeEnvironment.getApplication()
        shadowOf(app).grantPermissions(
            Manifest.permission.CAMERA,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        assertFalse(PermissionHelper.areRuntimePermissionsGranted(app))
    }
}
