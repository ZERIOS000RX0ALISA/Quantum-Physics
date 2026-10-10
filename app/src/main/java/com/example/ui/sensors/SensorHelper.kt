package com.example.ui.sensors

import android.hardware.Sensor

object SensorHelper {

    fun getScientificName(sensorType: Int): String {
        return when (sensorType) {
            Sensor.TYPE_ACCELEROMETER -> "Accelerometer"
            Sensor.TYPE_GRAVITY -> "Gravity Sensor"
            Sensor.TYPE_LINEAR_ACCELERATION -> "Linear Acceleration"
            Sensor.TYPE_GYROSCOPE -> "Gyroscope"
            Sensor.TYPE_MAGNETIC_FIELD -> "Magnetic Field Sensor"
            Sensor.TYPE_ROTATION_VECTOR -> "Rotation Vector"
            Sensor.TYPE_LIGHT -> "Light Sensor"
            Sensor.TYPE_PROXIMITY -> "Proximity Sensor"
            Sensor.TYPE_STEP_COUNTER -> "Step Counter"
            Sensor.TYPE_STEP_DETECTOR -> "Step Detector"
            Sensor.TYPE_PRESSURE -> "Barometer (Pressure Sensor)"
            Sensor.TYPE_AMBIENT_TEMPERATURE -> "Ambient Temperature"
            Sensor.TYPE_RELATIVE_HUMIDITY -> "Humidity Sensor"
            else -> "Advanced: " + (knownSensorTypes[sensorType] ?: "Custom Transducer ($sensorType)")
        }
    }

    fun isPrimarySensor(sensorType: Int): Boolean {
        return sensorType in listOf(
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_LINEAR_ACCELERATION,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_LIGHT,
            Sensor.TYPE_PROXIMITY
        )
    }

    val knownSensorTypes = mapOf(
        Sensor.TYPE_ACCELEROMETER to "Accelerometer",
        Sensor.TYPE_GRAVITY to "Gravity Sensor",
        Sensor.TYPE_LINEAR_ACCELERATION to "Linear Acceleration",
        Sensor.TYPE_GYROSCOPE to "Gyroscope",
        Sensor.TYPE_MAGNETIC_FIELD to "Magnetic Field Sensor",
        Sensor.TYPE_ROTATION_VECTOR to "Rotation Vector",
        Sensor.TYPE_LIGHT to "Light Sensor",
        Sensor.TYPE_PROXIMITY to "Proximity Sensor",
        Sensor.TYPE_STEP_COUNTER to "Step Counter",
        Sensor.TYPE_STEP_DETECTOR to "Step Detector",
        Sensor.TYPE_PRESSURE to "Barometer",
        Sensor.TYPE_AMBIENT_TEMPERATURE to "Ambient Temperature",
        Sensor.TYPE_RELATIVE_HUMIDITY to "Relative Humidity",
        @Suppress("DEPRECATION") Sensor.TYPE_ORIENTATION to "Orientation Sensor",
        Sensor.TYPE_STATIONARY_DETECT to "Stationary Detector",
        Sensor.TYPE_MOTION_DETECT to "Motion Detector",
        Sensor.TYPE_HEART_RATE to "Heart Rate Monitor",
        Sensor.TYPE_POSE_6DOF to "Pose 6DOF",
        Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT to "Offbody Detector"
    )
}
