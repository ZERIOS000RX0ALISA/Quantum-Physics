package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ProjectEntity::class, SensorSampleEntity::class], version = 1, exportSchema = false)
abstract class PhysicsDatabase : RoomDatabase() {
    abstract fun physicsDao(): PhysicsDao

    companion object {
        @Volatile
        private var INSTANCE: PhysicsDatabase? = null

        fun getDatabase(context: Context): PhysicsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PhysicsDatabase::class.java,
                    "quantum_physics_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
