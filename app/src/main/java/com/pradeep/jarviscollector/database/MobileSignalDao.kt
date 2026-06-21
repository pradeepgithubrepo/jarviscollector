package com.pradeep.jarviscollector.database


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

import com.pradeep.jarviscollector.model.MobileSignal


@Dao
interface MobileSignalDao {


    @Insert
    suspend fun insert(
        signal: MobileSignal
    )


    @Query(
        """
        SELECT *
        FROM mobile_signals
        ORDER BY timestamp DESC
        """
    )
    suspend fun getAll():
            List<MobileSignal>



    @Query(
        """
        SELECT *
        FROM mobile_signals
        WHERE syncStatus = 'PENDING'
        """
    )
    suspend fun getPendingSignals():
            List<MobileSignal>



    @Query(
        """
        UPDATE mobile_signals
        SET syncStatus='SYNCED'
        WHERE id=:id
        """
    )
    suspend fun markSynced(
        id: Int
    )

    @Query(
        """
    UPDATE mobile_signals
    SET syncStatus='SYNCED'
    WHERE syncStatus='PENDING'
    """
    )
    suspend fun markAllSynced()
}