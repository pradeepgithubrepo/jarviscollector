package com.pradeep.jarviscollector.repository

import android.content.Context
import com.pradeep.jarviscollector.database.JarvisDatabase
import com.pradeep.jarviscollector.model.DailyBriefEntity
import com.pradeep.jarviscollector.model.FinancialEventEntity
import com.pradeep.jarviscollector.model.FyiEventEntity
import com.pradeep.jarviscollector.model.TodoEntity

object InsightsRepository {

    private fun getDb(context: Context) = JarvisDatabase.getDatabase(context)

    suspend fun getTodos(
        context: Context
    ): List<TodoEntity> {
        return getDb(context).todoDao().getAll()
    }

    suspend fun getPendingTodos(
        context: Context
    ): List<TodoEntity> {
        return getDb(context).todoDao().getPending()
    }

    suspend fun getCompletedTodos(
        context: Context
    ): List<TodoEntity> {
        return getDb(context).todoDao().getCompleted()
    }

    suspend fun markTodoComplete(
        context: Context,
        id: String
    ) {
        val completedAt =
            System.currentTimeMillis().toString()

        getDb(context)
            .todoDao()
            .updateStatus(
                id = id,
                status = "completed",
                completedAt = completedAt,
                updatedAt = System.currentTimeMillis()
            )
    }

    suspend fun snoozeTodo(
        context: Context,
        id: String
    ) {
        getDb(context)
            .todoDao()
            .snoozeTodo(
                id = id,
                updatedAt = System.currentTimeMillis()
            )
    }

    suspend fun deleteTodo(
        context: Context,
        id: String
    ) {
        getDb(context)
            .todoDao()
            .deleteById(id)
    }

    suspend fun getFinancialEvents(
        context: Context
    ): List<FinancialEventEntity> {
        return getDb(context).financialEventDao().getAll()
    }

    suspend fun getFyiEvents(
        context: Context
    ): List<FyiEventEntity> {
        return getDb(context).fyiEventDao().getAll()
    }

    suspend fun getLatestDailyBrief(
        context: Context
    ): DailyBriefEntity? {
        return getDb(context).dailyBriefDao().getLatest()
    }
}
