package com.pradeep.jarviscollector.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pradeep.jarviscollector.model.TodoEntity
import com.pradeep.jarviscollector.model.FinancialEventEntity
import com.pradeep.jarviscollector.model.FyiEventEntity
import com.pradeep.jarviscollector.model.UserPreferenceEntity
import com.pradeep.jarviscollector.model.UserActionEntity
import com.pradeep.jarviscollector.model.DailyBriefEntity

@Dao
interface TodoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(todos: List<TodoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todo: TodoEntity)

    @Update
    suspend fun update(todo: TodoEntity)

    @Query("SELECT * FROM todos ORDER BY due_date ASC")
    suspend fun getAll(): List<TodoEntity>

    @Query("SELECT * FROM todos WHERE status = 'OPEN' ORDER BY due_date ASC")
    suspend fun getPending(): List<TodoEntity>

    @Query("SELECT * FROM todos WHERE status = 'COMPLETED' ORDER BY updated_at DESC")
    suspend fun getCompleted(): List<TodoEntity>

    @Query("UPDATE todos SET status = :status, updated_at = :updatedAt WHERE todo_id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: String)

    @Query("UPDATE todos SET status = 'SNOOZED', updated_at = :updatedAt WHERE todo_id = :id")
    suspend fun snoozeTodo(id: String, updatedAt: String)

    @Query("DELETE FROM todos WHERE todo_id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM todos")
    suspend fun deleteAll()
}

@Dao
interface FinancialEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<FinancialEventEntity>)

    @Query("SELECT * FROM financial_events ORDER BY event_timestamp DESC")
    suspend fun getAll(): List<FinancialEventEntity>

    @Query("DELETE FROM financial_events")
    suspend fun deleteAll()
}

@Dao
interface FyiEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<FyiEventEntity>)

    @Query("SELECT * FROM fyi_events ORDER BY created_at DESC")
    suspend fun getAll(): List<FyiEventEntity>

    @Query("DELETE FROM fyi_events")
    suspend fun deleteAll()
}

@Dao
interface UserPreferenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(preferences: List<UserPreferenceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preference: UserPreferenceEntity)

    @Query("SELECT * FROM user_preferences")
    suspend fun getAll(): List<UserPreferenceEntity>

    @Query("SELECT * FROM user_preferences WHERE preference_key = :key LIMIT 1")
    suspend fun getByKey(key: String): UserPreferenceEntity?

    @Query("DELETE FROM user_preferences WHERE preference_key = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM user_preferences")
    suspend fun deleteAll()
}

@Dao
interface UserActionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(action: UserActionEntity)

    @Query("SELECT * FROM user_actions ORDER BY action_timestamp ASC")
    suspend fun getAll(): List<UserActionEntity>

    @Query("DELETE FROM user_actions WHERE action_id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM user_actions")
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
