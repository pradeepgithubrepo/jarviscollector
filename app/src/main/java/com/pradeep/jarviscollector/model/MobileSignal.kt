package com.pradeep.jarviscollector.model


import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(
    tableName = "mobile_signals"
)
data class MobileSignal(

    @PrimaryKey(
        autoGenerate = true
    )
    val id: Int = 0,


    // pradeep_phone / shobana_phone
    val deviceId: String,


    // whatsapp / sms
    val source: String,


    // person/group/bank name
    val sender: String,


    val message: String,


    val timestamp: Long,


    // PENDING / SYNCED
    val syncStatus: String = "PENDING"
)