package com.pradeep.jarviscollector.repository

import android.content.Context
import com.pradeep.jarviscollector.database.JarvisDatabase
import com.pradeep.jarviscollector.model.DailyBriefEntity
import kotlinx.coroutines.flow.Flow

object DailyBriefRepository {

    private fun getDao(context: Context) = JarvisDatabase.getDatabase(context).dailyBriefDao()

    suspend fun getLatest(context: Context): DailyBriefEntity? {
        return getDao(context).getLatest()
    }

    suspend fun getLatestByType(context: Context, type: String): DailyBriefEntity? {
        return getDao(context).getLatestByType(type)
    }

    fun getLatestFlow(context: Context): Flow<DailyBriefEntity?> {
        return getDao(context).getLatestFlow()
    }

    fun getLatestFlowByType(context: Context, type: String): Flow<DailyBriefEntity?> {
        return getDao(context).getLatestFlowByType(type)
    }

    suspend fun getAll(context: Context): List<DailyBriefEntity> {
        return getDao(context).getAll()
    }
}
