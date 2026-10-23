package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AuditFormType
import com.example.ui.components.AuditInputField
import com.example.ui.components.CameraScannerDialog
import com.example.ui.components.CoolerMakeSelector
import com.example.ui.components.DuplicateWarningDialog
import com.example.ui.components.GpsAccuracyCard
import com.example.ui.components.QuickPickChips
import com.example.ui.components.ScannableField
import com.example.ui.theme.Green700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.viewmodel.AuditViewModel
import com.example.util.LocationHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ActiveScanTarget {
    SERIAL_NUMBER,
    TAG_NUMBER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditFormScreen(
    viewModel: AuditViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler {
        onNavigateBack()
    }

    val formType by viewModel.formType.collectAsStateWithLifecycle()
    val isWithCooler = formType == AuditFormType.WITH_COOLER

    // Form inputs
    val accountDistributor by viewModel.accountDistributor.collectAsStateWithLifecycle()
    val outletName by viewModel.outletName.collectAsStateWithLifecycle()
    val ownerName by viewModel.ownerName.collectAsStateWithLifecycle()
    val ownerPhone by viewModel.ownerPhone.collectAsStateWithLifecycle()
    val location by viewModel.location.collectAsStateWithLifecycle()
    val coolerMake by viewModel.coolerMake.collectAsStateWithLifecycle()
    val coolerMakeOther by viewModel.coolerMakeOther.collectAsStateWithLifecycle()
    val coolerModel by viewModel.coolerModel.collectAsStateWithLifecycle()
    val coolerSerialNumber by viewModel.coolerSerialNumber.collectAsStateWithLifecycle()
    val coolerTagNumber by viewModel.coolerTagNumber.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()

    val validationErrors by viewModel.validationErrors.collectAsStateWithLifecycle()
    val duplicateWarnings by viewModel.duplicateWarningList.collectAsStateWithLifecycle()

    val formTimestamp by viewModel.formTimestamp.collectAsStateWithLifecycle()
    val entryDateFormatted = remember(formTimestamp) {
        SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()).format(Date(formTimestamp))
    }
    val entryTimeFormatted = remember(formTimestamp) {
        SimpleDateFormat("hh:mm:ss a (z)", Locale.getDefault()).format(Date(formTimestamp))
    }

    // Suggestions
    val accountSuggestions by viewModel.accountSuggestions.collectAsStateWithLifecycle()
    val locationSuggestions by viewModel.locationSuggestions.collectAsStateWithLifecycle()
    val makeSuggestions by viewModel.coolerMakeSuggestions.collectAsStateWithLifecycle()

    // Location tracker
    val locationHelper = remember { LocationHelper(context) }
    val gpsStatus by locationHelper.gpsStatus.collectAsStateWithLifecycle()

    // Active camera scanner target
    var activeScannerTarget by remember { mutableStateOf<ActiveScanTarget?>(null) }
    var pendingSaveAndNew by remember { mutableStateOf(false) }

    // Permissions launcher
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true

        if (fineLocationGranted) {
            locationHelper.startLocationUpdates { lat, lng, acc ->
                viewModel.updateGpsCoordinates(lat, lng, acc)
            }
        }
    }

    LaunchedEffect(Unit) {
        val hasLocationPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCameraPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasLocationPerm) {
            locationHelper.startLocationUpdates { lat, lng, acc ->
                viewModel.updateGpsCoordinates(lat, lng, acc)
            }
        } else {
            permissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.CAMERA
                )
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            locationHelper.stopLocationUpdates()
        }
    }

    // Camera Scanner Dialog
    if (activeScannerTarget != null) {
        CameraScannerDialog(
            title = if (activeScannerTarget == ActiveScanTarget.SERIAL_NUMBER) "Cooler Serial Number" else "Cooler Tag Number",
            onCodeScanned = { scannedCode ->
                if (activeScannerTarget == ActiveScanTarget.SERIAL_NUMBER) {
                    viewModel.coolerSerialNumber.value = scannedCode
                } else {
                    viewModel.coolerTagNumber.value = scannedCode
                }
                activeScannerTarget = null
                Toast.makeText(context, "Scanned: $scannedCode", Toast.LENGTH_SHORT).show()
            },
            onDismiss = {
                activeScannerTarget = null
            }
        )
    }

    // Duplicate Serial/Tag Warning Dialog
    if (duplicateWarnings.isNotEmpty()) {
        DuplicateWarningDialog(
            duplicates = duplicateWarnings,
            onDismiss = {
                viewModel.clearDuplicateWarning()
            },
            onConfirmSaveAnyway = {
                viewModel.forceSaveAfterDuplicateCheck(saveAndNew = pendingSaveAndNew) { andNew ->
                    if (andNew) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Outlet saved! Ready for next outlet.")
                        }
                    } else {
                        Toast.makeText(context, "Outlet audit saved successfully!", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isWithCooler) "Audit With Cooler" else "Audit Without Cooler",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("form_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PureWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Navy900
                )
            )
        },
        bottomBar = {
            Surface(
                color = PureWhite,
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (!gpsStatus.isAccurateEnough) {
                        Text(
                            text = if (gpsStatus.hasLocation)
                                "GPS Accuracy: ${String.format("%.1f", gpsStatus.accuracy)}m (Required: <= 25m). Move to clear sky."
                            else
                                "Waiting for GPS satellite lock... (Required before saving)",
                            color = Color(0xFFB45309),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // SAVE & NEW (1-tap repeat for 30 outlets/day)
                        Button(
                            onClick = {
                                pendingSaveAndNew = true
                                viewModel.attemptSave {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Saved! Ready for next outlet.")
                                    }
                                }
                            },
                            enabled = gpsStatus.isAccurateEnough,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Green700,
                                contentColor = PureWhite
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("save_and_new_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save & New", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        // SAVE & CLOSE
                        Button(
                            onClick = {
                                pendingSaveAndNew = false
                                viewModel.attemptSave {
                                    Toast.makeText(context, "Outlet audit saved successfully!", Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                }
                            },
                            enabled = gpsStatus.isAccurateEnough,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Navy800,
                                contentColor = PureWhite
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("save_and_close_button")
                        ) {
                            Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save & Close", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // SECTION: ENTRY TIMESTAMP & DATE
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, Color(0xFF93C5FD), RoundedCornerShape(12.dp)),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFFE0EAFC), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Event,
                                    contentDescription = null,
                                    tint = Navy800,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "ENTRY TIMESTAMP & DATE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = entryDateFormatted,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Time: $entryTimeFormatted",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Navy800
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .background(Color(0xFFDCFCE7), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "STAMPED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Green700,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // SECTION: GPS LOCATION (CRITICAL GATING)
            item {
                GpsAccuracyCard(
                    gpsStatus = gpsStatus,
                    onRefreshLocation = {
                        locationHelper.startLocationUpdates { lat, lng, acc ->
                            viewModel.updateGpsCoordinates(lat, lng, acc)
                        }
                    },
                    onOpenInMaps = {
                        LocationHelper.openInGoogleMaps(
                            context,
                            gpsStatus.latitude,
                            gpsStatus.longitude,
                            outletName.ifEmpty { "Audited Outlet" }
                        )
                    }
                )
            }

            // SECTION: OUTLET IDENTIFICATION
            item {
                Text(
                    text = "OUTLET DETAILS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    letterSpacing = 0.5.sp
                )
            }

            if (isWithCooler) {
                // Form 1: Account / Distributor
                item {
                    Column {
                        AuditInputField(
                            value = accountDistributor,
                            onValueChange = { viewModel.accountDistributor.value = it },
                            label = "Account / Distributor *",
                            placeholder = "e.g. Coca-Cola Beverages / Eastern Drinks",
                            leadingIcon = Icons.Default.Business,
                            isError = validationErrors.accountError != null,
                            errorMessage = validationErrors.accountError,
                            testTag = "account_distributor_input"
                        )
                        QuickPickChips(
                            title = "Recent Accounts:",
                            suggestions = accountSuggestions,
                            onSelect = { viewModel.accountDistributor.value = it }
                        )
                    }
                }
            }

            // Outlet Name (With Cooler) OR Trading Name (Without Cooler)
            item {
                AuditInputField(
                    value = outletName,
                    onValueChange = { viewModel.outletName.value = it },
                    label = if (isWithCooler) "Outlet Name *" else "Trading Name *",
                    placeholder = if (isWithCooler) "e.g. Mama Joe Grocery Store" else "e.g. Corner Bar & Provisions",
                    leadingIcon = Icons.Default.Store,
                    isError = validationErrors.outletNameError != null,
                    errorMessage = validationErrors.outletNameError,
                    testTag = "outlet_name_input"
                )
            }

            // Owner Name
            item {
                AuditInputField(
                    value = ownerName,
                    onValueChange = { viewModel.ownerName.value = it },
                    label = "Owner Name *",
                    placeholder = "e.g. Joseph Mensah",
                    leadingIcon = Icons.Default.Person,
                    isError = validationErrors.ownerNameError != null,
                    errorMessage = validationErrors.ownerNameError,
                    testTag = "owner_name_input"
                )
            }

            // Owner Phone
            item {
                AuditInputField(
                    value = ownerPhone,
                    onValueChange = { viewModel.ownerPhone.value = it },
                    label = "Owner Phone *",
                    placeholder = "e.g. 0244123456",
                    leadingIcon = Icons.Default.Phone,
                    keyboardType = KeyboardType.Phone,
                    isError = validationErrors.ownerPhoneError != null,
                    errorMessage = validationErrors.ownerPhoneError,
                    testTag = "owner_phone_input"
                )
            }

            // Location (area / town)
            item {
                Column {
                    AuditInputField(
                        value = location,
                        onValueChange = { viewModel.location.value = it },
                        label = "Location (Area / Town) *",
                        placeholder = "e.g. Downtown Central Market, Accra",
                        leadingIcon = Icons.Default.LocationOn,
                        isError = validationErrors.locationError != null,
                        errorMessage = validationErrors.locationError,
                        testTag = "location_input"
                    )
                    QuickPickChips(
                        title = "Recent Locations:",
                        suggestions = locationSuggestions,
                        onSelect = { viewModel.location.value = it }
                    )
                }
            }

            // SECTION: COOLER HARDWARE AUDIT
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isWithCooler) "COOLER SPECIFICATIONS" else "UNSERVICED / MISSING COOLER TRACE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    letterSpacing = 0.5.sp
                )
            }

            if (isWithCooler) {
                // Cooler Make (Frigoglass, Klimasan, Metalfrio, Other)
                item {
                    CoolerMakeSelector(
                        selectedMake = coolerMake,
                        onMakeSelected = { viewModel.coolerMake.value = it },
                        customOtherValue = coolerMakeOther,
                        onCustomOtherChanged = { viewModel.coolerMakeOther.value = it },
                        previousSuggestions = makeSuggestions
                    )
                }
            }

            // Cooler Model
            item {
                AuditInputField(
                    value = coolerModel,
                    onValueChange = { viewModel.coolerModel.value = it },
                    label = "Cooler Model *",
                    placeholder = "e.g. Super 400, FV-650, M500",
                    isError = validationErrors.coolerModelError != null,
                    errorMessage = validationErrors.coolerModelError,
                    testTag = "cooler_model_input"
                )
            }

            // Cooler Serial Number with instant SCAN button
            item {
                ScannableField(
                    value = coolerSerialNumber,
                    onValueChange = { viewModel.coolerSerialNumber.value = it },
                    onScanClick = {
                        val hasCamera = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasCamera) {
                            activeScannerTarget = ActiveScanTarget.SERIAL_NUMBER
                        } else {
                            permissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                        }
                    },
                    label = "Cooler Serial Number *",
                    placeholder = "Scan barcode or enter serial",
                    isError = validationErrors.coolerSerialError != null,
                    errorMessage = validationErrors.coolerSerialError,
                    testTagPrefix = "cooler_serial"
                )
            }

            // Cooler Tag Number with instant SCAN button
            item {
                ScannableField(
                    value = coolerTagNumber,
                    onValueChange = { viewModel.coolerTagNumber.value = it },
                    onScanClick = {
                        val hasCamera = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasCamera) {
                            activeScannerTarget = ActiveScanTarget.TAG_NUMBER
                        } else {
                            permissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                        }
                    },
                    label = "Cooler Tag Number *",
                    placeholder = "Scan asset tag or enter tag ID",
                    isError = validationErrors.coolerTagError != null,
                    errorMessage = validationErrors.coolerTagError,
                    testTagPrefix = "cooler_tag"
                )
            }

            // Optional notes
            item {
                AuditInputField(
                    value = notes,
                    onValueChange = { viewModel.notes.value = it },
                    label = "Field Notes / Condition",
                    placeholder = "e.g. Good condition, clean glass, cold temperature",
                    singleLine = false,
                    imeAction = ImeAction.Done,
                    testTag = "field_notes_input"
                )
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
