// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Determines which runtime permissions this app requires and whether they're granted. */
object PermissionHelper {

    /** Lazily computed list of permissions to request at runtime. */
    val REQUIRED_RUNTIME_PERMISSIONS: Array<String> by lazy {
        getRequiredRuntimePermissions()
    }

    /**
     * Returns the set of runtime permissions for the given SDK level.
     *
     * - [CAMERA] and [RECORD_AUDIO] are always required (for Mic/Cam access checks).
     * - [BLUETOOTH_CONNECT] is added on API 31+ for Bluetooth status queries.
     */
    fun getRequiredRuntimePermissions(sdk: Int = Build.VERSION.SDK_INT): Array<String> {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (sdk >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        return permissions.toTypedArray()
    }

    /** Returns `true` when all runtime permissions are currently granted. */
    fun areRuntimePermissionsGranted(context: Context): Boolean {
        return REQUIRED_RUNTIME_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}
