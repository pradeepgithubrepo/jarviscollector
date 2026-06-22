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
        WHERE id IN (:ids)
        """
    )
    suspend fun markSynced(
        ids: List<Int>
    )

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM mobile_signals 
            WHERE sender = :sender 
              AND message = :message 
              AND timestamp = :timestamp
              AND source = :source
        )
        """
    )
    suspend fun exists(
        sender: String,
        message: String,
        timestamp: Long,
        source: String
    ): Boolean

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM mobile_signals 
            WHERE sender = :sender 
              AND message = :message 
              AND abs(timestamp - :timestamp) <= :windowMs
        )
        """
    )
    suspend fun hasDuplicate(
        sender: String,
        message: String,
        timestamp: Long,
        windowMs: Long
    ): Boolean

    @Query(
        """
    UPDATE mobile_signals
    SET syncStatus='SYNCED'
    WHERE syncStatus='PENDING'
    """
    )
    suspend fun markAllSynced()

    @Query(
        """
        SELECT *
        FROM mobile_signals
        WHERE source = :source
        ORDER BY timestamp DESC
        """
    )
    suspend fun getSignalsBySource(source: String): List<MobileSignal>
}