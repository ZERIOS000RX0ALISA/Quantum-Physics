package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "sensor_samples",
    indices = [Index(value = ["projectId"]), Index(value = ["projectId", "sensorType"])]
)
data class SensorSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Int,
    val timestamp: Long,
    val elapsedMs: Long,
    val sensorType: String,
    val valueX: Float,
    val valueY: Float,
    val valueZ: Float
)
