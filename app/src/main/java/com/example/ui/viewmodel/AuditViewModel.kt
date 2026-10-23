package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.AuditFormType
import com.example.data.model.AuditRecord
import com.example.data.repository.AuditRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.content.Context
import com.example.util.BackupData
import com.example.util.BackupHelper
import java.io.File
import java.util.Calendar
import java.util.Locale

data class DailySummaryStats(
    val todayRecords: List<AuditRecord> = emptyList(),
    val totalCount: Int = 0,
    val withCoolerCount: Int = 0,
    val withoutCoolerCount: Int = 0,
    val withCoolerPercentage: Int = 0,
    val withoutCoolerPercentage: Int = 0,
    val firstAuditTime: String = "--",
    val lastAuditTime: String = "--",
    val totalDurationFormatted: String = "--",
    val avgMinutesPerOutlet: String = "--",
    val outletsPerHour: String = "--",
    val dailyTarget: Int = 30,
    val targetReached: Boolean = false,
    val remainingToTarget: Int = 0,
    val gpsVerifiedCount: Int = 0
)

data class FormValidationErrors(
    val accountError: String? = null,
    val outletNameError: String? = null,
    val ownerNameError: String? = null,
    val ownerPhoneError: String? = null,
    val locationError: String? = null,
    val coolerMakeError: String? = null,
    val coolerModelError: String? = null,
    val coolerSerialError: String? = null,
    val coolerTagError: String? = null,
    val gpsError: String? = null
) {
    val hasErrors: Boolean
        get() = accountError != null || outletNameError != null || ownerNameError != null ||
                ownerPhoneError != null || locationError != null || coolerMakeError != null ||
                coolerModelError != null || coolerSerialError != null || coolerTagError != null ||
                gpsError != null
}

enum class DateFilterType {
    ALL,
    TODAY,
    LAST_7_DAYS
}

class AuditViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AuditRepository
    private val prefs = application.getSharedPreferences("cooler_audit_prefs", android.content.Context.MODE_PRIVATE)

    private val _dailyTarget = MutableStateFlow(prefs.getInt("daily_target", 30))
    val dailyTarget: StateFlow<Int> = _dailyTarget.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AuditRepository(database.auditRecordDao())
    }

    fun updateDailyTarget(newTarget: Int) {
        val clamped = newTarget.coerceIn(1, 200)
        _dailyTarget.value = clamped
        prefs.edit().putInt("daily_target", clamped).apply()
    }

    private fun getTodayStartMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    val allRecords: StateFlow<List<AuditRecord>> = repository.allRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayCount: StateFlow<Int> = repository.getTodayCount(getTodayStartMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayWithCoolerCount: StateFlow<Int> = repository.getTodayWithCoolerCount(getTodayStartMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayWithoutCoolerCount: StateFlow<Int> = repository.getTodayWithoutCoolerCount(getTodayStartMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayRecords: StateFlow<List<AuditRecord>> = allRecords.map { list ->
        val todayStart = getTodayStartMillis()
        list.filter { it.timestamp >= todayStart }.sortedByDescending { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailySummaryStats: StateFlow<DailySummaryStats> = combine(
        todayRecords,
        dailyTarget
    ) { records, target ->
        val total = records.size
        val withCooler = records.count { it.formType == AuditFormType.WITH_COOLER }
        val withoutCooler = records.count { it.formType == AuditFormType.WITHOUT_COOLER }
        val withCoolerPct = if (total > 0) ((withCooler.toFloat() / total) * 100).toInt() else 0
        val withoutCoolerPct = if (total > 0) 100 - withCoolerPct else 0
        val gpsCount = records.count { it.latitude != 0.0 && it.longitude != 0.0 }

        val sortedByTime = records.sortedBy { it.timestamp }
        val firstTime = sortedByTime.firstOrNull()?.formattedDisplayTime ?: "--"
        val lastTime = sortedByTime.lastOrNull()?.formattedDisplayTime ?: "--"

        val (durStr, paceStr, rateStr) = if (records.size >= 2) {
            val earliest = sortedByTime.first().timestamp
            val latest = sortedByTime.last().timestamp
            val elapsedMillis = (latest - earliest).coerceAtLeast(0L)
            val elapsedMinutes = elapsedMillis / (1000 * 60)
            val hours = elapsedMinutes / 60
            val mins = elapsedMinutes % 60
            val duration = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

            val avgMins = if (total > 1) (elapsedMinutes.toFloat() / (total - 1)).toInt().coerceAtLeast(1) else elapsedMinutes.toInt()
            val pace = "~${avgMins} min/outlet"

            val hoursFloat = elapsedMillis.toDouble() / (1000.0 * 60 * 60)
            val rate = if (hoursFloat >= 0.03) {
                String.format(Locale.getDefault(), "%.1f /hr", total / hoursFloat)
            } else {
                "--"
            }
            Triple(duration, pace, rate)
        } else if (records.size == 1) {
            Triple("Initial audit", "1st visit logged", "--")
        } else {
            Triple("0m", "--", "--")
        }

        DailySummaryStats(
            todayRecords = sortedByTime,
            totalCount = total,
            withCoolerCount = withCooler,
            withoutCoolerCount = withoutCooler,
            withCoolerPercentage = withCoolerPct,
            withoutCoolerPercentage = withoutCoolerPct,
            firstAuditTime = firstTime,
            lastAuditTime = lastTime,
            totalDurationFormatted = durStr,
            avgMinutesPerOutlet = paceStr,
            outletsPerHour = rateStr,
            dailyTarget = target,
            targetReached = total >= target,
            remainingToTarget = (target - total).coerceAtLeast(0),
            gpsVerifiedCount = gpsCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailySummaryStats())

    val accountSuggestions: StateFlow<List<String>> = repository.accountSuggestions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val locationSuggestions: StateFlow<List<String>> = repository.locationSuggestions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val coolerMakeSuggestions: StateFlow<List<String>> = repository.coolerMakeSuggestions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Form State ---
    var editingRecordId: Long? = null

    private val _formType = MutableStateFlow(AuditFormType.WITH_COOLER)
    val formType: StateFlow<AuditFormType> = _formType.asStateFlow()

    val accountDistributor = MutableStateFlow("")
    val outletName = MutableStateFlow("") // Outlet Name or Trading Name
    val ownerName = MutableStateFlow("")
    val ownerPhone = MutableStateFlow("")
    val location = MutableStateFlow("")

    val coolerMake = MutableStateFlow("Frigoglass")
    val coolerMakeOther = MutableStateFlow("")
    val coolerModel = MutableStateFlow("")
    val coolerSerialNumber = MutableStateFlow("")
    val coolerTagNumber = MutableStateFlow("")
    val notes = MutableStateFlow("")
    val formTimestamp = MutableStateFlow(System.currentTimeMillis())

    val formLatitude = MutableStateFlow(0.0)
    val formLongitude = MutableStateFlow(0.0)
    val formGpsAccuracy = MutableStateFlow(Float.MAX_VALUE)

    private val _validationErrors = MutableStateFlow(FormValidationErrors())
    val validationErrors: StateFlow<FormValidationErrors> = _validationErrors.asStateFlow()

    private val _duplicateWarningList = MutableStateFlow<List<AuditRecord>>(emptyList())
    val duplicateWarningList: StateFlow<List<AuditRecord>> = _duplicateWarningList.asStateFlow()

    // --- Search & Filter in Saved Records ---
    val searchQuery = MutableStateFlow("")
    val filterFormType = MutableStateFlow<AuditFormType?>(null) // null = ALL
    val filterDate = MutableStateFlow(DateFilterType.ALL)

    val filteredRecords: StateFlow<List<AuditRecord>> = combine(
        allRecords,
        searchQuery,
        filterFormType,
        filterDate
    ) { records, query, typeFilter, dateFilter ->
        val trimmedQuery = query.trim().lowercase()
        val now = System.currentTimeMillis()
        val todayStart = getTodayStartMillis()
        val sevenDaysAgo = now - (7L * 24 * 60 * 60 * 1000)

        records.filter { record ->
            // Form Type filter
            val matchesType = typeFilter == null || record.formType == typeFilter

            // Date filter
            val matchesDate = when (dateFilter) {
                DateFilterType.ALL -> true
                DateFilterType.TODAY -> record.timestamp >= todayStart
                DateFilterType.LAST_7_DAYS -> record.timestamp >= sevenDaysAgo
            }

            // Search query filter
            val matchesSearch = trimmedQuery.isEmpty() ||
                    record.outletName.lowercase().contains(trimmedQuery) ||
                    record.coolerSerialNumber.lowercase().contains(trimmedQuery) ||
                    record.coolerTagNumber.lowercase().contains(trimmedQuery) ||
                    record.ownerName.lowercase().contains(trimmedQuery) ||
                    record.location.lowercase().contains(trimmedQuery) ||
                    record.accountDistributor.lowercase().contains(trimmedQuery)

            matchesType && matchesDate && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun initNewForm(type: AuditFormType) {
        editingRecordId = null
        _formType.value = type
        resetFormFields()
    }

    fun loadRecordForEdit(record: AuditRecord) {
        editingRecordId = record.id
        _formType.value = record.formType
        accountDistributor.value = record.accountDistributor
        outletName.value = record.outletName
        ownerName.value = record.ownerName
        ownerPhone.value = record.ownerPhone
        location.value = record.location

        val standardMakes = listOf("Frigoglass", "Klimasan", "Metalfrio")
        if (record.coolerMake in standardMakes) {
            coolerMake.value = record.coolerMake
            coolerMakeOther.value = ""
        } else if (record.coolerMake.isNotEmpty()) {
            coolerMake.value = "Other"
            coolerMakeOther.value = record.coolerMake
        } else {
            coolerMake.value = "Frigoglass"
            coolerMakeOther.value = ""
        }

        coolerModel.value = record.coolerModel
        coolerSerialNumber.value = record.coolerSerialNumber
        coolerTagNumber.value = record.coolerTagNumber
        notes.value = record.notes
        formTimestamp.value = record.timestamp
        formLatitude.value = record.latitude
        formLongitude.value = record.longitude
        formGpsAccuracy.value = record.gpsAccuracyMeters
        _validationErrors.value = FormValidationErrors()
    }

    fun updateGpsCoordinates(lat: Double, lng: Double, accuracy: Float) {
        formLatitude.value = lat
        formLongitude.value = lng
        formGpsAccuracy.value = accuracy
    }

    fun resetFormFields(keepPreviousDistributorAndLocation: Boolean = true) {
        val prevDist = if (keepPreviousDistributorAndLocation) accountDistributor.value else ""
        val prevLoc = if (keepPreviousDistributorAndLocation) location.value else ""
        val prevMake = if (keepPreviousDistributorAndLocation) coolerMake.value else "Frigoglass"

        formTimestamp.value = System.currentTimeMillis()
        accountDistributor.value = prevDist
        outletName.value = ""
        ownerName.value = ""
        ownerPhone.value = ""
        location.value = prevLoc
        coolerMake.value = prevMake
        coolerMakeOther.value = ""
        coolerModel.value = ""
        coolerSerialNumber.value = ""
        coolerTagNumber.value = ""
        notes.value = ""
        _validationErrors.value = FormValidationErrors()
        _duplicateWarningList.value = emptyList()
    }

    fun clearDuplicateWarning() {
        _duplicateWarningList.value = emptyList()
    }

    /**
     * Validate fields, check GPS accuracy <= 25m, and check for duplicate serial or tag
     */
    fun attemptSave(
        onSuccess: (saveAndNew: Boolean) -> Unit
    ) {
        val currentType = _formType.value
        val dist = accountDistributor.value.trim()
        val name = outletName.value.trim()
        val owner = ownerName.value.trim()
        val phone = ownerPhone.value.trim()
        val loc = location.value.trim()
        val make = if (coolerMake.value == "Other") coolerMakeOther.value.trim() else coolerMake.value.trim()
        val model = coolerModel.value.trim()
        val serial = coolerSerialNumber.value.trim()
        val tag = coolerTagNumber.value.trim()
        val accuracy = formGpsAccuracy.value

        var errors = FormValidationErrors()

        if (currentType == AuditFormType.WITH_COOLER && dist.isEmpty()) {
            errors = errors.copy(accountError = "Account / Distributor is required")
        }

        if (name.isEmpty()) {
            errors = errors.copy(
                outletNameError = if (currentType == AuditFormType.WITH_COOLER) "Outlet Name is required" else "Trading Name is required"
            )
        }

        if (owner.isEmpty()) {
            errors = errors.copy(ownerNameError = "Owner Name is required")
        }

        if (phone.isEmpty()) {
            errors = errors.copy(ownerPhoneError = "Owner Phone is required")
        } else if (phone.count { it.isDigit() } < 7) {
            errors = errors.copy(ownerPhoneError = "Enter a valid phone number (min 7 digits)")
        }

        if (loc.isEmpty()) {
            errors = errors.copy(locationError = "Location (area/town) is required")
        }

        if (currentType == AuditFormType.WITH_COOLER && make.isEmpty()) {
            errors = errors.copy(coolerMakeError = "Cooler Make is required")
        }

        if (model.isEmpty()) {
            errors = errors.copy(coolerModelError = "Cooler Model is required")
        }

        if (serial.isEmpty()) {
            errors = errors.copy(coolerSerialError = "Serial Number is required")
        }

        if (tag.isEmpty()) {
            errors = errors.copy(coolerTagError = "Tag Number is required")
        }

        if (formLatitude.value == 0.0 && formLongitude.value == 0.0) {
            errors = errors.copy(gpsError = "GPS lock is required. Please wait for satellite fix.")
        } else if (accuracy > 25.0f) {
            errors = errors.copy(gpsError = "GPS accuracy is ${String.format("%.1f", accuracy)}m. Must be 25m or better to save.")
        }

        _validationErrors.value = errors

        if (errors.hasErrors) {
            return
        }

        // Check for duplicates
        viewModelScope.launch {
            val excludeId = editingRecordId ?: 0L
            val duplicates = repository.findDuplicates(serial, tag, excludeId)
            if (duplicates.isNotEmpty()) {
                _duplicateWarningList.value = duplicates
            } else {
                performSave(saveAndNew = false, onSuccess = onSuccess)
            }
        }
    }

    fun forceSaveAfterDuplicateCheck(saveAndNew: Boolean, onSuccess: (saveAndNew: Boolean) -> Unit) {
        _duplicateWarningList.value = emptyList()
        performSave(saveAndNew = saveAndNew, onSuccess = onSuccess)
    }

    fun performSave(saveAndNew: Boolean, onSuccess: (saveAndNew: Boolean) -> Unit) {
        val make = if (coolerMake.value == "Other") coolerMakeOther.value.trim() else coolerMake.value.trim()
        val recordTimestamp = if (editingRecordId == null || editingRecordId == 0L) System.currentTimeMillis() else formTimestamp.value

        val record = AuditRecord(
            id = editingRecordId ?: 0L,
            formType = _formType.value,
            timestamp = recordTimestamp,
            accountDistributor = if (_formType.value == AuditFormType.WITH_COOLER) accountDistributor.value.trim() else "",
            outletName = outletName.value.trim(),
            ownerName = ownerName.value.trim(),
            ownerPhone = ownerPhone.value.trim(),
            location = location.value.trim(),
            latitude = formLatitude.value,
            longitude = formLongitude.value,
            gpsAccuracyMeters = formGpsAccuracy.value,
            coolerMake = if (_formType.value == AuditFormType.WITH_COOLER) make else "",
            coolerModel = coolerModel.value.trim(),
            coolerSerialNumber = coolerSerialNumber.value.trim(),
            coolerTagNumber = coolerTagNumber.value.trim(),
            notes = notes.value.trim()
        )

        viewModelScope.launch {
            if (editingRecordId == null || editingRecordId == 0L) {
                repository.insertRecord(record)
            } else {
                repository.updateRecord(record)
            }

            if (saveAndNew) {
                resetFormFields(keepPreviousDistributorAndLocation = true)
                editingRecordId = null
            }
            onSuccess(saveAndNew)
        }
    }

    fun deleteRecord(record: AuditRecord) {
        viewModelScope.launch {
            repository.deleteRecord(record)
        }
    }

    suspend fun getRecordsForExport(dateFilterType: DateFilterType): List<AuditRecord> {
        val now = System.currentTimeMillis()
        val todayStart = getTodayStartMillis()
        val sevenDaysAgo = now - (7L * 24 * 60 * 60 * 1000)

        return when (dateFilterType) {
            DateFilterType.ALL -> repository.getAllRecordsSnapshot()
            DateFilterType.TODAY -> repository.getRecordsInRange(todayStart, now + 10000)
            DateFilterType.LAST_7_DAYS -> repository.getRecordsInRange(sevenDaysAgo, now + 10000)
        }
    }

    suspend fun createBackup(context: Context): File? {
        val records = repository.getAllRecordsSnapshot()
        return BackupHelper.createBackupFile(context, records, _dailyTarget.value)
    }

    suspend fun restoreDatabase(backupData: BackupData, cleanReplace: Boolean) {
        repository.restoreRecords(backupData.records, cleanReplace)
        if (backupData.dailyTarget > 0) {
            updateDailyTarget(backupData.dailyTarget)
        }
    }
}
