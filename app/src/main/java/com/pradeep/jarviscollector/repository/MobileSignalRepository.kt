package com.pradeep.jarviscollector.repository

import android.content.Context

import com.pradeep.jarviscollector.database.JarvisDatabase
import com.pradeep.jarviscollector.model.MobileSignal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MobileSignalRepository {

    suspend fun save(
        context: Context,
        signal: MobileSignal
    ) {

        withContext(
            Dispatchers.IO
        ) {

            JarvisDatabase
                .getDatabase(
                    context
                )
                .mobileSignalDao()
                .insert(
                    signal
                )
        }
    }

    suspend fun getSignals(
        context: Context
    ): List<MobileSignal> {

        return withContext(
            Dispatchers.IO
        ) {

            JarvisDatabase
                .getDatabase(
                    context
                )
                .mobileSignalDao()
                .getAll()
        }
    }

    suspend fun getPendingSignals(
        context: Context
    ): List<MobileSignal> {

        return withContext(
            Dispatchers.IO
        ) {

            JarvisDatabase
                .getDatabase(
                    context
                )
                .mobileSignalDao()
                .getPendingSignals()
        }
    }


    suspend fun markSynced(
        context: Context,
        id: Int
    ) {

        withContext(
            Dispatchers.IO
        ) {

            JarvisDatabase
                .getDatabase(
                    context
                )
                .mobileSignalDao()
                .markSynced(
                    id
                )
        }
    }

    suspend fun markAllSynced(
        context: Context
    ) {

        withContext(
            Dispatchers.IO
        ) {

            JarvisDatabase
                .getDatabase(context)
                .mobileSignalDao()
                .markAllSynced()
        }
    }


}