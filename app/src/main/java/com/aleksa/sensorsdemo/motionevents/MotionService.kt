package com.aleksa.sensorsdemo.motionevents

import android.app.Activity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import kotlin.math.sqrt

private const val TAG = "MotionService"

class MotionService(activity: Activity) : SensorEventListener {

    interface MotionListener {
        fun onRotationChange(pitch: Int, roll: Int)
        fun onShakeChange()
    }

    var currentRoll: Int = 0
    var currentPitch: Int = 0
    private val windowManager: WindowManager = activity.window.windowManager
    private val sensorManager: SensorManager =
        activity.getSystemService(Activity.SENSOR_SERVICE) as SensorManager
    private val rotationSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private var lastAccuracy: Int = SensorManager.SENSOR_STATUS_UNRELIABLE
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var accel: Float = 0f
    private var accelCurrent: Float = 0f
    private var accelLast: Float = 0f
    private var listener: MotionListener? = null

    fun startListening(listener: MotionListener) {
        if (this.listener === listener) {
            return
        }
        this.listener = listener
        if (rotationSensor == null) {
            Log.w(TAG, "Rotation vector sensor not available; will not provide orientation data.")
            return
        }
        sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
        listener = null
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        lastAccuracy = accuracy
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (listener == null || lastAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            return
        }
        if (event.sensor == rotationSensor) {
            updateOrientation(event.values)
        }
        if (event.sensor == accelerometer) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            accelLast = accelCurrent
            accelCurrent = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val delta = accelCurrent - accelLast
            accel = accel * 0.9f + delta

            if (accel > 15) {
                listener?.onShakeChange()
            }
        }
    }

    private fun updateOrientation(rotationVector: FloatArray) {
        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector)

        val (worldAxisForDeviceAxisX, worldAxisForDeviceAxisY) = when (windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_0 -> Pair(SensorManager.AXIS_X, SensorManager.AXIS_Z)
            Surface.ROTATION_90 -> Pair(SensorManager.AXIS_Z, SensorManager.AXIS_MINUS_X)
            Surface.ROTATION_180 -> Pair(SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Z)
            Surface.ROTATION_270 -> Pair(SensorManager.AXIS_MINUS_Z, SensorManager.AXIS_X)
            else -> Pair(SensorManager.AXIS_X, SensorManager.AXIS_Z)
        }

        val adjustedRotationMatrix = FloatArray(9)
        SensorManager.remapCoordinateSystem(
            rotationMatrix, worldAxisForDeviceAxisX,
            worldAxisForDeviceAxisY, adjustedRotationMatrix
        )

        val orientation = FloatArray(3)
        SensorManager.getOrientation(adjustedRotationMatrix, orientation)

        val pitch = orientation[1] * -57
        val roll = orientation[2] * -57
        currentPitch = pitch.toInt()
        currentRoll = roll.toInt()

        listener?.onRotationChange(pitch.toInt(), roll.toInt())
    }
}
