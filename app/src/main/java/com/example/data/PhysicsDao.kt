package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PhysicsDao {
    @Query("SELECT * FROM projects ORDER BY creationDate DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectById(id: Int): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectByIdSync(id: Int): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Int)

    @Query("SELECT * FROM sensor_samples WHERE projectId = :projectId ORDER BY elapsedMs ASC")
    fun getSamplesForProject(projectId: Int): Flow<List<SensorSampleEntity>>

    @Query("SELECT * FROM sensor_samples WHERE projectId = :projectId AND sensorType = :sensorType ORDER BY elapsedMs ASC")
    fun getSamplesForProjectAndSensor(projectId: Int, sensorType: String): Flow<List<SensorSampleEntity>>

    @Query("SELECT * FROM sensor_samples WHERE projectId = :projectId ORDER BY elapsedMs ASC")
    suspend fun getSamplesForProjectSync(projectId: Int): List<SensorSampleEntity>

    @Query("SELECT * FROM sensor_samples WHERE projectId = :projectId AND sensorType = :sensorType ORDER BY elapsedMs ASC")
    suspend fun getSamplesForProjectAndSensorSync(projectId: Int, sensorType: String): List<SensorSampleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSamples(samples: List<SensorSampleEntity>)

    @Query("DELETE FROM sensor_samples WHERE projectId = :projectId")
    suspend fun deleteSamplesForProject(projectId: Int)
}
