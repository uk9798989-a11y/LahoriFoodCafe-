package com.example.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LocationHelper
import com.example.model.BranchDirectory
import com.example.model.CafeBranch
import com.example.model.OrderType
import com.example.model.SavedAddress
import com.example.ui.theme.LahoriDarkCard
import com.example.ui.theme.LahoriDarkSheet
import com.example.ui.theme.LahoriGold
import com.example.ui.theme.LahoriRed
import com.example.ui.theme.LahoriSuccess
import com.example.ui.theme.LahoriWhite
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SavedAddressesBottomSheet(
    savedAddresses: List<SavedAddress>,
    selectedAddressStreet: String,
    onSelectAddress: (SavedAddress) -> Unit,
    onAddNewAddress: (label: String, street: String, area: String) -> Unit,
    onStartOrder: () -> Unit,
    onDismiss: () -> Unit,
    branches: List<CafeBranch> = BranchDirectory.defaultBranches,
    orderType: OrderType = OrderType.DELIVERY,
    selectedBranchId: String = BranchDirectory.branch1.id,
    deviceLatitude: Double? = null,
    deviceLongitude: Double? = null,
    detectedGpsAddress: String = "",
    isLocatingGps: Boolean = false,
    onOrderTypeChange: (OrderType) -> Unit = {},
    onUseCurrentLocation: () -> Unit = {},
    onSelectPickupBranch: (CafeBranch) -> Unit = {},
    onSelectQuickArea: (area: String, branchId: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var activeMode by remember(orderType) { mutableStateOf(orderType) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddForm by remember { mutableStateOf(false) }
    var labelInput by remember { mutableStateOf("Home") }
    var streetInput by remember { mutableStateOf("") }
    var areaInput by remember { mutableStateOf("Taranda Muhammad Panah (TMP)") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var permissionDeniedNote by remember { mutableStateOf<String?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
            (grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        if (granted) {
            permissionDeniedNote = null
            onUseCurrentLocation()
        } else {
            permissionDeniedNote = "Location permission denied. Choose a branch or delivery zone below."
        }
    }

    val activeBranches = remember(branches) {
        branches.filter { it.active }.ifEmpty { branches.ifEmpty { BranchDirectory.defaultBranches } }
    }

    val sortedBranchesWithDistance = remember(activeBranches, deviceLatitude, deviceLongitude, searchQuery) {
        val baseList = if (deviceLatitude != null && deviceLongitude != null) {
            BranchDirectory.branchesSortedByDistance(deviceLatitude, deviceLongitude, activeBranches)
        } else {
            activeBranches.sortedBy { it.sortOrder }.map { it to null }
        }
        val query = searchQuery.trim().lowercase()
        if (query.isBlank()) {
            baseList
        } else {
            baseList.filter { (branch, _) ->
                branch.name.lowercase().contains(query) ||
                    branch.shortLabel.lowercase().contains(query) ||
                    branch.shortAddress.lowercase().contains(query) ||
                    branch.areas.any { it.lowercase().contains(query) }
            }
        }
    }

    val quickAreas = remember(activeBranches, searchQuery) {
        val allZones = activeBranches
            .sortedBy { it.sortOrder }
            .flatMap { it.areas.take(5) }
            .distinct()
            .ifEmpty {
                listOf(
                    "Taranda Muhammad Panah",
                    "TMP",
                    "Main KLP Road",
                    "Zahir Pir",
                    "Khanpur",
                    "M-5 Interchange"
                )
            }
        val query = searchQuery.trim().lowercase()
        if (query.isBlank()) allZones.take(10)
        else allZones.filter { it.lowercase().contains(query) }.take(10)
    }

    val filteredAddresses = remember(savedAddresses, searchQuery) {
        val query = searchQuery.trim().lowercase()
        if (query.isBlank()) savedAddresses
        else savedAddresses.filter {
            it.label.lowercase().contains(query) ||
                it.area.lowercase().contains(query) ||
                it.streetAddress.lowercase().contains(query) ||
                it.branchName.lowercase().contains(query)
        }
    }

    val previewBranch = remember(areaInput, streetInput, activeBranches) {
        BranchDirectory.routeAddressToBranch("$areaInput $streetInput", activeBranches)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LahoriDarkSheet,
        contentColor = LahoriWhite,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (activeMode == OrderType.DELIVERY) "CHOOSE LOCATION & BRANCH" else "SELECT PICKUP STORE",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        color = LahoriWhite
                    )
                    Text(
                        text = "Lahori Food Cafe • TMP & Zahir Pir Live Store Finder",
                        style = MaterialTheme.typography.bodySmall,
                        color = LahoriWhite.copy(alpha = 0.68f)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = LahoriRed,
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_saved_addresses_x")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close Location Selector",
                            tint = LahoriWhite
                        )
                    }
                }
            }

            // KFC-style Segmented Delivery / Pickup Mode Switcher
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = LahoriDarkCard,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(OrderType.DELIVERY, OrderType.PICKUP).forEach { mode ->
                        val selected = activeMode == mode
                        Surface(
                            onClick = {
                                activeMode = mode
                                onOrderTypeChange(mode)
                            },
                            shape = RoundedCornerShape(9.dp),
                            color = if (selected) LahoriRed else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("sheet_mode_${mode.name.lowercase()}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = if (mode == OrderType.DELIVERY) Icons.Filled.DeliveryDining else Icons.Filled.ShoppingBag,
                                    contentDescription = mode.label,
                                    tint = LahoriWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = LahoriWhite
                                )
                            }
                        }
                    }
                }
            }

            // Use GPS Location Card + Search Field
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (LocationHelper.hasLocationPermission(context)) {
                            permissionDeniedNote = null
                            onUseCurrentLocation()
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    enabled = !isLocatingGps,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LahoriGold,
                        contentColor = Color(0xFF141414)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("use_gps_location_button")
                ) {
                    if (isLocatingGps) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color(0xFF141414),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("DETECTING NEAREST BRANCH...", style = MaterialTheme.typography.labelLarge)
                    } else {
                        Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("USE MY GPS LOCATION", style = MaterialTheme.typography.labelLarge)
                    }
                }

                if (permissionDeniedNote != null) {
                    Text(
                        text = permissionDeniedNote!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = LahoriGold
                    )
                }

                if (detectedGpsAddress.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LahoriSuccess.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, LahoriSuccess.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(18.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "DETECTED GPS LOCATION",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF4ADE80)
                                )
                                Text(
                                    text = detectedGpsAddress,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LahoriWhite,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search TMP, Zahir Pir, KLP Road, Khanpur...",
                            color = LahoriWhite.copy(alpha = 0.55f)
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = LahoriGold)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = LahoriWhite)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LahoriWhite,
                        unfocusedTextColor = LahoriWhite,
                        focusedBorderColor = LahoriGold,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                        focusedContainerColor = LahoriDarkCard,
                        unfocusedContainerColor = LahoriDarkCard
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("location_search_input")
                )
            }

            // Scrollable Stores, Quick Zones, and Saved Addresses
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 330.dp)
            ) {
                if (activeMode == OrderType.DELIVERY && quickAreas.isNotEmpty()) {
                    item(key = "quick_zones_section") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "DELIVERY AREAS",
                                style = MaterialTheme.typography.labelMedium,
                                color = LahoriGold
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                quickAreas.forEach { zone ->
                                    val isAreaSelected = selectedAddressStreet.contains(zone, ignoreCase = true)
                                    FilterChip(
                                        selected = isAreaSelected,
                                        onClick = {
                                            val matchedBranch = BranchDirectory.routeAddressToBranch(zone, activeBranches)
                                            areaInput = zone
                                            onSelectQuickArea(zone, matchedBranch.id)
                                        },
                                        label = { Text(zone, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = LahoriRed,
                                            selectedLabelColor = LahoriWhite,
                                            containerColor = LahoriDarkCard,
                                            labelColor = LahoriWhite.copy(alpha = 0.88f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "branches_heading") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (deviceLatitude != null) "BRANCHES NEAREST TO YOU" else "LAHORI FOOD CAFE BRANCHES (${sortedBranchesWithDistance.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = LahoriGold
                        )
                        if (deviceLatitude != null) {
                            Text(
                                text = "Sorted by GPS",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4ADE80)
                            )
                        }
                    }
                }

                items(sortedBranchesWithDistance.size, key = { idx -> "branch_${sortedBranchesWithDistance[idx].first.id}" }) { index ->
                    val (branch, distanceKm) = sortedBranchesWithDistance[index]
                    val isSelected = branch.id == selectedBranchId
                    val isNearest = index == 0 && distanceKm != null
                    StoreLocatorBranchCard(
                        branch = branch,
                        distanceKm = distanceKm,
                        isNearest = isNearest,
                        isSelected = isSelected,
                        onSelect = {
                            onSelectPickupBranch(branch)
                        },
                        onDirectionsClick = {
                            LocationHelper.openBranchInMaps(context, branch)
                        }
                    )
                }

                if (activeMode == OrderType.DELIVERY && filteredAddresses.isNotEmpty()) {
                    item(key = "saved_addresses_heading") {
                        Text(
                            text = "YOUR SAVED ADDRESSES (${filteredAddresses.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = LahoriGold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(filteredAddresses.size, key = { idx -> "addr_${filteredAddresses[idx].id}" }) { idx ->
                        val addr = filteredAddresses[idx]
                        val isSelected = selectedAddressStreet.contains(addr.streetAddress)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = LahoriDarkCard,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) LahoriRed else Color.White.copy(alpha = 0.14f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectAddress(addr) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = null,
                                        tint = LahoriRed
                                    )
                                    Column {
                                        Text(
                                            text = "${addr.label} • ${addr.area}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = LahoriWhite
                                        )
                                        Text(
                                            text = addr.streetAddress,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LahoriWhite.copy(alpha = 0.72f)
                                        )
                                        Text(
                                            text = "${addr.branchId.uppercase()}: ${addr.branchName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LahoriGold,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = LahoriRed
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = activeMode == OrderType.DELIVERY && showAddForm) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = LahoriDarkCard,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "ADD FULL DELIVERY ADDRESS",
                            style = MaterialTheme.typography.labelLarge,
                            color = LahoriGold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = labelInput,
                                onValueChange = { labelInput = it },
                                label = { Text("Label") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = LahoriWhite,
                                    unfocusedTextColor = LahoriWhite,
                                    focusedBorderColor = LahoriRed,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                                    focusedLabelColor = LahoriWhite,
                                    unfocusedLabelColor = LahoriWhite.copy(alpha = 0.65f)
                                ),
                                modifier = Modifier.weight(0.38f)
                            )
                            OutlinedTextField(
                                value = areaInput,
                                onValueChange = { areaInput = it },
                                label = { Text("Area / City") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = LahoriWhite,
                                    unfocusedTextColor = LahoriWhite,
                                    focusedBorderColor = LahoriRed,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                                    focusedLabelColor = LahoriWhite,
                                    unfocusedLabelColor = LahoriWhite.copy(alpha = 0.65f)
                                ),
                                modifier = Modifier.weight(0.62f)
                            )
                        }

                        OutlinedTextField(
                            value = streetInput,
                            onValueChange = {
                                streetInput = it
                                validationError = null
                            },
                            label = { Text("House / Street / Landmark") },
                            placeholder = { Text("e.g. Near Main KLP Chowk") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = LahoriWhite,
                                unfocusedTextColor = LahoriWhite,
                                focusedBorderColor = LahoriRed,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                                focusedLabelColor = LahoriWhite,
                                unfocusedLabelColor = LahoriWhite.copy(alpha = 0.65f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("address_street_input")
                        )

                        Text(
                            text = "Auto-routed to: ${previewBranch.displayShortLabel()} (${previewBranch.name})",
                            style = MaterialTheme.typography.bodySmall,
                            color = LahoriGold
                        )

                        if (validationError != null) {
                            Text(
                                text = validationError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = LahoriRed
                            )
                        }

                        Button(
                            onClick = {
                                if (streetInput.trim().length < 3 || areaInput.trim().isBlank()) {
                                    validationError = "Please enter at least 3 characters for street and area."
                                } else {
                                    onAddNewAddress(labelInput.trim(), streetInput.trim(), areaInput.trim())
                                    showAddForm = false
                                    streetInput = ""
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LahoriRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_new_address_btn")
                        ) {
                            Text("SAVE & ROUTE TO ${previewBranch.displayShortLabel().uppercase()}", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            // Bottom Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (activeMode == OrderType.DELIVERY) {
                    OutlinedButton(
                        onClick = { showAddForm = !showAddForm },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.5.dp, LahoriWhite.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LahoriWhite),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("add_new_address_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showAddForm) "HIDE FORM" else "ADD ADDRESS",
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                            maxLines = 1
                        )
                    }
                }

                Button(
                    onClick = onStartOrder,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LahoriRed,
                        contentColor = LahoriWhite
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("start_your_order_button")
                ) {
                    Text(
                        text = if (activeMode == OrderType.PICKUP) "CONFIRM BRANCH" else "START YOUR ORDER",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickupBranchBottomSheet(
    branches: List<CafeBranch>,
    selectedBranchId: String,
    onSelectBranch: (CafeBranch) -> Unit,
    onDismiss: () -> Unit,
    deviceLatitude: Double? = null,
    deviceLongitude: Double? = null,
    detectedGpsAddress: String = "",
    isLocatingGps: Boolean = false,
    onUseCurrentLocation: () -> Unit = {},
    onSwitchToDelivery: () -> Unit = {}
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var permissionDeniedNote by remember { mutableStateOf<String?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
            (grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        if (granted) {
            permissionDeniedNote = null
            onUseCurrentLocation()
        } else {
            permissionDeniedNote = "Location permission denied. Select any branch below."
        }
    }

    val activeBranches = remember(branches) {
        branches.filter { it.active }.ifEmpty { branches.ifEmpty { BranchDirectory.defaultBranches } }
    }

    val sortedBranches = remember(activeBranches, deviceLatitude, deviceLongitude, searchQuery) {
        val list = if (deviceLatitude != null && deviceLongitude != null) {
            BranchDirectory.branchesSortedByDistance(deviceLatitude, deviceLongitude, activeBranches)
        } else {
            activeBranches.sortedBy { it.sortOrder }.map { it to null }
        }
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) list
        else list.filter { (branch, _) ->
            branch.name.lowercase().contains(q) ||
                branch.shortLabel.lowercase().contains(q) ||
                branch.shortAddress.lowercase().contains(q) ||
                branch.areas.any { it.lowercase().contains(q) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LahoriDarkSheet,
        contentColor = LahoriWhite,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SELECT PICKUP BRANCH",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        color = LahoriWhite
                    )
                    Text(
                        text = "${sortedBranches.size} Lahori Food Cafe store${if (sortedBranches.size == 1) "" else "s"} ready for pickup",
                        style = MaterialTheme.typography.bodySmall,
                        color = LahoriWhite.copy(alpha = 0.68f)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = LahoriRed,
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_pickup_branch_x")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close Pickup Branch Picker",
                            tint = LahoriWhite
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (LocationHelper.hasLocationPermission(context)) {
                            permissionDeniedNote = null
                            onUseCurrentLocation()
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    enabled = !isLocatingGps,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LahoriGold,
                        contentColor = Color(0xFF141414)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("pickup_use_gps_button")
                ) {
                    if (isLocatingGps) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF141414), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("LOCATING...", style = MaterialTheme.typography.labelLarge)
                    } else {
                        Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("FIND NEAREST", style = MaterialTheme.typography.labelLarge)
                    }
                }

                OutlinedButton(
                    onClick = onSwitchToDelivery,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, LahoriWhite.copy(alpha = 0.45f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LahoriWhite),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("pickup_switch_to_delivery")
                ) {
                    Icon(Icons.Filled.DeliveryDining, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("DELIVERY", style = MaterialTheme.typography.labelLarge)
                }
            }

            if (permissionDeniedNote != null) {
                Text(permissionDeniedNote!!, style = MaterialTheme.typography.bodySmall, color = LahoriGold)
            }

            if (detectedGpsAddress.isNotBlank()) {
                Text(
                    text = "Near: $detectedGpsAddress",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4ADE80),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search branch name or city...", color = LahoriWhite.copy(alpha = 0.55f)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = LahoriGold) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = LahoriWhite,
                    unfocusedTextColor = LahoriWhite,
                    focusedBorderColor = LahoriGold,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                    focusedContainerColor = LahoriDarkCard,
                    unfocusedContainerColor = LahoriDarkCard
                ),
                modifier = Modifier.fillMaxWidth()
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 330.dp)
            ) {
                items(sortedBranches.size, key = { idx -> sortedBranches[idx].first.id }) { idx ->
                    val (branch, distanceKm) = sortedBranches[idx]
                    val isSelected = branch.id == selectedBranchId
                    val isNearest = idx == 0 && distanceKm != null
                    StoreLocatorBranchCard(
                        branch = branch,
                        distanceKm = distanceKm,
                        isNearest = isNearest,
                        isSelected = isSelected,
                        onSelect = { onSelectBranch(branch) },
                        onDirectionsClick = { LocationHelper.openBranchInMaps(context, branch) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StoreLocatorBranchCard(
    branch: CafeBranch,
    distanceKm: Double?,
    isNearest: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDirectionsClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = LahoriDarkCard,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) LahoriRed else Color.White.copy(alpha = 0.14f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag("pickup_branch_card_${branch.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Storefront,
                        contentDescription = null,
                        tint = if (isSelected) LahoriRed else LahoriGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = branch.displayShortLabel(),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = LahoriGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isNearest) {
                        Surface(
                            color = LahoriGold,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "NEAREST",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Black),
                                color = Color(0xFF141414),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (distanceKm != null) {
                        Surface(
                            color = LahoriWhite.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.1f km", distanceKm),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4ADE80),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Selected Branch",
                            tint = LahoriRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Text(
                text = branch.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = LahoriWhite
            )
            Text(
                text = branch.shortAddress,
                style = MaterialTheme.typography.bodySmall,
                color = LahoriWhite.copy(alpha = 0.74f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = if (branch.active) LahoriSuccess.copy(alpha = 0.18f) else LahoriRed.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (branch.active) "OPEN" else "PAUSED",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = if (branch.active) Color(0xFF4ADE80) else LahoriRed,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = LahoriWhite.copy(alpha = 0.6f),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = branch.openHours.ifBlank { "12:00 PM - 2:00 AM" },
                        style = MaterialTheme.typography.labelSmall,
                        color = LahoriWhite.copy(alpha = 0.72f)
                    )
                }

                TextButton(
                    onClick = onDirectionsClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("branch_directions_${branch.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Directions,
                        contentDescription = "Open ${branch.displayShortLabel()} in Google Maps",
                        tint = LahoriGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Directions",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = LahoriGold
                    )
                }
            }
        }
    }
}
