package com.pradeep.jarviscollector.database


import android.content.Context

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import com.pradeep.jarviscollector.model.MobileSignal


@Database(

    entities = [
        MobileSignal::class
    ],

    version = 1
)

abstract class JarvisDatabase :
    RoomDatabase() {


    abstract fun mobileSignalDao():
            MobileSignalDao



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
                            .build()


                    INSTANCE =
                        instance


                    instance
                }
        }
    }
}