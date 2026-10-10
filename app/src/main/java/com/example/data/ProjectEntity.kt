package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val selectedSensors: String, // Comma-separated list of sensor names
    val samplingRateHz: Int,
    val durationSeconds: Int,
    val saveRawData: Boolean,
    val autoGenerateGraphs: Boolean,
    val creationDate: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val notes: String = ""
) {
    val formattedId: String
        get() = String.format("RS%05d", id)

    val folderName: String
        get() = "$formattedId - ${name.replace(Regex("[^a-zA-Z0-9 _-]"), "")}"
}
