package com.pradeep.jarviscollector.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pradeep.jarviscollector.model.TodoEntity
import com.pradeep.jarviscollector.model.FinancialEventEntity
import com.pradeep.jarviscollector.model.FyiEventEntity
import com.pradeep.jarviscollector.model.DailyBriefEntity

@Dao
interface TodoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(todos: List<TodoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todo: TodoEntity)

    @Update
    suspend fun update(todo: TodoEntity)

    @Query("SELECT * FROM todos ORDER BY dueDate ASC")
    suspend fun getAll(): List<TodoEntity>

    @Query("SELECT * FROM todos WHERE status = 'pending' ORDER BY dueDate ASC")
    suspend fun getPending(): List<TodoEntity>

    @Query("SELECT * FROM todos WHERE status = 'completed' ORDER BY completedAt DESC")
    suspend fun getCompleted(): List<TodoEntity>

    @Query("UPDATE todos SET status = :status, completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, completedAt: String?, updatedAt: Long)

    @Query("UPDATE todos SET snoozeCount = snoozeCount + 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun snoozeTodo(id: String, updatedAt: Long)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM todos")
    suspend fun deleteAll()
}

@Dao
interface FinancialEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<FinancialEventEntity>)

    @Query("SELECT * FROM financial_events ORDER BY dueDate ASC")
    suspend fun getAll(): List<FinancialEventEntity>

    @Query("DELETE FROM financial_events")
    suspend fun deleteAll()
}

@Dao
interface FyiEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<FyiEventEntity>)

    @Query("SELECT * FROM fyi_events ORDER BY timestamp DESC")
    suspend fun getAll(): List<FyiEventEntity>

    @Query("DELETE FROM fyi_events")
    suspend fun deleteAll()
}

@Dao
interface DailyBriefDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(brief: DailyBriefEntity)

    @Query("SELECT * FROM daily_briefs ORDER BY generatedAt DESC LIMIT 1")
    suspend fun getLatest(): DailyBriefEntity?

    @Query("SELECT * FROM daily_briefs ORDER BY generatedAt DESC")
    suspend fun getAll(): List<DailyBriefEntity>

    @Query("DELETE FROM daily_briefs")
    suspend fun deleteAll()
}
