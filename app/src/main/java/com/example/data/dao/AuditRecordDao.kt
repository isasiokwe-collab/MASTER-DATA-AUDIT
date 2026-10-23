package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AuditRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditRecordDao {

    @Query("SELECT * FROM audit_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<AuditRecord>>

    @Query("SELECT * FROM audit_records WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: Long): AuditRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AuditRecord): Long

    @Update
    suspend fun updateRecord(record: AuditRecord)

    @Delete
    suspend fun deleteRecord(record: AuditRecord)

    @Query("DELETE FROM audit_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    @Query("SELECT COUNT(*) FROM audit_records WHERE timestamp >= :todayStart")
    fun getTodayCount(todayStart: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM audit_records WHERE timestamp >= :todayStart AND formType = 'WITH_COOLER'")
    fun getTodayWithCoolerCount(todayStart: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM audit_records WHERE timestamp >= :todayStart AND formType = 'WITHOUT_COOLER'")
    fun getTodayWithoutCoolerCount(todayStart: Long): Flow<Int>

    // Distinct values for quick-pick suggestions
    @Query("SELECT DISTINCT accountDistributor FROM audit_records WHERE accountDistributor != '' ORDER BY timestamp DESC LIMIT 15")
    fun getDistinctAccountDistributors(): Flow<List<String>>

    @Query("SELECT DISTINCT location FROM audit_records WHERE location != '' ORDER BY timestamp DESC LIMIT 15")
    fun getDistinctLocations(): Flow<List<String>>

    @Query("SELECT DISTINCT coolerMake FROM audit_records WHERE coolerMake != '' ORDER BY timestamp DESC LIMIT 10")
    fun getDistinctCoolerMakes(): Flow<List<String>>

    // Check for duplicate serial number or tag number
    @Query("""
        SELECT * FROM audit_records 
        WHERE id != :excludeId AND (
            (:serial != '' AND coolerSerialNumber = :serial) OR 
            (:tag != '' AND coolerTagNumber = :tag)
        )
        LIMIT 5
    """)
    suspend fun findDuplicates(serial: String, tag: String, excludeId: Long): List<AuditRecord>

    // Export query
    @Query("SELECT * FROM audit_records WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp DESC")
    suspend fun getRecordsInRange(start: Long, end: Long): List<AuditRecord>

    @Query("SELECT * FROM audit_records ORDER BY timestamp DESC")
    suspend fun getAllRecordsSnapshot(): List<AuditRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<AuditRecord>)

    @Query("DELETE FROM audit_records")
    suspend fun clearAllRecords()
}
