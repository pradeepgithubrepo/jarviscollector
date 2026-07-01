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
import com.pradeep.jarviscollector.model.FactInsightEntity
import com.pradeep.jarviscollector.model.NotificationEntity
import com.pradeep.jarviscollector.model.FinancialInsightEntity
import com.pradeep.jarviscollector.model.SyncDiagnosticsEntity
import kotlinx.coroutines.flow.Flow

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

    @Query("SELECT * FROM todos ORDER BY due_date ASC")
    fun getAllFlow(): Flow<List<TodoEntity>>

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

    @Query("SELECT * FROM financial_events ORDER BY event_timestamp DESC")
    fun getAllFlow(): Flow<List<FinancialEventEntity>>

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(actions: List<UserActionEntity>)

    @Query("SELECT * FROM user_actions ORDER BY action_timestamp DESC")
    suspend fun getAll(): List<UserActionEntity>

    @Query("SELECT * FROM user_actions ORDER BY action_timestamp DESC")
    fun getAllFlow(): Flow<List<UserActionEntity>>

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

    @Query("SELECT * FROM daily_briefs ORDER BY generatedAt DESC LIMIT 1")
    fun getLatestFlow(): Flow<DailyBriefEntity?>

    @Query("SELECT * FROM daily_briefs WHERE briefType = :type ORDER BY generatedAt DESC LIMIT 1")
    suspend fun getLatestByType(type: String): DailyBriefEntity?

    @Query("SELECT * FROM daily_briefs WHERE briefType = :type ORDER BY generatedAt DESC LIMIT 1")
    fun getLatestFlowByType(type: String): Flow<DailyBriefEntity?>

    @Query("SELECT * FROM daily_briefs ORDER BY generatedAt DESC")
    suspend fun getAll(): List<DailyBriefEntity>

    @Query("DELETE FROM daily_briefs")
    suspend fun deleteAll()
}

@Dao
interface FactInsightDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(facts: List<FactInsightEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(fact: FactInsightEntity)

    @Query("SELECT * FROM facts ORDER BY created_at DESC")
    suspend fun getAll(): List<FactInsightEntity>

    @Query("SELECT * FROM facts ORDER BY created_at DESC")
    fun getAllFlow(): Flow<List<FactInsightEntity>>

    @Query("UPDATE facts SET read_flag = :readFlag, status = :status WHERE id = :id")
    suspend fun updateReadStatus(id: String, readFlag: Boolean, status: String)

    @Query("DELETE FROM facts")
    suspend fun deleteAll()
}

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<NotificationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)

    @Query("SELECT * FROM notifications ORDER BY created_at DESC")
    suspend fun getAll(): List<NotificationEntity>

    @Query("SELECT * FROM notifications ORDER BY created_at DESC")
    fun getAllFlow(): Flow<List<NotificationEntity>>

    @Query("UPDATE notifications SET status = :status, read_flag = :readFlag WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, readFlag: Boolean)

    @Query("DELETE FROM notifications")
    suspend fun deleteAll()
}

@Dao
interface FinancialInsightDao {
    @Query("SELECT * FROM financial_insights ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<FinancialInsightEntity>>

    @Query("SELECT * FROM financial_insights WHERE status = 'PENDING' OR status = 'pending' ORDER BY createdAt DESC")
    fun observePending(): Flow<List<FinancialInsightEntity>>

    @Query("SELECT * FROM financial_insights WHERE type = 'subscription' ORDER BY createdAt DESC")
    fun observeSubscriptions(): Flow<List<FinancialInsightEntity>>

    @Query("SELECT * FROM financial_insights WHERE type = 'bill' ORDER BY createdAt DESC")
    fun observeBills(): Flow<List<FinancialInsightEntity>>

    @Query("SELECT * FROM financial_insights WHERE type = 'unusual' ORDER BY createdAt DESC")
    fun observeUnusualActivity(): Flow<List<FinancialInsightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(insights: List<FinancialInsightEntity>)

    @Query("UPDATE financial_insights SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("SELECT * FROM financial_insights")
    suspend fun getAll(): List<FinancialInsightEntity>

    @Query("DELETE FROM financial_insights")
    suspend fun deleteAll()
}

@Dao
interface SyncDiagnosticsDao {
    @Query("SELECT * FROM sync_diagnostics")
    fun observeAll(): Flow<List<SyncDiagnosticsEntity>>

    @Query("SELECT * FROM sync_diagnostics")
    suspend fun getAll(): List<SyncDiagnosticsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(diagnostics: SyncDiagnosticsEntity)

    @Query("DELETE FROM sync_diagnostics")
    suspend fun deleteAll()
}
