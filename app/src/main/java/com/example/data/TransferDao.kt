package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfer_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<TransferLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TransferLog): Long

    @Query("DELETE FROM transfer_logs")
    suspend fun clearLogs()

    @Query("SELECT SUM(transferredPercent) FROM transfer_logs")
    fun getTotalPercentTransferred(): Flow<Int?>
}
