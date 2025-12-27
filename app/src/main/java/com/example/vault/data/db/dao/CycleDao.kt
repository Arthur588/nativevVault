package com.example.vault.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.vault.data.model.CycleState
import kotlinx.coroutines.flow.Flow

@Dao
interface CycleDao {
    @Query("SELECT * FROM cycle_state WHERE id = 0")
    fun observeState(): Flow<CycleState?>

    @Query("SELECT * FROM cycle_state WHERE id = 0")
    suspend fun getState(): CycleState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(state: CycleState)

    @Update
    suspend fun update(state: CycleState)
}