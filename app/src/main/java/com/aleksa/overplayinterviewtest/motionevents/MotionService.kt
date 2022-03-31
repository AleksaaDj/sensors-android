package com.aleksa.overplayinterviewtest.motionevents

/**
 * Created by Aleksa Djordjevic on March 31st 2022
 * Copyright (c) 2022 . All rights reserved.
 */
import android.app.Activity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.view.Surface
import android.view.WindowManager

private const val TAG = "Rotation"

class MotionService(activity: Activity) : SensorEventListener {

    interface MotionListener {
        fun onRotationChange(pitch: Int, roll: Int)
        fun onShakeChange()
    }

    var currentRoll: Int = 0
    var currentPitch: Int = 0
    private val mWindowManager: WindowManager = activity.window.windowManager
    private val mSensorManager: SensorManager =
        activity.getSystemService(Activity.SENSOR_SERVICE) as SensorManager
    private val mRotationSensor: Sensor? =
        mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private var mLastAccuracy: Int = SensorManager.SENSOR_STATUS_UNRELIABLE
    private val mAccelerometer: Sensor? = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var mAccel: Float = 0.toFloat() // acceleration apart from gravity
    private var mAccelCurrent: Float = 0.toFloat() // current acceleration including gravity
    private var mAccelLast: Float = 0.toFloat() // last acceleration including gravity
    private var mListener: MotionListener? = null


    fun startListening(listener: MotionListener) {
        if (mListener === listener) {
            return
        }
        mListener = listener
        if (mRotationSensor == null) {
            Log.w(TAG, "Rotation vector sensor not available; will not provide orientation data.")
            return
        }
        mSensorManager.registerListener(this, mRotationSensor, SensorManager.SENSOR_DELAY_UI)
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI)

    }

    fun stopListening() {
        mSensorManager.unregisterListener(this)
        mListener = null
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        mLastAccuracy = accuracy
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (mListener == null) {
            return
        }
        if (mLastAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            return
        }
        if (event.sensor == mRotationSensor) {
            updateOrientation(event.values)
        }
        if (event.sensor == mAccelerometer) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            mAccelLast = mAccelCurrent
            mAccelCurrent = kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val delta = mAccelCurrent - mAccelLast
            mAccel = mAccel * 0.9f + delta

            if (mAccel > 15) {
                mListener?.onShakeChange()
            }
        }
    }

    private fun updateOrientation(rotationVector: FloatArray) {
        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector)

        val (worldAxisForDeviceAxisX, worldAxisForDeviceAxisY) = when (mWindowManager.defaultDisplay.rotation) {
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

        // Transform rotation matrix into azimuth/pitch/roll
        val orientation = FloatArray(3)
        SensorManager.getOrientation(adjustedRotationMatrix, orientation)

        // Convert radians to degrees
        val pitch = orientation[1] * -57
        val roll = orientation[2] * -57
        // Setup initial pitch and roll value.
        currentPitch = pitch.toInt()
        currentRoll = roll.toInt()

        mListener?.onRotationChange(pitch.toInt(), roll.toInt())
    }
}