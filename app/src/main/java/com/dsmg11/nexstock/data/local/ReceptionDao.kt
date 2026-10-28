package com.dsmg11.nexstock.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceptionDao {

    // ABORT: una recepción nunca se sobrescribe
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(reception: ReceptionEntity)

    @Query("SELECT * FROM receptions WHERE orderId = :orderId ORDER BY receivedAt DESC")
    fun observeByOrder(orderId: String): Flow<List<ReceptionEntity>>

    // --- Sincronización (MOV-51) ---

    @Query("SELECT * FROM receptions WHERE pendingSync = 1 ORDER BY receivedAt ASC")
    suspend fun getPending(): List<ReceptionEntity>

    @Query("SELECT * FROM receptions WHERE pendingSync = 1 ORDER BY receivedAt ASC")
    fun observePending(): Flow<List<ReceptionEntity>>

    @Query("UPDATE receptions SET pendingSync = 0 WHERE id = :id")
    suspend fun markSynced(id: String)
}
