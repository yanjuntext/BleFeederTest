package com.jk.blefeeder.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jk.blefeeder.bean.LocalSet

@Database(
    entities = [LocalSet::class],
    version = 1,
    exportSchema = false
)
abstract class AppDataBase : RoomDatabase() {
    abstract fun getLocalSerDao(): LocalSetDao


    companion object {
        @Volatile
        private var instance: AppDataBase? = null

        fun getInstance(context: Context): AppDataBase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): AppDataBase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDataBase::class.java,
                "JkBleFeederTest"
            )
                .build()
        }
    }

}