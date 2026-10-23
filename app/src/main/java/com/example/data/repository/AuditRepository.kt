package com.example.data.repository

import com.example.data.dao.AuditRecordDao
import com.example.data.model.AuditRecord
import kotlinx.coroutines.flow.Flow

class AuditRepository(private val dao: AuditRecordDao) {

    val allRecords: Flow<List<AuditRecord>> = dao.getAllRecords()
    val accountSuggestions: Flow<List<String>> = dao.getDistinctAccountDistributors()
    val locationSuggestions: Flow<List<String>> = dao.getDistinctLocations()
    val coolerMakeSuggestions: Flow<List<String>> = dao.getDistinctCoolerMakes()

    fun getTodayCount(todayStart: Long): Flow<Int> = dao.getTodayCount(todayStart)
    fun getTodayWithCoolerCount(todayStart: Long): Flow<Int> = dao.getTodayWithCoolerCount(todayStart)
    fun getTodayWithoutCoolerCount(todayStart: Long): Flow<Int> = dao.getTodayWithoutCoolerCount(todayStart)

    suspend fun insertRecord(record: AuditRecord): Long = dao.insertRecord(record)

    suspend fun updateRecord(record: AuditRecord) = dao.updateRecord(record)

    suspend fun deleteRecord(record: AuditRecord) = dao.deleteRecord(record)

    suspend fun deleteRecordById(id: Long) = dao.deleteRecordById(id)

    suspend fun getRecordById(id: Long): AuditRecord? = dao.getRecordById(id)

    suspend fun findDuplicates(serial: String, tag: String, excludeId: Long): List<AuditRecord> {
        return dao.findDuplicates(serial.trim(), tag.trim(), excludeId)
    }

    suspend fun getRecordsInRange(start: Long, end: Long): List<AuditRecord> {
        return dao.getRecordsInRange(start, end)
    }

    suspend fun getAllRecordsSnapshot(): List<AuditRecord> {
        return dao.getAllRecordsSnapshot()
    }

    suspend fun restoreRecords(records: List<AuditRecord>, cleanReplace: Boolean) {
        if (cleanReplace) {
            dao.clearAllRecords()
        }
        dao.insertRecords(records)
    }
}

