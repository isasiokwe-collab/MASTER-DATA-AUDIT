package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuditRecord
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber700
import com.example.ui.theme.Green100
import com.example.ui.theme.Green700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Red100
import com.example.ui.theme.Red700
import com.example.util.GpsStatus

/**
 * High-visibility GPS Status Card with live accuracy verification (required <= 25m)
 */
@Composable
fun GpsAccuracyCard(
    gpsStatus: GpsStatus,
    onRefreshLocation: () -> Unit,
    onOpenInMaps: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isReady = gpsStatus.isAccurateEnough
    val hasGps = gpsStatus.hasLocation

    val containerBg = when {
        isReady -> Green100
        hasGps -> Amber100
        else -> Red100
    }

    val borderColor = when {
        isReady -> Green700
        hasGps -> Amber700
        else -> Red700
    }

    val statusTitle = when {
        isReady -> "GPS LOCKED (${String.format("%.1f", gpsStatus.accuracy)} m) - READY"
        hasGps -> "GPS WEAK (${String.format("%.1f", gpsStatus.accuracy)} m) - TARGET <= 25 m"
        gpsStatus.isFetching -> "FETCHING HIGH ACCURACY GPS..."
        else -> "NO GPS LOCK YET"
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .background(containerBg)
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isReady) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "GPS Ready",
                            tint = Green700,
                            modifier = Modifier.size(24.dp)
                        )
                    } else if (gpsStatus.isFetching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = borderColor,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "GPS Alert",
                            tint = borderColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = borderColor,
                        fontSize = 15.sp
                    )
                }

                IconButton(
                    onClick = onRefreshLocation,
                    modifier = Modifier.testTag("refresh_gps_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh GPS",
                        tint = borderColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (hasGps) {
                Text(
                    text = "Coordinates: ${String.format("%.6f", gpsStatus.latitude)}, ${String.format("%.6f", gpsStatus.longitude)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    fontSize = 14.sp
                )
            } else {
                Text(
                    text = gpsStatus.error ?: "Waiting for satellite signal. Ensure you are outdoors with clear sky view.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF334155),
                    fontSize = 13.sp
                )
            }

            if (!isReady && hasGps) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Save is disabled until GPS accuracy is 25 meters or better.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Red700
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onOpenInMaps,
                    enabled = hasGps,
                    modifier = Modifier.testTag("open_maps_button"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Navy800
                    )
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open in Google Maps", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Accessible Outlined TextField with clear button and outdoor high contrast styling
 */
@Composable
fun AuditInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    singleLine: Boolean = true,
    testTag: String = ""
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = {
                Text(
                    text = label,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            },
            placeholder = {
                if (placeholder.isNotEmpty()) {
                    Text(placeholder, color = Color(0xFF94A3B8))
                }
            },
            leadingIcon = leadingIcon?.let {
                { Icon(imageVector = it, contentDescription = null, tint = Navy800) }
            },
            trailingIcon = trailingIcon ?: if (value.isNotEmpty()) {
                {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF64748B))
                    }
                }
            } else null,
            isError = isError,
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF0F172A)
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Navy800,
                unfocusedBorderColor = Color(0xFF475569),
                focusedLabelColor = Navy800,
                unfocusedLabelColor = Color(0xFF334155),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color(0xFFFDFDFD)
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = Red700,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }
    }
}

/**
 * Text field paired with an instant "Scan" button for Serial Numbers and Tags
 */
@Composable
fun ScannableField(
    value: String,
    onValueChange: (String) -> Unit,
    onScanClick: () -> Unit,
    label: String,
    placeholder: String = "",
    isError: Boolean = false,
    errorMessage: String? = null,
    testTagPrefix: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AuditInputField(
                value = value,
                onValueChange = onValueChange,
                label = label,
                placeholder = placeholder,
                isError = isError,
                leadingIcon = Icons.Default.QrCodeScanner,
                testTag = "${testTagPrefix}_input",
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onScanClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0253B3),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .height(56.dp)
                    .testTag("${testTagPrefix}_scan_button")
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Scan",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scan", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = Red700,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }
    }
}

/**
 * Horizontal row of quick-pick suggestion chips to minimize typing in the field
 */
@Composable
fun QuickPickChips(
    title: String,
    suggestions: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (suggestions.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF475569),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.take(8).forEach { item ->
                SuggestionChip(
                    onClick = { onSelect(item) },
                    label = { Text(item, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = Color(0xFFE2E8F0),
                        labelColor = Navy800
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = Color(0xFF94A3B8)
                    )
                )
            }
        }
    }
}

/**
 * Selectable Cooler Make chips (Frigoglass, Klimasan, Metalfrio, Other)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoolerMakeSelector(
    selectedMake: String,
    onMakeSelected: (String) -> Unit,
    customOtherValue: String,
    onCustomOtherChanged: (String) -> Unit,
    previousSuggestions: List<String> = emptyList()
) {
    val standardMakes = listOf("Frigoglass", "Klimasan", "Metalfrio", "Other")

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Cooler Make *",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            standardMakes.forEach { make ->
                val isSelected = selectedMake == make || (make == "Other" && selectedMake !in standardMakes.take(3) && selectedMake.isNotEmpty())
                FilterChip(
                    selected = isSelected,
                    onClick = { onMakeSelected(make) },
                    label = { Text(make, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Navy800,
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFFF1F5F9),
                        labelColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.testTag("cooler_make_${make.lowercase()}")
                )
            }
        }

        if (selectedMake == "Other" || (selectedMake !in standardMakes.take(3) && selectedMake.isNotEmpty())) {
            Spacer(modifier = Modifier.height(6.dp))
            AuditInputField(
                value = customOtherValue,
                onValueChange = onCustomOtherChanged,
                label = "Specify Cooler Make *",
                placeholder = "e.g. True, Imbera, Haier",
                testTag = "cooler_make_other_input"
            )
        }

        if (previousSuggestions.isNotEmpty()) {
            QuickPickChips(
                title = "Recent makes:",
                suggestions = previousSuggestions,
                onSelect = onMakeSelected
            )
        }
    }
}

/**
 * Duplicate Serial / Tag Number Warning Dialog
 */
@Composable
fun DuplicateWarningDialog(
    duplicates: List<AuditRecord>,
    onDismiss: () -> Unit,
    onConfirmSaveAnyway: () -> Unit
) {
    val firstDup = duplicates.firstOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Amber700)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Duplicate Cooler Warning", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    text = "A record with this Serial Number or Tag Number already exists in your saved database:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (firstDup != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "Outlet: ${firstDup.outletName.ifEmpty { "Unnamed" }}",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF78350F)
                            )
                            Text(
                                text = "Serial: ${firstDup.coolerSerialNumber} | Tag: ${firstDup.coolerTagNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF78350F)
                            )
                            Text(
                                text = "Location: ${firstDup.location}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF78350F)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Are you sure you want to save this record anyway, or would you like to review and correct the numbers?",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmSaveAnyway,
                colors = ButtonDefaults.buttonColors(containerColor = Amber700),
                modifier = Modifier.testTag("confirm_duplicate_save")
            ) {
                Text("Save Anyway", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_duplicate_dialog")
            ) {
                Text("Review & Correct", fontWeight = FontWeight.Bold)
            }
        }
    )
}
