package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AuditFormType
import com.example.data.model.AuditRecord
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.viewmodel.AuditViewModel
import com.example.util.LocationHelper

enum class MapTimeFilter {
    TODAY,
    ALL
}

enum class MapCategoryFilter {
    ALL,
    WITH_COOLER,
    WITHOUT_COOLER
}

class MapBridge(
    private val onMarkerSelected: (Long) -> Unit,
    private val onOpenInMaps: (Double, Double, String) -> Unit
) {
    @JavascriptInterface
    fun onMarkerClicked(recordId: Long) {
        onMarkerSelected(recordId)
    }

    @JavascriptInterface
    fun openGoogleMaps(lat: Double, lng: Double, label: String) {
        onOpenInMaps(lat, lng, label)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapViewScreen(
    viewModel: AuditViewModel,
    onNavigateBack: () -> Unit,
    onEditRecord: (AuditRecord) -> Unit
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val allRecords by viewModel.allRecords.collectAsStateWithLifecycle()
    val todayRecords by viewModel.todayRecords.collectAsStateWithLifecycle()

    var timeFilter by remember { mutableStateOf(MapTimeFilter.TODAY) }
    var categoryFilter by remember { mutableStateOf(MapCategoryFilter.ALL) }
    var selectedRecordId by remember { mutableStateOf<Long?>(null) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // Filtered records based on active filters
    val scopedRecords = remember(allRecords, todayRecords, timeFilter, categoryFilter) {
        val baseList = if (timeFilter == MapTimeFilter.TODAY) todayRecords else allRecords
        baseList.filter { record ->
            // Must have valid coordinates
            val hasValidGps = record.latitude != 0.0 && record.longitude != 0.0
            val matchesCategory = when (categoryFilter) {
                MapCategoryFilter.ALL -> true
                MapCategoryFilter.WITH_COOLER -> record.formType == AuditFormType.WITH_COOLER
                MapCategoryFilter.WITHOUT_COOLER -> record.formType == AuditFormType.WITHOUT_COOLER
            }
            hasValidGps && matchesCategory
        }
    }

    val selectedRecord = remember(scopedRecords, selectedRecordId) {
        allRecords.firstOrNull { it.id == selectedRecordId }
    }

    val todayGpsCount = remember(todayRecords) {
        todayRecords.count { it.latitude != 0.0 && it.longitude != 0.0 }
    }
    val allGpsCount = remember(allRecords) {
        allRecords.count { it.latitude != 0.0 && it.longitude != 0.0 }
    }

    val withCoolerGpsCount = remember(scopedRecords) {
        scopedRecords.count { it.formType == AuditFormType.WITH_COOLER }
    }
    val withoutCoolerGpsCount = remember(scopedRecords) {
        scopedRecords.count { it.formType == AuditFormType.WITHOUT_COOLER }
    }

    // Refresh map content when scoped records change
    LaunchedEffect(scopedRecords) {
        webViewInstance?.let { webView ->
            val html = generateLeafletMapHtml(scopedRecords, selectedRecordId)
            webView.loadDataWithBaseURL("https://leaflet.local", html, "text/html", "UTF-8", null)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Audit Coverage Map",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                        Text(
                            text = "${scopedRecords.size} outlets plotted • ${if (timeFilter == MapTimeFilter.TODAY) "Today's Route" else "All Outlets"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("map_back_button")
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
                        onClick = {
                            webViewInstance?.let { webView ->
                                val html = generateLeafletMapHtml(scopedRecords, selectedRecordId)
                                webView.loadDataWithBaseURL("https://leaflet.local", html, "text/html", "UTF-8", null)
                            }
                        },
                        modifier = Modifier.testTag("map_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Map",
                            tint = PureWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Navy900
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    webViewInstance?.evaluateJavascript("fitAllBounds();", null)
                },
                containerColor = Navy800,
                contentColor = PureWhite,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = if (selectedRecord != null) 210.dp else 16.dp)
                    .testTag("map_fit_bounds_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Fit All Locations"
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("map_view_screen")
        ) {
            // 1. Map View (Full screen container)
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        @SuppressLint("SetJavaScriptEnabled")
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        webChromeClient = WebChromeClient()
                        webViewClient = WebViewClient()

                        addJavascriptInterface(
                            MapBridge(
                                onMarkerSelected = { id ->
                                    selectedRecordId = id
                                },
                                onOpenInMaps = { lat, lng, label ->
                                    LocationHelper.openInGoogleMaps(context, lat, lng, label)
                                }
                            ),
                            "AndroidBridge"
                        )

                        val html = generateLeafletMapHtml(scopedRecords, selectedRecordId)
                        loadDataWithBaseURL("https://leaflet.local", html, "text/html", "UTF-8", null)
                        webViewInstance = this
                    }
                },
                update = { webView ->
                    webViewInstance = webView
                },
                modifier = Modifier.fillMaxSize()
            )

            // 2. Top Filter Chips Bar overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color(0xEEFFFFFF))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Row 1: Time filter (Today vs All)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = timeFilter == MapTimeFilter.TODAY,
                        onClick = { timeFilter = MapTimeFilter.TODAY },
                        label = { Text("Today ($todayGpsCount)", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Navy800,
                            selectedLabelColor = PureWhite
                        ),
                        modifier = Modifier.testTag("map_filter_today")
                    )

                    FilterChip(
                        selected = timeFilter == MapTimeFilter.ALL,
                        onClick = { timeFilter = MapTimeFilter.ALL },
                        label = { Text("All ($allGpsCount)", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Navy800,
                            selectedLabelColor = PureWhite
                        ),
                        modifier = Modifier.testTag("map_filter_all")
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Legend indicators
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFF0253B3), CircleShape))
                        Text("Cooler", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Navy900)
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFFF59E0B), CircleShape))
                        Text("No Cooler", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                    }
                }

                // Row 2: Category filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = categoryFilter == MapCategoryFilter.ALL,
                        onClick = { categoryFilter = MapCategoryFilter.ALL },
                        label = { Text("All Categories (${scopedRecords.size})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0253B3),
                            selectedLabelColor = PureWhite
                        ),
                        modifier = Modifier.testTag("map_filter_category_all")
                    )

                    FilterChip(
                        selected = categoryFilter == MapCategoryFilter.WITH_COOLER,
                        onClick = { categoryFilter = MapCategoryFilter.WITH_COOLER },
                        label = { Text("With Cooler ($withCoolerGpsCount)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0253B3),
                            selectedLabelColor = PureWhite
                        ),
                        modifier = Modifier.testTag("map_filter_with_cooler")
                    )

                    FilterChip(
                        selected = categoryFilter == MapCategoryFilter.WITHOUT_COOLER,
                        onClick = { categoryFilter = MapCategoryFilter.WITHOUT_COOLER },
                        label = { Text("Without ($withoutCoolerGpsCount)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFD97706),
                            selectedLabelColor = PureWhite
                        ),
                        modifier = Modifier.testTag("map_filter_without_cooler")
                    )
                }
            }

            // 3. Empty State overlay if no coordinates available
            if (scopedRecords.isEmpty()) {
                ElevatedCard(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = PureWhite)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFFEFF6FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF0253B3),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (timeFilter == MapTimeFilter.TODAY) "No GPS coordinates logged today" else "No GPS coordinates recorded yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "When you audit an outlet with GPS enabled (accuracy <= 25m), its location marker will automatically appear on this interactive coverage map.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onNavigateBack,
                            colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                        ) {
                            Text("Return to Dashboard", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 4. Selected Record Detail Overlay Card (Bottom)
            selectedRecord?.let { record ->
                val isCooler = record.formType == AuditFormType.WITH_COOLER
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                        .testTag("map_selected_record_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = PureWhite),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isCooler) Color(0xFFDBEAFE) else Color(0xFFFEF3C7),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isCooler) "WITH COOLER" else "WITHOUT COOLER",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCooler) Color(0xFF0253B3) else Color(0xFFB45309)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "⏰ ${record.formattedDisplayTime} • ${record.formattedDisplayDate}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = record.outletName.ifEmpty { "Unnamed Outlet" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Navy900
                                )

                                Text(
                                    text = "${record.location.ifEmpty { "Area not specified" }} • Rep: ${record.ownerName.ifEmpty { "N/A" }} (${record.ownerPhone})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF475569)
                                )

                                if (isCooler) {
                                    Text(
                                        text = "Make: ${record.coolerMake} • Model: ${record.coolerModel} • SN: ${record.coolerSerialNumber}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = Color(0xFF0369A1),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    Text(
                                        text = "Serial: ${record.coolerSerialNumber} • Tag: ${record.coolerTagNumber}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = Color(0xFF92400E),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            IconButton(
                                onClick = { selectedRecordId = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Detail",
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Action Buttons in Selected Card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    LocationHelper.openInGoogleMaps(
                                        context,
                                        record.latitude,
                                        record.longitude,
                                        record.outletName
                                    )
                                },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(44.dp)
                                    .testTag("map_open_in_google_maps_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0253B3),
                                    contentColor = PureWhite
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Navigate in Google Maps", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { onEditRecord(record) },
                                modifier = Modifier
                                    .weight(0.8f)
                                    .height(44.dp)
                                    .testTag("map_edit_record_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Generates an interactive Leaflet HTML map that displays customized pins for each outlet
 * with click interactions, bounds fitting, and Google Maps intent integration.
 */
fun generateLeafletMapHtml(records: List<AuditRecord>, selectedId: Long?): String {
    val markersJson = StringBuilder("[")
    records.forEachIndexed { index, r ->
        val safeName = r.outletName.replace("\\", "\\\\").replace("\"", "\\\"").replace("'", "\\'")
        val safeLoc = r.location.replace("\\", "\\\\").replace("\"", "\\\"").replace("'", "\\'")
        val safeTime = r.formattedDisplayTime
        val safeDate = r.formattedDisplayDate
        val safeSerial = r.coolerSerialNumber.replace("\\", "\\\\").replace("\"", "\\\"")
        val safeTag = r.coolerTagNumber.replace("\\", "\\\\").replace("\"", "\\\"")
        val isCooler = r.formType == AuditFormType.WITH_COOLER

        markersJson.append("""
            {
                "id": ${r.id},
                "lat": ${r.latitude},
                "lng": ${r.longitude},
                "name": "$safeName",
                "location": "$safeLoc",
                "time": "$safeTime",
                "date": "$safeDate",
                "serial": "$safeSerial",
                "tag": "$safeTag",
                "isCooler": $isCooler,
                "isSelected": ${r.id == selectedId}
            }
        """)
        if (index < records.size - 1) markersJson.append(",")
    }
    markersJson.append("]")

    return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <style>
        html, body, #map {
            width: 100%;
            height: 100%;
            margin: 0;
            padding: 0;
            background: #f1f5f9;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
        }
        .custom-pin {
            display: flex;
            align-items: center;
            justify-content: center;
            border-radius: 50%;
            border: 2.5px solid #ffffff;
            box-shadow: 0 4px 10px rgba(0, 0, 0, 0.35);
            font-size: 13px;
            font-weight: 800;
            color: #ffffff;
            transition: transform 0.2s ease;
        }
        .pin-cooler {
            background: linear-gradient(135deg, #0284c7, #1e3a8a);
        }
        .pin-nocooler {
            background: linear-gradient(135deg, #f59e0b, #b45309);
        }
        .pin-selected {
            border: 3.5px solid #22c55e !important;
            transform: scale(1.25);
            box-shadow: 0 6px 16px rgba(34, 197, 94, 0.5) !important;
        }
        .leaflet-popup-content-wrapper {
            border-radius: 14px;
            box-shadow: 0 10px 25px rgba(0,0,0,0.2);
            padding: 2px;
        }
        .popup-content {
            padding: 6px;
        }
        .popup-title {
            font-size: 14px;
            font-weight: 700;
            color: #0f172a;
            margin-bottom: 4px;
        }
        .popup-badge {
            display: inline-block;
            font-size: 10px;
            font-weight: 700;
            padding: 2px 7px;
            border-radius: 6px;
            margin-bottom: 6px;
        }
        .badge-cooler {
            background: #dbeafe;
            color: #1e3a8a;
        }
        .badge-nocooler {
            background: #fef3c7;
            color: #92400e;
        }
        .popup-meta {
            font-size: 11px;
            color: #64748b;
            margin-bottom: 2px;
        }
        .popup-action-btn {
            display: block;
            width: 100%;
            background: #0253B3;
            color: #ffffff;
            text-align: center;
            padding: 7px 0;
            border-radius: 8px;
            font-size: 12px;
            font-weight: 700;
            text-decoration: none;
            margin-top: 8px;
            border: none;
            cursor: pointer;
        }
    </style>
</head>
<body>
    <div id="map"></div>
    <script>
        var markersData = $markersJson;
        var map = L.map('map', {
            zoomControl: false,
            attributionControl: false
        });

        // Crisp OpenStreetMap layer
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19
        }).addTo(map);

        L.control.zoom({ position: 'topright' }).addTo(map);

        var markersGroup = L.featureGroup();

        markersData.forEach(function(item, idx) {
            var pinClass = item.isCooler ? 'pin-cooler' : 'pin-nocooler';
            if (item.isSelected) {
                pinClass += ' pin-selected';
            }
            var iconHtml = item.isCooler ? '❄️' : '⚠️';

            var customIcon = L.divIcon({
                className: 'custom-pin ' + pinClass,
                html: '<span>' + iconHtml + '</span>',
                iconSize: [34, 34],
                iconAnchor: [17, 17],
                popupAnchor: [0, -18]
            });

            var marker = L.marker([item.lat, item.lng], { icon: customIcon });

            var badgeClass = item.isCooler ? 'badge-cooler' : 'badge-nocooler';
            var badgeText = item.isCooler ? 'WITH COOLER' : 'WITHOUT COOLER';

            var popupHtml = '<div class="popup-content">' +
                '<span class="popup-badge ' + badgeClass + '">' + badgeText + '</span>' +
                '<div class="popup-title">' + item.name + '</div>' +
                '<div class="popup-meta">📍 ' + item.location + '</div>' +
                '<div class="popup-meta">⏰ ' + item.time + ' (' + item.date + ')</div>' +
                '<div class="popup-meta">🏷️ Serial: ' + item.serial + ' • Tag: ' + item.tag + '</div>' +
                '<button class="popup-action-btn" onclick="openMaps(' + item.lat + ',' + item.lng + ',\'' + item.name + '\')">Open in Google Maps</button>' +
                '</div>';

            marker.bindPopup(popupHtml);

            marker.on('click', function() {
                if (window.AndroidBridge && window.AndroidBridge.onMarkerClicked) {
                    window.AndroidBridge.onMarkerClicked(item.id);
                }
            });

            marker.addTo(markersGroup);
        });

        markersGroup.addTo(map);

        function fitAllBounds() {
            if (markersData.length > 0) {
                map.fitBounds(markersGroup.getBounds().pad(0.18));
            } else {
                map.setView([5.6037, -0.1870], 12);
            }
        }

        function openMaps(lat, lng, name) {
            if (window.AndroidBridge && window.AndroidBridge.openGoogleMaps) {
                window.AndroidBridge.openGoogleMaps(lat, lng, name);
            }
        }

        // Initial bounds zoom
        if (markersData.length === 1) {
            map.setView([markersData[0].lat, markersData[0].lng], 16);
        } else if (markersData.length > 1) {
            fitAllBounds();
        } else {
            map.setView([5.6037, -0.1870], 12);
        }
    </script>
</body>
</html>
    """.trimIndent()
}
