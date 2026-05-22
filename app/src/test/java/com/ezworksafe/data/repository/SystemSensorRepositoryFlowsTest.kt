// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.data.repository

import android.Manifest
import android.app.AppOpsManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.hardware.camera2.CameraManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Process
import androidx.test.core.app.ApplicationProvider
import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.data.model.SensorType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class SystemSensorRepositoryFlowsTest {

    private lateinit var context: Context
    private lateinit var repo: SystemSensorRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repo = SystemSensorRepository(context)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `wifi active when wifi enabled`() = runTest {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiManager.setWifiEnabled(true)

        val status = repo.observeSensor(SensorType.WIFI).first()
        assertEquals(SensorStatus.Active, status)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `wifi blocked when wifi disabled`() = runTest {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiManager.setWifiEnabled(false)

        val status = repo.observeSensor(SensorType.WIFI).first()
        assertEquals(SensorStatus.Blocked, status)
    }

    @Test
    fun `wifi unavailable when wifi manager returns null`() = runTest {
        val repo = SystemSensorRepository(mockContextForNullService())
        val status = repo.observeSensor(SensorType.WIFI).first()
        assertEquals(SensorStatus.Unavailable, status)
    }

    @Test
    fun `bluetooth active when bt enabled`() = runTest {
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = btManager.adapter
        if (adapter != null) {
            shadowOf(adapter).setEnabled(true)
        }

        val status = repo.observeSensor(SensorType.BLUETOOTH).first()
        assertEquals(SensorStatus.Active, status)
    }

    @Test
    fun `bluetooth blocked when bt disabled`() = runTest {
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = btManager.adapter
        if (adapter != null) {
            shadowOf(adapter).setEnabled(false)
        }

        val status = repo.observeSensor(SensorType.BLUETOOTH).first()
        assertEquals(SensorStatus.Blocked, status)
    }

    @Test
    fun `bluetooth unavailable when adapter is null`() = runTest {
        val repo = SystemSensorRepository(mockContextForNullService())
        val status = repo.observeSensor(SensorType.BLUETOOTH).first()
        assertEquals(SensorStatus.Unavailable, status)
    }

    @Test
    fun `mic active when permission granted and app ops allowed`() = runTest {
        grantMicPermission()

        val status = repo.observeSensor(SensorType.MICROPHONE).first()
        assertEquals(SensorStatus.Active, status)
    }

    @Test
    fun `mic denied when permission not granted`() = runTest {
        val status = repo.observeSensor(SensorType.MICROPHONE).first()
        assertEquals(SensorStatus.Denied, status)
    }

    @Test
    fun `mic unavailable when audio manager returns null`() = runTest {
        val repo = SystemSensorRepository(mockContextForNullService())
        val status = repo.observeSensor(SensorType.MICROPHONE).first()
        assertEquals(SensorStatus.Unavailable, status)
    }

    @Test
    fun `camera active when permission granted and app ops allowed`() = runTest {
        grantCameraPermission()
        setupCamera()

        val status = repo.observeSensor(SensorType.CAMERA).first()
        assertEquals(SensorStatus.Active, status)
    }

    @Test
    fun `camera denied when permission not granted`() = runTest {
        val status = repo.observeSensor(SensorType.CAMERA).first()
        assertEquals(SensorStatus.Denied, status)
    }

    @Test
    fun `camera unavailable when camera manager returns null`() = runTest {
        val repo = SystemSensorRepository(mockContextForNullService())
        val status = repo.observeSensor(SensorType.CAMERA).first()
        assertEquals(SensorStatus.Unavailable, status)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `refresh triggers flow re-emission after state change`() = runTest {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiManager.setWifiEnabled(true)

        val first = repo.observeSensor(SensorType.WIFI).first()
        assertEquals(SensorStatus.Active, first)

        wifiManager.setWifiEnabled(false)
        repo.refresh()
        val afterRefresh = repo.observeSensor(SensorType.WIFI).first()
        assertEquals(SensorStatus.Blocked, afterRefresh)
    }

    private fun grantMicPermission() {
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.RECORD_AUDIO)
    }

    private fun grantCameraPermission() {
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.CAMERA)
    }

    private fun setupCamera() {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val shadowCam = shadowOf(cameraManager)
        shadowCam.addCamera("0", mock())
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class AppOpsBlockedTest {

    @Suppress("DEPRECATION")
    @Test
    fun `mic blocked when app ops is ignored`() = runTest {
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.RECORD_AUDIO)

        val appOps = mock<AppOpsManager>().apply {
            whenever(unsafeCheckOpNoThrow(AppOpsManager.OPSTR_RECORD_AUDIO, Process.myUid(), "com.ezworksafe"))
                .doReturn(AppOpsManager.MODE_IGNORED)
        }
        val realContext = ApplicationProvider.getApplicationContext<Context>()
        val context = mock<Context>().apply {
            whenever(getSystemService(Context.APP_OPS_SERVICE)).doReturn(appOps)
            whenever(getSystemService(Context.AUDIO_SERVICE)).doReturn(
                realContext.getSystemService(Context.AUDIO_SERVICE)
            )
            whenever(applicationContext).doReturn(this)
            whenever(packageName).doReturn("com.ezworksafe")
        }

        val repo = SystemSensorRepository(context)
        val status = repo.observeSensor(SensorType.MICROPHONE).first()
        assertEquals(SensorStatus.Blocked, status)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `camera blocked when app ops is ignored`() = runTest {
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.CAMERA)

        val appOps = mock<AppOpsManager>().apply {
            whenever(unsafeCheckOpNoThrow(AppOpsManager.OPSTR_CAMERA, Process.myUid(), "com.ezworksafe"))
                .doReturn(AppOpsManager.MODE_IGNORED)
        }
        val realContext = ApplicationProvider.getApplicationContext<Context>()
        val context = mock<Context>().apply {
            whenever(getSystemService(Context.APP_OPS_SERVICE)).doReturn(appOps)
            whenever(getSystemService(Context.CAMERA_SERVICE)).doReturn(
                realContext.getSystemService(Context.CAMERA_SERVICE)
            )
            whenever(applicationContext).doReturn(this)
            whenever(packageName).doReturn("com.ezworksafe")
        }

        val repo = SystemSensorRepository(context)
        val status = repo.observeSensor(SensorType.CAMERA).first()
        assertEquals(SensorStatus.Blocked, status)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class AppOpsBlockedApi28Test {

    @Suppress("DEPRECATION")
    @Test
    fun `mic blocked when app ops is ignored on API 28`() = runTest {
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.RECORD_AUDIO)

        val appOps = mock<AppOpsManager>().apply {
            whenever(checkOpNoThrow(AppOpsManager.OPSTR_RECORD_AUDIO, Process.myUid(), "com.ezworksafe"))
                .doReturn(AppOpsManager.MODE_IGNORED)
        }
        val realContext = ApplicationProvider.getApplicationContext<Context>()
        val context = mock<Context>().apply {
            whenever(getSystemService(Context.APP_OPS_SERVICE)).doReturn(appOps)
            whenever(getSystemService(Context.AUDIO_SERVICE)).doReturn(
                realContext.getSystemService(Context.AUDIO_SERVICE)
            )
            whenever(applicationContext).doReturn(this)
            whenever(packageName).doReturn("com.ezworksafe")
        }

        val repo = SystemSensorRepository(context)
        val status = repo.observeSensor(SensorType.MICROPHONE).first()
        assertEquals(SensorStatus.Blocked, status)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `camera blocked when app ops is ignored on API 28`() = runTest {
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.CAMERA)

        val appOps = mock<AppOpsManager>().apply {
            whenever(checkOpNoThrow(AppOpsManager.OPSTR_CAMERA, Process.myUid(), "com.ezworksafe"))
                .doReturn(AppOpsManager.MODE_IGNORED)
        }
        val realContext = ApplicationProvider.getApplicationContext<Context>()
        val context = mock<Context>().apply {
            whenever(getSystemService(Context.APP_OPS_SERVICE)).doReturn(appOps)
            whenever(getSystemService(Context.CAMERA_SERVICE)).doReturn(
                realContext.getSystemService(Context.CAMERA_SERVICE)
            )
            whenever(applicationContext).doReturn(this)
            whenever(packageName).doReturn("com.ezworksafe")
        }

        val repo = SystemSensorRepository(context)
        val status = repo.observeSensor(SensorType.CAMERA).first()
        assertEquals(SensorStatus.Blocked, status)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `mic active when app ops allowed on API 28`() = runTest {
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.RECORD_AUDIO)

        val appOps = mock<AppOpsManager>().apply {
            whenever(checkOpNoThrow(AppOpsManager.OPSTR_RECORD_AUDIO, Process.myUid(), "com.ezworksafe"))
                .doReturn(AppOpsManager.MODE_ALLOWED)
        }
        val realContext = ApplicationProvider.getApplicationContext<Context>()
        val context = mock<Context>().apply {
            whenever(getSystemService(Context.APP_OPS_SERVICE)).doReturn(appOps)
            whenever(getSystemService(Context.AUDIO_SERVICE)).doReturn(
                realContext.getSystemService(Context.AUDIO_SERVICE)
            )
            whenever(applicationContext).doReturn(this)
            whenever(packageName).doReturn("com.ezworksafe")
        }

        val repo = SystemSensorRepository(context)
        val status = repo.observeSensor(SensorType.MICROPHONE).first()
        assertEquals(SensorStatus.Active, status)
    }
}

private fun mockContextForNullService(): Context = mock<Context>().apply {
    whenever(getSystemService(any<String>())).doReturn(null)
    whenever(applicationContext).doReturn(mock())
    whenever(packageName).doReturn("com.ezworksafe")
}
