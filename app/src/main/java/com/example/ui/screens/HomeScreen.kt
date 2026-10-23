package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AuditFormType
import com.example.data.model.AuditRecord
import com.example.ui.theme.Blue100
import com.example.ui.theme.Blue600
import com.example.ui.theme.Green600
import com.example.ui.theme.Green700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.viewmodel.AuditViewModel
import com.example.util.LocationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AuditViewModel,
    onNavigateToWithCoolerForm: () -> Unit,
    onNavigateToWithoutCoolerForm: () -> Unit,
    onNavigateToSavedRecords: () -> Unit,
    onNavigateToMapView: () -> Unit,
    onEditRecord: (AuditRecord) -> Unit
) {
    val context = LocalContext.current
    val allRecords by viewModel.allRecords.collectAsStateWithLifecycle()
    val todayTotal by viewModel.todayCount.collectAsStateWithLifecycle()
    val todayWithCooler by viewModel.todayWithCoolerCount.collectAsStateWithLifecycle()
    val todayWithoutCooler by viewModel.todayWithoutCoolerCount.collectAsStateWithLifecycle()
    val dailyTarget by viewModel.dailyTarget.collectAsStateWithLifecycle()
    val dailySummaryStats by viewModel.dailySummaryStats.collectAsStateWithLifecycle()

    var showExportDialog by remember { mutableStateOf(false) }
    var showTargetDialog by remember { mutableStateOf(false) }
    var showBackupRestoreDialog by remember { mutableStateOf(false) }
    var showDailySummaryBottomSheet by remember { mutableStateOf(false) }

    val todayFormatted = remember {
        SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()).format(Date())
    }

    if (showExportDialog) {
        ExportDialog(
            viewModel = viewModel,
            totalRecordsCount = allRecords.size,
            todayRecordsCount = todayTotal,
            onDismiss = { showExportDialog = false }
        )
    }

    if (showTargetDialog) {
        TargetSettingsDialog(
            currentTarget = dailyTarget,
            onSaveTarget = { newTarget ->
                viewModel.updateDailyTarget(newTarget)
            },
            onDismiss = { showTargetDialog = false }
        )
    }

    if (showBackupRestoreDialog) {
        BackupRestoreDialog(
            viewModel = viewModel,
            totalRecordsCount = allRecords.size,
            dailyTarget = dailyTarget,
            onDismiss = { showBackupRestoreDialog = false }
        )
    }

    if (showDailySummaryBottomSheet) {
        DailySummaryBottomSheet(
            stats = dailySummaryStats,
            onDismiss = { showDailySummaryBottomSheet = false },
            onNavigateToMapView = {
                showDailySummaryBottomSheet = false
                onNavigateToMapView()
            },
            onExportToday = {
                showDailySummaryBottomSheet = false
                showExportDialog = true
            },
            onSelectRecord = { record ->
                showDailySummaryBottomSheet = false
                onEditRecord(record)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Field Cooler Audit",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                        Text(
                            text = todayFormatted,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToMapView,
                        modifier = Modifier.testTag("top_bar_map_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Coverage Map",
                            tint = PureWhite
                        )
                    }
                    IconButton(
                        onClick = { showDailySummaryBottomSheet = true },
                        modifier = Modifier.testTag("top_bar_summary_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Insights,
                            contentDescription = "Daily Summary",
                            tint = PureWhite
                        )
                    }
                    IconButton(
                        onClick = { showBackupRestoreDialog = true },
                        modifier = Modifier.testTag("top_bar_backup_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = "Backup & Restore",
                            tint = PureWhite
                        )
                    }
                    IconButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.testTag("top_bar_export_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = "Export to Excel",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. TODAY'S METRIC BANNER
            // 1. TODAY'S METRIC BANNER WITH PROGRESS RING
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, Color(0xFF0253B3), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = Navy800
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "TODAY'S OUTLET PROGRESS",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF93C5FD),
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )

                                    // Clickable target badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF0F2B48))
                                            .clickable { showTargetDialog = true }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                            .testTag("edit_daily_target_button")
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Goal",
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Goal: $dailyTarget",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF93C5FD),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (todayTotal >= dailyTarget) "Daily Target Reached! 🎉" else "${(dailyTarget - todayTotal).coerceAtLeast(0)} more to reach $dailyTarget goal",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PureWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(Color(0xFF38BDF8), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$todayWithCooler With Cooler",
                                        color = Color(0xFFE2E8F0),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(Color(0xFFFBBF24), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$todayWithoutCooler Without Cooler",
                                        color = Color(0xFFE2E8F0),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Circular Progress Ring showing outlets count vs custom target
                            AuditProgressRing(
                                currentCount = todayTotal,
                                targetCount = dailyTarget
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFF1E3A5F))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F2B48))
                                .clickable { showDailySummaryBottomSheet = true }
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                                .testTag("banner_daily_summary_button"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Insights,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Daily Summary & Breakdown",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PureWhite
                                    )
                                    Text(
                                        text = "Time: ${dailySummaryStats.totalDurationFormatted} • Pace: ${if (dailySummaryStats.avgMinutesPerOutlet != "--") dailySummaryStats.avgMinutesPerOutlet else "Active field"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = Color(0xFF93C5FD)
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "View",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. TWO BIG AUDIT BUTTONS (PRIMARY USER ACTIONS)
            item {
                Text(
                    text = "START NEW OUTLET AUDIT",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    letterSpacing = 0.5.sp
                )
            }

            // Big Button 1: Outlet Audit With Cooler
            item {
                Button(
                    onClick = onNavigateToWithCoolerForm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(76.dp)
                        .testTag("audit_with_cooler_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Navy800,
                        contentColor = PureWhite
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(Color(0xFF0253B3), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Kitchen,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Outlet Audit With Cooler",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = "Capture distributor, cooler make/model/serial",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8)
                        )
                    }
                }
            }

            // Big Button 2: Outlet Audit Without Cooler
            item {
                Button(
                    onClick = onNavigateToWithoutCoolerForm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(76.dp)
                        .testTag("audit_without_cooler_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Slate800,
                        contentColor = PureWhite
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(Color(0xFF334155), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Outlet Audit Without Cooler",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = "Trace unserviced/missing coolers & trading name",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24)
                        )
                    }
                }
            }

            // 3. SECONDARY CONTROLS: Saved Records & Export to Excel
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Saved Records Button
                    OutlinedButton(
                        onClick = onNavigateToSavedRecords,
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .testTag("saved_records_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = PureWhite,
                            contentColor = Navy800
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            width = 2.dp,
                            brush = androidx.compose.ui.graphics.SolidColor(Navy800)
                        )
                    ) {
                        Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Saved Records", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("${allRecords.size} total", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                        }
                    }

                    // Export to Excel Button
                    Button(
                        onClick = { showExportDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .testTag("export_to_excel_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Green700,
                            contentColor = PureWhite
                        )
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Export to Excel", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(".xlsx Sheets", style = MaterialTheme.typography.bodySmall, color = Color(0xFFDCFCE7))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Field Coverage Map Button
                Button(
                    onClick = onNavigateToMapView,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("home_coverage_map_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0253B3),
                        contentColor = PureWhite
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF1E3A8A), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Field Coverage Map",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "${dailySummaryStats.gpsVerifiedCount} outlets plotted today • Interactive map",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF93C5FD),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Backup & Restore Database Button
                OutlinedButton(
                    onClick = { showBackupRestoreDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("backup_restore_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = PureWhite,
                        contentColor = Navy800
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 1.5.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF64748B))
                    )
                ) {
                    Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(20.dp), tint = Navy800)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Backup & Restore Database (.json)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            // 4. TODAY'S RECENT OUTLETS
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT CAPTURES TODAY",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🗺️ Map",
                            color = Color(0xFF0253B3),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clickable { onNavigateToMapView() }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .testTag("recent_captures_map_link")
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "📊 Summary",
                            color = Navy800,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clickable { showDailySummaryBottomSheet = true }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .testTag("recent_captures_summary_link")
                        )
                        if (allRecords.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "View All",
                                color = Blue600,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .clickable { onNavigateToSavedRecords() }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (allRecords.isEmpty()) {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = PureWhite)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No outlets captured yet today",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                            Text(
                                text = "Tap either button above to begin your first audit.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            } else {
                items(allRecords.take(4)) { record ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditRecord(record) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = PureWhite)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val isCooler = record.formType == AuditFormType.WITH_COOLER
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isCooler) Color(0xFFE0EAFC) else Color(0xFFFEF3C7),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isCooler) "WITH COOLER" else "WITHOUT COOLER",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCooler) Navy800 else Color(0xFFB45309)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = record.location.ifEmpty { "Location set" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = record.outletName.ifEmpty { "Unnamed Outlet" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Serial: ${record.coolerSerialNumber} • Tag: ${record.coolerTagNumber}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF475569)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "📅 ${record.formattedDisplayDate}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = Color(0xFF475569),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "⏰ ${record.formattedDisplayTime}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = Color(0xFF0369A1),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    LocationHelper.openInGoogleMaps(
                                        context,
                                        record.latitude,
                                        record.longitude,
                                        record.outletName
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = "View Map",
                                    tint = Navy800
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Animated high-visibility circular progress ring tracking captured outlets against daily goal
 */
@Composable
fun AuditProgressRing(
    currentCount: Int,
    targetCount: Int = 30,
    modifier: Modifier = Modifier
) {
    val progress = (currentCount.toFloat() / targetCount.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800),
        label = "progress_ring_anim"
    )

    val ringColor = when {
        currentCount >= targetCount -> Color(0xFF4ADE80) // Emerald celebratory
        currentCount >= 15 -> Color(0xFF38BDF8)          // High-vis sky blue
        else -> Color(0xFF60A5FA)                        // Electric blue
    }

    val percentage = ((currentCount.toFloat() / targetCount.toFloat()) * 100).toInt()

    Box(
        modifier = modifier
            .size(108.dp)
            .testTag("daily_progress_ring"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = 10.dp.toPx()
            val diameter = size.minDimension - strokeWidthPx
            val topLeftOffset = Offset(strokeWidthPx / 2, strokeWidthPx / 2)
            val arcSize = Size(diameter, diameter)

            // Background circle track
            drawArc(
                color = Color(0xFF1E3A5F),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeftOffset,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )

            // Foreground progress arc
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeftOffset,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$currentCount",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PureWhite
            )
            Text(
                text = "/$targetCount ($percentage%)",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF93C5FD)
            )
        }
    }
}

/**
 * Dialog allowing the user to select or enter a custom daily outlet target
 */
@Composable
fun TargetSettingsDialog(
    currentTarget: Int,
    onSaveTarget: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var targetText by remember { mutableStateOf(currentTarget.toString()) }
    val presets = listOf(15, 20, 25, 30, 35, 40, 50)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = Navy800,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Daily Outlet Target",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Set your personal target number of outlets to audit each day. The progress ring and daily indicators will update automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF475569)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Quick Presets:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        val isSelected = targetText == preset.toString()
                        FilterChip(
                            selected = isSelected,
                            onClick = { targetText = preset.toString() },
                            label = { Text("$preset outlets", fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Navy800,
                                selectedLabelColor = PureWhite
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = targetText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 3) {
                            targetText = input
                        }
                    },
                    label = { Text("Custom Target Number") },
                    placeholder = { Text("e.g. 30") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_target_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = targetText.toIntOrNull() ?: currentTarget
                    onSaveTarget(parsed.coerceIn(1, 200))
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                modifier = Modifier.testTag("save_target_button")
            ) {
                Text("Save Target", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_target_button")
            ) {
                Text("Cancel", fontWeight = FontWeight.Bold)
            }
        }
    )
}


