package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Indicates whether the audit is for an outlet with or without cooler.
 */
enum class AuditFormType {
    WITH_COOLER,
    WITHOUT_COOLER
}

/**
 * Room Entity representing a completed field sales / cooler audit record with full timestamp and date tracking.
 */
@Entity(tableName = "audit_records")
data class AuditRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val formType: AuditFormType,
    val timestamp: Long = System.currentTimeMillis(),

    // Field Rep / Outlet Details
    val accountDistributor: String = "", // Form 1: Account / Distributor
    val outletName: String = "",        // Form 1: Outlet Name | Form 2: Trading Name
    val ownerName: String = "",
    val ownerPhone: String = "",
    val location: String = "",          // Area / Town

    // GPS Telemetry
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val gpsAccuracyMeters: Float = 0f,

    // Cooler Identification
    val coolerMake: String = "",        // Form 1: Frigoglass, Klimasan, Metalfrio, Other
    val coolerModel: String = "",       // Form 1 & 2
    val coolerSerialNumber: String = "", // Form 1 & 2
    val coolerTagNumber: String = "",   // Form 1 & 2

    // Notes / Comments
    val notes: String = ""
) {
    val formattedDate: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))

    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

    val formattedDisplayDate: String
        get() = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))

    val formattedDisplayTime: String
        get() = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date(timestamp))

    val formattedFullDateTime: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}
