package com.songxi.app.core.sensory

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.SensorEventBuilder
import org.robolectric.shadows.ShadowSensor
import org.robolectric.shadows.ShadowSensorManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FaceDownSensorMonitorTest {

    private lateinit var context: Context
    private lateinit var shadowSensorManager: ShadowSensorManager
    private lateinit var accelerometer: Sensor

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val sensorManager = context.getSystemService(SensorManager::class.java)
        shadowSensorManager = Shadows.shadowOf(sensorManager)
        accelerometer = ShadowSensor.newInstance(Sensor.TYPE_ACCELEROMETER)
        shadowSensorManager.addSensor(accelerometer)
    }

    private fun emitAccel(x: Float, y: Float, z: Float) {
        val event = SensorEventBuilder.newBuilder()
            .setSensor(accelerometer)
            .setValues(floatArrayOf(x, y, z))
            .setTimestamp(System.nanoTime())
            .build()
        shadowSensorManager.sendSensorEventToListeners(event, accelerometer)
    }

    @Test
    fun strictZeroListenerConstructorAndStartStopLifecycleIsolation() {
        val monitor = AndroidFaceDownSensorMonitor(context)
        assertEquals(0, shadowSensorManager.listeners.size)
        assertFalse(monitor.isFaceDown.value)

        monitor.startMonitoring()
        assertEquals(1, shadowSensorManager.listeners.size)

        // Idempotent second startMonitoring call must not register duplicate listeners
        monitor.startMonitoring()
        assertEquals(1, shadowSensorManager.listeners.size)

        emitAccel(0.2f, -0.1f, -9.6f)
        assertTrue(monitor.isFaceDown.value)

        monitor.stopMonitoring()
        assertEquals(0, shadowSensorManager.listeners.size)
        assertFalse(monitor.isFaceDown.value)
    }

    @Test
    fun hysteresisAcceptsFlatFaceDownRejectsShakeAndRecoversOnLift() {
        val monitor = AndroidFaceDownSensorMonitor(context)
        monitor.startMonitoring()

        // Upright desk-angled hold -> stays false
        emitAccel(0f, 9.2f, -3.0f)
        assertFalse(monitor.isFaceDown.value)

        // Violent horizontal shake with negative Z (hypot(4, 3) = 5.0 >= 3.5) -> rejected
        emitAccel(4.0f, 3.0f, -9.2f)
        assertFalse(monitor.isFaceDown.value)

        // Calm flat face-down placement on desk (z < -8.5 && hypot < 3.5) -> enters face-down
        emitAccel(0.4f, -0.3f, -9.5f)
        assertTrue(monitor.isFaceDown.value)

        // Desk micro-vibration inside hysteresis band (-8.5 <= z <= -5.5) -> stays true without chatter
        emitAccel(0.2f, 0.1f, -6.2f)
        assertTrue(monitor.isFaceDown.value)

        // Lifting phone above z = -5.5f -> exits face-down
        emitAccel(0.0f, 4.0f, -4.8f)
        assertFalse(monitor.isFaceDown.value)

        // Full face-up -> stays false
        emitAccel(0f, 0f, 9.2f)
        assertFalse(monitor.isFaceDown.value)

        // Deterministic test override hook
        monitor.simulateFaceDownForTest(true)
        assertTrue(monitor.isFaceDown.value)
        monitor.simulateFaceDownForTest(false)
        assertFalse(monitor.isFaceDown.value)

        monitor.stopMonitoring()
    }
}
