package com.pradeep.jarviscollector.repository

import android.content.Context

import com.pradeep.jarviscollector.database.JarvisDatabase
import com.pradeep.jarviscollector.model.MobileSignal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object MobileSignalRepository {

    private const val DUPLICATE_WINDOW_MS = 10000L
    private val saveMutex = Mutex()

    suspend fun save(
        context: Context,
        signal: MobileSignal
    ) {

        saveMutex.withLock {
            withContext(
                Dispatchers.IO
            ) {

                val dao = JarvisDatabase
                    .getDatabase(
                        context
                    )
                    .mobileSignalDao()

                // 1. Check for exact duplicate (e.g. SMS already imported)
                val exactExists = dao.exists(
                    sender = signal.sender,
                    message = signal.message,
                    timestamp = signal.timestamp,
                    source = signal.source
                )
                if (exactExists) {
                    return@withContext
                }

                // 2. Check for duplicate within time window (e.g. rapid notification updates)
                val duplicateExists = dao.hasDuplicate(
                    sender = signal.sender,
                    message = signal.message,
                    timestamp = signal.timestamp,
                    windowMs = DUPLICATE_WINDOW_MS
                )
                if (duplicateExists) {
                    return@withContext
                }

                dao.insert(
                    signal
                )
            }
        }
    }

    suspend fun exists(
        context: Context,
        signal: MobileSignal
    ): Boolean {

        return withContext(
            Dispatchers.IO
        ) {

            JarvisDatabase
                .getDatabase(
                    context
                )
                .mobileSignalDao()
                .exists(
                    sender = signal.sender,
                    message = signal.message,
                    timestamp = signal.timestamp,
                    source = signal.source
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

    suspend fun markSynced(
        context: Context,
        ids: List<Int>
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
                    ids
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