package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AuditFormType
import com.example.data.model.AuditRecord
import com.example.ui.theme.Amber700
import com.example.ui.theme.Green700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Red700
import com.example.ui.viewmodel.AuditViewModel
import com.example.ui.viewmodel.DateFilterType
import com.example.util.LocationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedRecordsScreen(
    viewModel: AuditViewModel,
    onNavigateBack: () -> Unit,
    onEditRecord: (AuditRecord) -> Unit
) {
    val context = LocalContext.current
    BackHandler { onNavigateBack() }

    val records by viewModel.filteredRecords.collectAsStateWithLifecycle()
    val allRecords by viewModel.allRecords.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterType by viewModel.filterFormType.collectAsStateWithLifecycle()
    val filterDate by viewModel.filterDate.collectAsStateWithLifecycle()

    var recordToDelete by remember { mutableStateOf<AuditRecord?>(null) }
    var recordToViewDetail by remember { mutableStateOf<AuditRecord?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }

    val dateFormatter = remember {
        SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault())
    }

    if (showExportDialog) {
        ExportDialog(
            viewModel = viewModel,
            totalRecordsCount = allRecords.size,
            todayRecordsCount = records.count { it.timestamp >= System.currentTimeMillis() - 86400000L },
            onDismiss = { showExportDialog = false }
        )
    }

    // Delete Confirmation Dialog
    if (recordToDelete != null) {
        val rec = recordToDelete!!
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = {
                Text(
                    text = "Delete Audit Record?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete the audit record for \"${rec.outletName.ifEmpty { "this outlet" }}\"? This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRecord(rec)
                        recordToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red700),
                    modifier = Modifier.testTag("confirm_delete_record_button")
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { recordToDelete = null },
                    modifier = Modifier.testTag("cancel_delete_record_button")
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Record Detail Modal
    if (recordToViewDetail != null) {
        val detail = recordToViewDetail!!
        AlertDialog(
            onDismissRequest = { recordToViewDetail = null },
            title = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = detail.outletName.ifEmpty { "Outlet Audit" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    if (detail.formType == AuditFormType.WITH_COOLER) Color(0xFFE0EAFC) else Color(0xFFFEF3C7),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (detail.formType == AuditFormType.WITH_COOLER) "WITH COOLER" else "WITHOUT COOLER",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (detail.formType == AuditFormType.WITH_COOLER) Navy800 else Amber700
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "📅 ${detail.formattedDisplayDate}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE0F2FE), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "⏰ ${detail.formattedDisplayTime}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0369A1)
                            )
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider()

                    DetailRow(label = "Audit Date:", value = detail.formattedDisplayDate)
                    DetailRow(label = "Audit Time:", value = detail.formattedDisplayTime)
                    DetailRow(label = "Full Timestamp:", value = detail.formattedFullDateTime, isMonospace = true)

                    HorizontalDivider()

                    if (detail.formType == AuditFormType.WITH_COOLER && detail.accountDistributor.isNotEmpty()) {
                        DetailRow(label = "Account / Distributor:", value = detail.accountDistributor)
                    }

                    DetailRow(label = "Owner Name:", value = detail.ownerName)
                    DetailRow(label = "Owner Phone:", value = detail.ownerPhone)
                    DetailRow(label = "Location:", value = detail.location)

                    HorizontalDivider()

                    if (detail.formType == AuditFormType.WITH_COOLER && detail.coolerMake.isNotEmpty()) {
                        DetailRow(label = "Cooler Make:", value = detail.coolerMake)
                    }

                    DetailRow(label = "Cooler Model:", value = detail.coolerModel)
                    DetailRow(label = "Serial Number:", value = detail.coolerSerialNumber, isMonospace = true)
                    DetailRow(label = "Tag Number:", value = detail.coolerTagNumber, isMonospace = true)

                    if (detail.notes.isNotEmpty()) {
                        DetailRow(label = "Notes:", value = detail.notes)
                    }

                    HorizontalDivider()

                    DetailRow(
                        label = "GPS Coordinates:",
                        value = "${String.format("%.6f", detail.latitude)}, ${String.format("%.6f", detail.longitude)} (±${String.format("%.1f", detail.gpsAccuracyMeters)}m)",
                        isMonospace = true
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            LocationHelper.openInGoogleMaps(
                                context,
                                detail.latitude,
                                detail.longitude,
                                detail.outletName
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open in Google Maps", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val editTarget = recordToViewDetail
                        recordToViewDetail = null
                        if (editTarget != null) {
                            onEditRecord(editTarget)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Record", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToViewDetail = null }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Saved Records",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF0253B3), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${records.size}",
                                color = PureWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("records_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PureWhite
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.testTag("records_export_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = "Export Excel",
                            tint = PureWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Navy900
                )
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // SEARCH & FILTERS HEADER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PureWhite)
                    .padding(14.dp)
            ) {
                // Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search outlet, serial, tag, owner, location...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Navy800)
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = Color(0xFF64748B))
                            }
                        }
                    } else null,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Navy800,
                        unfocusedBorderColor = Color(0xFF94A3B8),
                        focusedContainerColor = PureWhite,
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("records_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filters Row: Form Type & Date
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(18.dp)
                    )

                    // Form Type Filters
                    FilterChip(
                        selected = filterType == null,
                        onClick = { viewModel.filterFormType.value = null },
                        label = { Text("All Types", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Navy800,
                            selectedLabelColor = PureWhite
                        )
                    )

                    FilterChip(
                        selected = filterType == AuditFormType.WITH_COOLER,
                        onClick = { viewModel.filterFormType.value = AuditFormType.WITH_COOLER },
                        label = { Text("With Cooler", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Navy800,
                            selectedLabelColor = PureWhite
                        )
                    )

                    FilterChip(
                        selected = filterType == AuditFormType.WITHOUT_COOLER,
                        onClick = { viewModel.filterFormType.value = AuditFormType.WITHOUT_COOLER },
                        label = { Text("Without Cooler", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Navy800,
                            selectedLabelColor = PureWhite
                        )
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Date Filters
                    FilterChip(
                        selected = filterDate == DateFilterType.ALL,
                        onClick = { viewModel.filterDate.value = DateFilterType.ALL },
                        label = { Text("All Dates") }
                    )

                    FilterChip(
                        selected = filterDate == DateFilterType.TODAY,
                        onClick = { viewModel.filterDate.value = DateFilterType.TODAY },
                        label = { Text("Today") }
                    )

                    FilterChip(
                        selected = filterDate == DateFilterType.LAST_7_DAYS,
                        onClick = { viewModel.filterDate.value = DateFilterType.LAST_7_DAYS },
                        label = { Text("Last 7 Days") }
                    )
                }
            }

            HorizontalDivider()

            // RECORDS LIST
            if (records.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty() || filterType != null || filterDate != DateFilterType.ALL)
                                "No records match your search or filter"
                            else
                                "No audit records saved yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                        Text(
                            text = "Audited outlets will appear here with instant search and Google Maps links.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(records, key = { it.id }) { record ->
                        RecordCard(
                            record = record,
                            dateString = dateFormatter.format(Date(record.timestamp)),
                            onCardClick = { recordToViewDetail = record },
                            onEditClick = { onEditRecord(record) },
                            onDeleteClick = { recordToDelete = record },
                            onOpenMaps = {
                                LocationHelper.openInGoogleMaps(
                                    context,
                                    record.latitude,
                                    record.longitude,
                                    record.outletName
                                )
                            },
                            onCallPhone = {
                                if (record.ownerPhone.isNotEmpty()) {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${record.ownerPhone}"))
                                    context.startActivity(dialIntent)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordCard(
    record: AuditRecord,
    dateString: String,
    onCardClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onOpenMaps: () -> Unit,
    onCallPhone: () -> Unit
) {
    val isWithCooler = record.formType == AuditFormType.WITH_COOLER

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = PureWhite),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Form Type Badge + Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            if (isWithCooler) Color(0xFFE0EAFC) else Color(0xFFFEF3C7),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isWithCooler) "WITH COOLER" else "WITHOUT COOLER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isWithCooler) Navy800 else Amber700
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "📅 ${record.formattedDisplayDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF334155),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE0F2FE), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "⏰ ${record.formattedDisplayTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF0369A1),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Outlet Name
            Text(
                text = record.outletName.ifEmpty { "Unnamed Outlet" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                fontSize = 19.sp
            )

            // Owner & Location
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Owner: ${record.ownerName.ifEmpty { "N/A" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF334155),
                    fontWeight = FontWeight.Medium
                )

                if (record.location.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = record.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Cooler Details Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (isWithCooler && record.coolerMake.isNotEmpty()) {
                        Text(
                            text = "Make & Model: ${record.coolerMake} ${record.coolerModel}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    } else if (record.coolerModel.isNotEmpty()) {
                        Text(
                            text = "Model: ${record.coolerModel}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Serial: ${record.coolerSerialNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Navy800
                        )
                        Text(
                            text = "Tag: ${record.coolerTagNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F766E)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row: Maps, Call, Edit, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // GPS accuracy chip
                Box(
                    modifier = Modifier
                        .background(Color(0xFFDCFCE7), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "GPS ±${String.format("%.1f", record.gpsAccuracyMeters)}m",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Green700
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Google Maps Button
                    IconButton(
                        onClick = onOpenMaps,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Open in Maps",
                            tint = Navy800
                        )
                    }

                    // Call Owner Button (if phone present)
                    if (record.ownerPhone.isNotEmpty()) {
                        IconButton(
                            onClick = onCallPhone,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Call Owner",
                                tint = Green700
                            )
                        }
                    }

                    // Edit Button
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Record",
                            tint = Color(0xFF0253B3)
                        )
                    }

                    // Delete Button
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Record",
                            tint = Red700
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, isMonospace: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value.ifEmpty { "None" },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
        )
    }
}
