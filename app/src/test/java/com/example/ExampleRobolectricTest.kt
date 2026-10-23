package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.model.AuditFormType
import com.example.data.model.AuditRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Cooler Audit Tracker", appName)
    }

    @Test
    fun `insert and retrieve audit record`() {
        runBlocking {
            val dao = db.auditRecordDao()
            val record = AuditRecord(
                formType = AuditFormType.WITH_COOLER,
                accountDistributor = "Eastern Distributors",
                outletName = "Sunrise Grocery",
                ownerName = "Kwame Asante",
                ownerPhone = "0244123456",
                location = "Accra Central",
                latitude = 5.6037,
                longitude = -0.1870,
                gpsAccuracyMeters = 12.5f,
                coolerMake = "Frigoglass",
                coolerModel = "FV-650",
                coolerSerialNumber = "SN-987654",
                coolerTagNumber = "TAG-1002"
            )

            val id = dao.insertRecord(record)
            val retrieved = dao.getRecordById(id)
            assertNotNull(retrieved)
            assertEquals("Sunrise Grocery", retrieved?.outletName)
            assertEquals("SN-987654", retrieved?.coolerSerialNumber)

            val duplicates = dao.findDuplicates("SN-987654", "TAG-DIFF", excludeId = 0L)
            assertEquals(1, duplicates.size)
        }
    }

    @Test
    fun `test daily target persistence in preferences`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = com.example.ui.viewmodel.AuditViewModel(app)
        
        // Initial target defaults to 30
        assertEquals(30, viewModel.dailyTarget.value)
        
        // Update to 45
        viewModel.updateDailyTarget(45)
        assertEquals(45, viewModel.dailyTarget.value)
        
        // Check SharedPreferences directly
        val prefs = app.getSharedPreferences("cooler_audit_prefs", Context.MODE_PRIVATE)
        assertEquals(45, prefs.getInt("daily_target", 30))
    }

    @Test
    fun `test backup creation and restore flow`() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<android.app.Application>()
            val records = listOf(
                AuditRecord(
                    id = 1L,
                    formType = AuditFormType.WITH_COOLER,
                    accountDistributor = "Alpha Drinks",
                    outletName = "Blue Star Mart",
                    ownerName = "John Doe",
                    ownerPhone = "0240001111",
                    location = "Tema Community 1",
                    latitude = 5.6698,
                    longitude = -0.0166,
                    gpsAccuracyMeters = 10.0f,
                    coolerMake = "Metalfrio",
                    coolerModel = "M500",
                    coolerSerialNumber = "SN-METAL-001",
                    coolerTagNumber = "TAG-001"
                )
            )

            // 1. Create backup file
            val backupFile = com.example.util.BackupHelper.createBackupFile(app, records, dailyTarget = 35)
            assertNotNull(backupFile)
            org.junit.Assert.assertTrue(backupFile!!.exists())

            // 2. Read back from file
            val fileUri = android.net.Uri.fromFile(backupFile)
            val parsedBackup = com.example.util.BackupHelper.readBackupFromUri(app, fileUri)
            assertNotNull(parsedBackup)
            assertEquals(35, parsedBackup?.dailyTarget)
            assertEquals(1, parsedBackup?.records?.size)
            assertEquals("Blue Star Mart", parsedBackup?.records?.get(0)?.outletName)

            // 3. Restore into repository
            val repository = com.example.data.repository.AuditRepository(db.auditRecordDao())
            repository.restoreRecords(parsedBackup!!.records, cleanReplace = true)
            val allRestored = repository.getAllRecordsSnapshot()
            assertEquals(1, allRestored.size)
            assertEquals("Blue Star Mart", allRestored[0].outletName)

            // Clean up
            backupFile.delete()
        }
    }

    @Test
    fun `test audit record timestamp and date formatting`() {
        val testTimestamp = 1776902400000L // specific fixed epoch
        val record = AuditRecord(
            id = 42L,
            formType = AuditFormType.WITH_COOLER,
            timestamp = testTimestamp,
            outletName = "Apex Mini Mart"
        )

        assertNotNull(record.formattedDate)
        assertNotNull(record.formattedTime)
        assertNotNull(record.formattedDisplayDate)
        assertNotNull(record.formattedDisplayTime)
        assertNotNull(record.formattedFullDateTime)
        org.junit.Assert.assertTrue(record.formattedDate.contains("-"))
        org.junit.Assert.assertTrue(record.formattedTime.contains(":"))
    }

    @Test
    fun `test daily summary breakdown and auditing time calculation`() {
        val now = System.currentTimeMillis()
        val oneHourAgo = now - (60 * 60 * 1000)
        val twoHoursAgo = now - (2 * 60 * 60 * 1000)

        val records = listOf(
            AuditRecord(
                id = 1L,
                formType = AuditFormType.WITH_COOLER,
                timestamp = twoHoursAgo,
                outletName = "Outlet A",
                latitude = 5.6037,
                longitude = -0.1870
            ),
            AuditRecord(
                id = 2L,
                formType = AuditFormType.WITH_COOLER,
                timestamp = oneHourAgo,
                outletName = "Outlet B",
                latitude = 5.6050,
                longitude = -0.1890
            ),
            AuditRecord(
                id = 3L,
                formType = AuditFormType.WITHOUT_COOLER,
                timestamp = now,
                outletName = "Outlet C",
                latitude = 5.6080,
                longitude = -0.1910
            )
        )

        val total = records.size
        val withCooler = records.count { it.formType == AuditFormType.WITH_COOLER }
        val withoutCooler = records.count { it.formType == AuditFormType.WITHOUT_COOLER }
        val withCoolerPct = ((withCooler.toFloat() / total) * 100).toInt()
        val withoutCoolerPct = 100 - withCoolerPct

        assertEquals(3, total)
        assertEquals(2, withCooler)
        assertEquals(1, withoutCooler)
        assertEquals(66, withCoolerPct)
        assertEquals(34, withoutCoolerPct)

        // Elapsed duration between earliest and latest
        val sorted = records.sortedBy { it.timestamp }
        val elapsedMillis = sorted.last().timestamp - sorted.first().timestamp
        val elapsedMinutes = elapsedMillis / (1000 * 60)
        assertEquals(120L, elapsedMinutes)
    }

    @Test
    fun `test map view HTML generation and leaflet markers`() {
        val records = listOf(
            AuditRecord(
                id = 101L,
                formType = AuditFormType.WITH_COOLER,
                outletName = "Super Mart Accra",
                location = "Osu",
                coolerSerialNumber = "SN-COOL-99",
                coolerTagNumber = "TAG-888",
                latitude = 5.5560,
                longitude = -0.1969
            ),
            AuditRecord(
                id = 102L,
                formType = AuditFormType.WITHOUT_COOLER,
                outletName = "Corner Store",
                location = "Cantonments",
                coolerSerialNumber = "SN-NONE",
                coolerTagNumber = "TAG-NONE",
                latitude = 5.5800,
                longitude = -0.1700
            )
        )

        val html = com.example.ui.screens.generateLeafletMapHtml(records, selectedId = 101L)
        assertNotNull(html)
        org.junit.Assert.assertTrue(html.contains("Super Mart Accra"))
        org.junit.Assert.assertTrue(html.contains("Corner Store"))
        org.junit.Assert.assertTrue(html.contains("5.556"))
        org.junit.Assert.assertTrue(html.contains("pin-cooler"))
        org.junit.Assert.assertTrue(html.contains("pin-nocooler"))
        org.junit.Assert.assertTrue(html.contains("Open in Google Maps"))
    }
}

