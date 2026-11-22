package com.songxi.app.core.sensory

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.hypot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidFaceDownSensorMonitor(context: Context) : FaceDownSensorMonitor {

    companion object {
        const val ENTER_FACE_DOWN_Z_THRESHOLD: Float = -8.5f
        const val EXIT_FACE_DOWN_Z_THRESHOLD: Float = -5.5f
        const val MAX_HORIZONTAL_TILT_MPS2: Float = 3.5f
    }

    private val appContext: Context = context.applicationContext ?: context
    private val sensorManager: SensorManager? =
        appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val _isFaceDown = MutableStateFlow(false)
    override val isFaceDown: StateFlow<Boolean> = _isFaceDown.asStateFlow()

    @Volatile
    private var monitoringActive: Boolean = false

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (!monitoringActive || event == null) return
            val type = event.sensor?.type
            if (type != null && type != Sensor.TYPE_ACCELEROMETER) return
            val values = event.values
            if (values == null || values.size < 3) return
            onAccelerometerSample(values[0], values[1], values[2])
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    // Constructor intentionally registers ZERO listeners.

    @Synchronized
    override fun startMonitoring() {
        if (monitoringActive) return
        val mgr = sensorManager ?: return
        monitoringActive = true
        _isFaceDown.value = false
        val accelerometer = mgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        runCatching {
            mgr.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }
    }

    @Synchronized
    override fun stopMonitoring() {
        monitoringActive = false
        runCatching {
            sensorManager?.unregisterListener(sensorListener)
        }
        _isFaceDown.value = false
    }

    override fun simulateFaceDownForTest(faceDown: Boolean) {
        _isFaceDown.value = faceDown
    }

    internal fun onAccelerometerSample(x: Float, y: Float, z: Float) {
        if (!x.isFinite() || !y.isFinite() || !z.isFinite()) return
        val horizontalTilt = hypot(x.toDouble(), y.toDouble()).toFloat()
        val current = _isFaceDown.value
        if (!current) {
            if (z < ENTER_FACE_DOWN_Z_THRESHOLD && horizontalTilt < MAX_HORIZONTAL_TILT_MPS2) {
                _isFaceDown.value = true
            }
        } else {
            if (z > EXIT_FACE_DOWN_Z_THRESHOLD) {
                _isFaceDown.value = false
            }
        }
    }
}
