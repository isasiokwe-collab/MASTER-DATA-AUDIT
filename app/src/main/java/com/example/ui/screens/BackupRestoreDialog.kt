package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Green700
import com.example.ui.theme.Navy800
import com.example.ui.viewmodel.AuditViewModel
import com.example.util.BackupData
import com.example.util.BackupHelper
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupRestoreDialog(
    viewModel: AuditViewModel,
    totalRecordsCount: Int,
    dailyTarget: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Backup, 1 = Restore

    // Backup tab state
    var isBackingUp by remember { mutableStateOf(false) }
    var generatedBackupFile by remember { mutableStateOf<File?>(null) }

    // Restore tab state
    var isRestoring by remember { mutableStateOf(false) }
    var loadedBackupData by remember { mutableStateOf<BackupData?>(null) }
    var cleanReplace by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val data = BackupHelper.readBackupFromUri(context, uri)
            if (data != null) {
                loadedBackupData = data
                Toast.makeText(context, "Loaded ${data.records.size} records from backup", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Invalid or unreadable backup file", Toast.LENGTH_LONG).show()
            }
        }
    }

    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = null,
                        tint = Navy800,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Database Backup & Restore",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFFF1F5F9),
                    contentColor = Navy800,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Navy800
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Backup", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Restore", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // TAB 0: CREATE BACKUP
                    Text(
                        text = "Exports all saved outlet audits, GPS coordinates, cooler serials, and preferences into a portable file in your Downloads folder.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF475569)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Current Database Snapshot:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Total Outlets: $totalRecordsCount",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "• Daily Goal: $dailyTarget outlets/day",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (generatedBackupFile != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFDCFCE7), RoundedCornerShape(10.dp))
                                .border(1.5.dp, Green700, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Green700)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Backup File Created!",
                                        fontWeight = FontWeight.Bold,
                                        color = Green700
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = generatedBackupFile!!.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF14532D),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                } else {
                    // TAB 1: RESTORE BACKUP
                    Text(
                        text = "Import a previous backup file to restore records on this device or when switching phones.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF475569)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            filePickerLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("pick_backup_file_button")
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (loadedBackupData == null) "Select Backup File (.json)" else "Change Backup File",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (loadedBackupData != null) {
                        val data = loadedBackupData!!
                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE0EAFC), RoundedCornerShape(10.dp))
                                .border(1.5.dp, Navy800, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Backup File Verified",
                                    fontWeight = FontWeight.Bold,
                                    color = Navy800
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• Contains: ${data.records.size} outlet records",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "• Exported: ${dateFormatter.format(Date(data.exportedAt))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF334155)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Choose Restore Mode:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = !cleanReplace,
                                    onClick = { cleanReplace = false }
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = !cleanReplace,
                                onClick = { cleanReplace = false },
                                colors = RadioButtonDefaults.colors(selectedColor = Navy800)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("Merge with existing data", fontWeight = FontWeight.Bold)
                                Text("Keeps existing records and adds backup records", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = cleanReplace,
                                    onClick = { cleanReplace = true }
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = cleanReplace,
                                onClick = { cleanReplace = true },
                                colors = RadioButtonDefaults.colors(selectedColor = Navy800)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("Clean replace (Overwrite)", fontWeight = FontWeight.Bold)
                                Text("Clears current records and installs backup snapshot", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedTab == 0) {
                // Backup tab confirm
                if (generatedBackupFile != null) {
                    Button(
                        onClick = {
                            BackupHelper.shareBackupFile(context, generatedBackupFile!!)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Green700),
                        modifier = Modifier.testTag("share_backup_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Backup", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            scope.launch {
                                isBackingUp = true
                                val file = viewModel.createBackup(context)
                                isBackingUp = false
                                if (file != null) {
                                    generatedBackupFile = file
                                    Toast.makeText(context, "Backup saved to Downloads!", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isBackingUp,
                        colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                        modifier = Modifier.testTag("create_backup_button")
                    ) {
                        if (isBackingUp) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Backing up...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Backup", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Restore tab confirm
                Button(
                    onClick = {
                        if (loadedBackupData != null) {
                            scope.launch {
                                isRestoring = true
                                viewModel.restoreDatabase(loadedBackupData!!, cleanReplace = cleanReplace)
                                isRestoring = false
                                Toast.makeText(context, "Restored ${loadedBackupData!!.records.size} records successfully!", Toast.LENGTH_LONG).show()
                                onDismiss()
                            }
                        }
                    },
                    enabled = loadedBackupData != null && !isRestoring,
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                    modifier = Modifier.testTag("confirm_restore_button")
                ) {
                    if (isRestoring) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restoring...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore Database", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_backup_restore_dialog")
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}
