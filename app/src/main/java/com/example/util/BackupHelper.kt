package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.AuditFormType
import com.example.data.model.AuditRecord
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupData(
    val exportedAt: Long,
    val dailyTarget: Int,
    val records: List<AuditRecord>
)

object BackupHelper {

    private val fileTimestampFormatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun createBackupFile(
        context: Context,
        records: List<AuditRecord>,
        dailyTarget: Int
    ): File? {
        val timestamp = fileTimestampFormatter.format(Date())
        val fileName = "CoolerAudit_Backup_$timestamp.json"

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val targetDir = if (downloadsDir != null && (downloadsDir.exists() || downloadsDir.mkdirs())) {
            downloadsDir
        } else {
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        }

        val outputFile = File(targetDir, fileName)

        try {
            val rootJson = JSONObject().apply {
                put("app", "Field Sales & Cooler Audit Tracker")
                put("version", 1)
                put("exportedAt", System.currentTimeMillis())
                put("dailyTarget", dailyTarget)
                put("recordsCount", records.size)

                val recordsArray = JSONArray()
                records.forEach { record ->
                    val recObj = JSONObject().apply {
                        put("id", record.id)
                        put("formType", record.formType.name)
                        put("timestamp", record.timestamp)
                        put("accountDistributor", record.accountDistributor)
                        put("outletName", record.outletName)
                        put("ownerName", record.ownerName)
                        put("ownerPhone", record.ownerPhone)
                        put("location", record.location)
                        put("latitude", record.latitude)
                        put("longitude", record.longitude)
                        put("gpsAccuracyMeters", record.gpsAccuracyMeters.toDouble())
                        put("coolerMake", record.coolerMake)
                        put("coolerModel", record.coolerModel)
                        put("coolerSerialNumber", record.coolerSerialNumber)
                        put("coolerTagNumber", record.coolerTagNumber)
                        put("notes", record.notes)
                    }
                    recordsArray.put(recObj)
                }
                put("records", recordsArray)
            }

            FileOutputStream(outputFile).use { fos ->
                fos.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
            }

            return outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
            return null
        }
    }

    fun shareBackupFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Cooler Audit Database Backup (${file.name})")
                putExtra(Intent.EXTRA_TEXT, "Here is the database backup file for Cooler Audit Tracker: ${file.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Save or Share Database Backup...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share backup: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun readBackupFromUri(context: Context, uri: Uri): BackupData? {
        return try {
            val stringBuilder = java.lang.StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line)
                        line = reader.readLine()
                    }
                }
            }

            val jsonString = stringBuilder.toString()
            if (jsonString.isBlank()) return null

            val rootJson = JSONObject(jsonString)
            val exportedAt = rootJson.optLong("exportedAt", System.currentTimeMillis())
            val dailyTarget = rootJson.optInt("dailyTarget", 30)

            val recordsArray = rootJson.optJSONArray("records") ?: JSONArray()
            val recordsList = mutableListOf<AuditRecord>()

            for (i in 0 until recordsArray.length()) {
                val obj = recordsArray.getJSONObject(i)
                val typeStr = obj.optString("formType", "WITH_COOLER")
                val formType = try {
                    AuditFormType.valueOf(typeStr)
                } catch (_: Exception) {
                    AuditFormType.WITH_COOLER
                }

                val record = AuditRecord(
                    id = 0L, // Reset ID so Room assigns new auto-generated IDs on insert
                    formType = formType,
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    accountDistributor = obj.optString("accountDistributor", ""),
                    outletName = obj.optString("outletName", ""),
                    ownerName = obj.optString("ownerName", ""),
                    ownerPhone = obj.optString("ownerPhone", ""),
                    location = obj.optString("location", ""),
                    latitude = obj.optDouble("latitude", 0.0),
                    longitude = obj.optDouble("longitude", 0.0),
                    gpsAccuracyMeters = obj.optDouble("gpsAccuracyMeters", 0.0).toFloat(),
                    coolerMake = obj.optString("coolerMake", ""),
                    coolerModel = obj.optString("coolerModel", ""),
                    coolerSerialNumber = obj.optString("coolerSerialNumber", ""),
                    coolerTagNumber = obj.optString("coolerTagNumber", ""),
                    notes = obj.optString("notes", "")
                )
                recordsList.add(record)
            }

            BackupData(
                exportedAt = exportedAt,
                dailyTarget = dailyTarget,
                records = recordsList
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
