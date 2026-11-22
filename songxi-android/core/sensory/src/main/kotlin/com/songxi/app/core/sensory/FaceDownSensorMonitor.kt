package com.songxi.app.core.sensory

import kotlinx.coroutines.flow.StateFlow

interface FaceDownSensorMonitor {
    val isFaceDown: StateFlow<Boolean>
    fun startMonitoring()
    fun stopMonitoring()
    fun simulateFaceDownForTest(faceDown: Boolean)
}
