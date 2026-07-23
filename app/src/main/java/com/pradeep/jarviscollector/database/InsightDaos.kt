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
import com.pradeep.jarviscollector.model.ReminderEntity
import com.pradeep.jarviscollector.model.MonthlySpendingSummaryEntity
import com.pradeep.jarviscollector.model.MonthlyCategorySpendEntity
import com.pradeep.jarviscollector.model.LifecycleItemEntity
import com.pradeep.jarviscollector.model.VaultCategoryEntity
import com.pradeep.jarviscollector.model.VaultEntryEntity
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

    @Query("SELECT * FROM financial_events WHERE (is_self_transfer IS NULL OR is_self_transfer = 0) ORDER BY event_timestamp DESC")
    suspend fun getAllNonTransfers(): List<FinancialEventEntity>

    @Query("SELECT * FROM financial_events WHERE substr(event_timestamp, 1, 7) = :monthKey AND (is_self_transfer IS NULL OR is_self_transfer = 0) ORDER BY event_timestamp DESC")
    suspend fun getByMonth(monthKey: String): List<FinancialEventEntity>

    @Query("SELECT * FROM financial_events WHERE substr(event_timestamp, 1, 7) = :monthKey AND category = :category AND (is_self_transfer IS NULL OR is_self_transfer = 0) ORDER BY event_timestamp DESC")
    suspend fun getByMonthAndCategory(monthKey: String, category: String): List<FinancialEventEntity>

    @Query("SELECT * FROM financial_events WHERE substr(event_timestamp, 1, 7) = :monthKey AND (is_self_transfer IS NULL OR is_self_transfer = 0) ORDER BY amount DESC")
    suspend fun getByMonthSortedByMaxSpend(monthKey: String): List<FinancialEventEntity>

    @Query("DELETE FROM financial_events")
    suspend fun deleteAll()

    @Query("DELETE FROM financial_events WHERE financial_event_id = :id")
    suspend fun deleteById(id: String)
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

    @Query("DELETE FROM facts WHERE id = :id")
    suspend fun deleteById(id: String)

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

@Dao
interface ReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity)

    @Query("SELECT * FROM local_reminders WHERE reminder_id = :id LIMIT 1")
    suspend fun getById(id: String): ReminderEntity?

    @Query("SELECT * FROM local_reminders WHERE reminder_id = :id LIMIT 1")
    fun getByIdFlow(id: String): Flow<ReminderEntity?>

    @Query("SELECT * FROM local_reminders ORDER BY scheduled_timestamp ASC")
    suspend fun getAll(): List<ReminderEntity>

    @Query("SELECT * FROM local_reminders ORDER BY scheduled_timestamp ASC")
    fun getAllFlow(): Flow<List<ReminderEntity>>

    @Query("DELETE FROM local_reminders WHERE reminder_id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM local_reminders")
    suspend fun deleteAll()
}

@Dao
interface MonthlySpendingSummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MonthlySpendingSummaryEntity>)

    @Query("SELECT * FROM monthly_spending_summary ORDER BY month_key DESC")
    suspend fun getAll(): List<MonthlySpendingSummaryEntity>

    @Query("SELECT * FROM monthly_spending_summary ORDER BY month_key DESC")
    fun getAllFlow(): Flow<List<MonthlySpendingSummaryEntity>>

    @Query("SELECT * FROM monthly_spending_summary ORDER BY month_key DESC LIMIT 1")
    suspend fun getLatest(): MonthlySpendingSummaryEntity?

    @Query("DELETE FROM monthly_spending_summary")
    suspend fun deleteAll()
}

@Dao
interface MonthlyCategorySpendDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MonthlyCategorySpendEntity>)

    @Query("SELECT * FROM monthly_category_spend WHERE month_key = :monthKey ORDER BY amount DESC")
    suspend fun getForMonth(monthKey: String): List<MonthlyCategorySpendEntity>

    @Query("SELECT * FROM monthly_category_spend ORDER BY month_key DESC, amount DESC")
    fun getAllFlow(): Flow<List<MonthlyCategorySpendEntity>>

    @Query("SELECT :monthKey as month_key, category, SUM(amount) as amount, SUM(transaction_count) as transaction_count FROM monthly_category_spend WHERE month_key <= :monthKey GROUP BY category")
    suspend fun getCumulativeSpend(monthKey: String): List<MonthlyCategorySpendEntity>

    @Query("DELETE FROM monthly_category_spend")
    suspend fun deleteAll()
}

@Dao
interface LifecycleItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<LifecycleItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: LifecycleItemEntity)

    @Query("SELECT * FROM lifecycle_items ORDER BY next_occurrence_date ASC")
    suspend fun getAll(): List<LifecycleItemEntity>

    @Query("SELECT * FROM lifecycle_items ORDER BY next_occurrence_date ASC")
    fun getAllFlow(): Flow<List<LifecycleItemEntity>>

    @Query("DELETE FROM lifecycle_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM lifecycle_items")
    suspend fun deleteAll()
}

@Dao
interface VaultCategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<VaultCategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: VaultCategoryEntity)

    @Query("SELECT * FROM vault_categories ORDER BY display_order ASC, category_name ASC")
    suspend fun getAll(): List<VaultCategoryEntity>

    @Query("SELECT * FROM vault_categories ORDER BY display_order ASC, category_name ASC")
    fun getAllFlow(): Flow<List<VaultCategoryEntity>>

    @Query("DELETE FROM vault_categories")
    suspend fun deleteAll()
}

@Dao
interface VaultEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<VaultEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: VaultEntryEntity)

    @Query("SELECT * FROM vault_entries WHERE vault_category_id = :categoryId OR vault_category_id = :categoryName OR LOWER(vault_category_id) = LOWER(:categoryName) ORDER BY sort_order ASC, title ASC")
    suspend fun getForCategory(categoryId: String, categoryName: String): List<VaultEntryEntity>

    @Query("SELECT * FROM vault_entries WHERE vault_category_id = :categoryId OR vault_category_id = :categoryName OR LOWER(vault_category_id) = LOWER(:categoryName) ORDER BY sort_order ASC, title ASC")
    fun getForCategoryFlow(categoryId: String, categoryName: String): Flow<List<VaultEntryEntity>>

    @Query("SELECT * FROM vault_entries WHERE vault_category_id = :categoryId ORDER BY sort_order ASC, title ASC")
    suspend fun getForCategoryId(categoryId: String): List<VaultEntryEntity>

    @Query("SELECT * FROM vault_entries WHERE vault_category_id = :categoryId ORDER BY sort_order ASC, title ASC")
    fun getForCategoryIdFlow(categoryId: String): Flow<List<VaultEntryEntity>>

    @Query("SELECT * FROM vault_entries ORDER BY sort_order ASC, title ASC")
    suspend fun getAll(): List<VaultEntryEntity>

    @Query("SELECT * FROM vault_entries ORDER BY sort_order ASC, title ASC")
    fun getAllFlow(): Flow<List<VaultEntryEntity>>

    @Query("DELETE FROM vault_entries WHERE vault_entry_id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM vault_entries")
    suspend fun deleteAll()
}



