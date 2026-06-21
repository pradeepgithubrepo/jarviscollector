package com.pradeep.jarviscollector.database


import android.content.Context

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import com.pradeep.jarviscollector.model.MobileSignal
import com.pradeep.jarviscollector.model.TodoEntity
import com.pradeep.jarviscollector.model.FinancialEventEntity
import com.pradeep.jarviscollector.model.FyiEventEntity
import com.pradeep.jarviscollector.model.DailyBriefEntity


@Database(

    entities = [
        MobileSignal::class,
        TodoEntity::class,
        FinancialEventEntity::class,
        FyiEventEntity::class,
        DailyBriefEntity::class
    ],

    version = 2
)

abstract class JarvisDatabase :
    RoomDatabase() {


    abstract fun mobileSignalDao():
            MobileSignalDao

    abstract fun todoDao(): TodoDao
    abstract fun financialEventDao(): FinancialEventDao
    abstract fun fyiEventDao(): FyiEventDao
    abstract fun dailyBriefDao(): DailyBriefDao


    companion object {


        private var INSTANCE:
                JarvisDatabase? = null



        fun getDatabase(
            context: Context
        ): JarvisDatabase {


            return INSTANCE
                ?: synchronized(this) {


                    val instance =
                        Room.databaseBuilder(

                            context.applicationContext,

                            JarvisDatabase::class.java,

                            "jarvis_mobile.db"

                        )
                            .fallbackToDestructiveMigration()
                            .build()


                    INSTANCE =
                        instance


                    instance
                }
        }
    }
}