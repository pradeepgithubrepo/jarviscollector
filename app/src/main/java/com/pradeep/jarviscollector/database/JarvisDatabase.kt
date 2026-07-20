package com.pradeep.jarviscollector.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.pradeep.jarviscollector.model.MobileSignal
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

@Database(
    entities = [
        MobileSignal::class,
        TodoEntity::class,
        FinancialEventEntity::class,
        FyiEventEntity::class,
        UserPreferenceEntity::class,
        UserActionEntity::class,
        DailyBriefEntity::class,
        FactInsightEntity::class,
        NotificationEntity::class,
        FinancialInsightEntity::class,
        SyncDiagnosticsEntity::class,
        ReminderEntity::class,
        MonthlySpendingSummaryEntity::class,
        MonthlyCategorySpendEntity::class,
        LifecycleItemEntity::class,
        VaultCategoryEntity::class,
        VaultEntryEntity::class
    ],
    version = 13,
    exportSchema = false
)
abstract class JarvisDatabase : RoomDatabase() {

    abstract fun mobileSignalDao(): MobileSignalDao
    abstract fun todoDao(): TodoDao
    abstract fun financialEventDao(): FinancialEventDao
    abstract fun fyiEventDao(): FyiEventDao
    abstract fun userPreferenceDao(): UserPreferenceDao
    abstract fun userActionDao(): UserActionDao
    abstract fun dailyBriefDao(): DailyBriefDao
    abstract fun factInsightDao(): FactInsightDao
    abstract fun notificationDao(): NotificationDao
    abstract fun financialInsightDao(): FinancialInsightDao
    abstract fun syncDiagnosticsDao(): SyncDiagnosticsDao
    abstract fun reminderDao(): ReminderDao
    abstract fun monthlySpendingSummaryDao(): MonthlySpendingSummaryDao
    abstract fun monthlyCategorySpendDao(): MonthlyCategorySpendDao
    abstract fun lifecycleItemDao(): LifecycleItemDao
    abstract fun vaultCategoryDao(): VaultCategoryDao
    abstract fun vaultEntryDao(): VaultEntryDao


    companion object {
        private var INSTANCE: JarvisDatabase? = null

        fun getDatabase(context: Context): JarvisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JarvisDatabase::class.java,
                    "jarvis_mobile.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}