package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.dao.AuditRecordDao
import com.example.data.model.AuditConverters
import com.example.data.model.AuditRecord

@Database(entities = [AuditRecord::class], version = 1, exportSchema = false)
@TypeConverters(AuditConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun auditRecordDao(): AuditRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cooler_audit_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
