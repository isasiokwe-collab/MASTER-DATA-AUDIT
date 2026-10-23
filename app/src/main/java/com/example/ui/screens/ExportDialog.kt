package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.viewmodel.DateFilterType
import com.example.util.ExcelExporter
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ExportDialog(
    viewModel: AuditViewModel,
    totalRecordsCount: Int,
    todayRecordsCount: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedRange by remember { mutableStateOf(DateFilterType.ALL) }
    var isExporting by remember { mutableStateOf(false) }
    var exportedFile by remember { mutableStateOf<File?>(null) }

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
                        imageVector = Icons.Default.TableChart,
                        contentDescription = null,
                        tint = Navy800,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Export to Excel (.xlsx)",
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
                Text(
                    text = "Generates a multi-sheet spreadsheet with separate sheets for With Cooler and Without Cooler, all fields, timestamps, and clickable Google Maps links.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF475569)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Select Records Range:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(8.dp))

                val options = listOf(
                    DateFilterType.ALL to "All Records ($totalRecordsCount total)",
                    DateFilterType.TODAY to "Today's Outlets Only ($todayRecordsCount total)",
                    DateFilterType.LAST_7_DAYS to "Last 7 Days"
                )

                options.forEach { (type, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedRange == type,
                                onClick = { selectedRange = type }
                            )
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedRange == type,
                            onClick = { selectedRange = type },
                            colors = RadioButtonDefaults.colors(selectedColor = Navy800)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selectedRange == type) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                if (exportedFile != null) {
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
                                    text = "Export Created Successfully!",
                                    fontWeight = FontWeight.Bold,
                                    color = Green700
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "File saved to: ${exportedFile!!.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF14532D),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (exportedFile != null) {
                Button(
                    onClick = {
                        ExcelExporter.shareExportedFile(context, exportedFile!!)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                    modifier = Modifier.testTag("share_excel_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share File Now", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            isExporting = true
                            val records = viewModel.getRecordsForExport(selectedRange)
                            val file = ExcelExporter.exportRecordsToExcel(context, records)
                            isExporting = false
                            if (file != null) {
                                exportedFile = file
                                Toast.makeText(context, "Saved to Downloads: ${file.name}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isExporting && totalRecordsCount > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                    modifier = Modifier.testTag("generate_excel_button")
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exporting...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate .xlsx", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_export_dialog")
            ) {
                Text(if (exportedFile != null) "Done" else "Cancel", fontWeight = FontWeight.Bold)
            }
        }
    )
}
