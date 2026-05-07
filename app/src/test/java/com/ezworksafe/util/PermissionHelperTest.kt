package com.ezworksafe.util

import android.Manifest
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class PermissionHelperTest {

    @Test
    fun `required runtime permissions are CAMERA and RECORD_AUDIO`() {
        val expected = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        assertArrayEquals(expected, PermissionHelper.REQUIRED_RUNTIME_PERMISSIONS)
    }
}
