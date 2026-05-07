package com.ezworksafe.util

import android.Manifest
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PermissionHelperTest {

    @get:Rule
    val instantRule = InstantTaskExecutorRule()

    @Test
    fun `required runtime permissions are CAMERA and RECORD_AUDIO`() {
        val expected = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        assertArrayEquals(expected, PermissionHelper.REQUIRED_RUNTIME_PERMISSIONS)
    }
}
