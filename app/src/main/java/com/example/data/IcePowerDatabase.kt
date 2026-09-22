package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TransferLog::class], version = 1, exportSchema = false)
abstract class IcePowerDatabase : RoomDatabase() {
    abstract fun transferDao(): TransferDao

    companion object {
        @Volatile
        private var INSTANCE: IcePowerDatabase? = null

        fun getInstance(context: Context): IcePowerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    IcePowerDatabase::class.java,
                    "icepower_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
