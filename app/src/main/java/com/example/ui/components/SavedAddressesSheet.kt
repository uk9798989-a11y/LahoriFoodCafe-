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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalOffer
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
import com.example.ui.theme.LahoriBlack
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
    onAddNewAddress: (label: String, street: String, area: String) -> Unit = { _, _, _ -> },
    onStartOrder: () -> Unit,
    onDismiss: () -> Unit,
    branches: List<CafeBranch> = BranchDirectory.defaultBranches,
    orderType: OrderType = OrderType.DELIVERY,
    selectedBranchId: String = BranchDirectory.branch1.id,
    selectedLandmark: String = "",
    selectedAddressLabel: String = "Home",
    deviceLatitude: Double? = null,
    deviceLongitude: Double? = null,
    detectedGpsAddress: String = "",
    isLocatingGps: Boolean = false,
    onOrderTypeChange: (OrderType) -> Unit = {},
    onUseCurrentLocation: () -> Unit = {},
    onSelectPickupBranch: (CafeBranch) -> Unit = {},
    onSelectQuickArea: (area: String, branchId: String) -> Unit = { _, _ -> },
    onSaveManualAddress: (editingAddressId: String?, label: String, fullAddress: String, landmark: String) -> Unit = { _, lbl, addr, _ ->
        onAddNewAddress(lbl, addr, "")
    },
    onDeleteAddress: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var activeMode by remember(orderType) { mutableStateOf(orderType) }
    var searchQuery by remember { mutableStateOf("") }
    var editingAddressId by remember { mutableStateOf<String?>(null) }
    var labelInput by remember(selectedAddressLabel) {
        mutableStateOf(selectedAddressLabel.ifBlank { "Home" })
    }
    var fullAddressInput by remember(selectedAddressStreet) {
        mutableStateOf(selectedAddressStreet)
    }
    var landmarkInput by remember(selectedLandmark) {
        mutableStateOf(selectedLandmark)
    }
    var validationError by remember { mutableStateOf<String?>(null) }
    var saveSuccessMessage by remember { mutableStateOf<String?>(null) }
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
            permissionDeniedNote = "GPS is optional — you can type your complete address above anytime."
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
                it.landmark.lowercase().contains(query) ||
                it.branchName.lowercase().contains(query)
        }
    }

    val previewBranch = remember(fullAddressInput, landmarkInput, activeBranches) {
        val combined = "$fullAddressInput $landmarkInput".trim()
        if (combined.isBlank()) {
            BranchDirectory.byId(selectedBranchId, activeBranches)
        } else {
            BranchDirectory.routeAddressToBranch(combined, activeBranches)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LahoriBlack,
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
                        text = if (activeMode == OrderType.DELIVERY) "DELIVERY ADDRESS" else "SELECT PICKUP STORE",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        color = LahoriWhite
                    )
                    Text(
                        text = if (activeMode == OrderType.DELIVERY) {
                            "FREE DELIVERY (Rs. 0) • Type your address manually — no GPS needed"
                        } else {
                            "Lahori Food Cafe • TMP & Zahir Pir Live Store Finder"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = LahoriGold
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

            // Segmented Delivery / Pickup Mode Switcher
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = LahoriDarkCard,
                border = BorderStroke(1.dp, LahoriGold.copy(alpha = 0.28f)),
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
                                    text = if (mode == OrderType.DELIVERY) "DELIVERY • FREE" else mode.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = LahoriWhite
                                )
                            }
                        }
                    }
                }
            }

            // Main Scrollable Area: Default Manual Address Form + Saved Addresses + Branches + Optional GPS
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp, max = 440.dp)
            ) {
                // DEFAULT MANUAL ADDRESS FORM (always visible in Delivery mode)
                if (activeMode == OrderType.DELIVERY) {
                    item(key = "manual_address_form_card") {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = LahoriDarkCard,
                            border = BorderStroke(1.5.dp, LahoriGold.copy(alpha = 0.65f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.LocationOn,
                                            contentDescription = null,
                                            tint = LahoriGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = if (editingAddressId != null) "EDIT SAVED ADDRESS" else "ENTER DELIVERY ADDRESS",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                            color = LahoriGold
                                        )
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = LahoriSuccess.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, Color(0xFF4ADE80).copy(alpha = 0.6f))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.LocalOffer,
                                                contentDescription = null,
                                                tint = Color(0xFF4ADE80),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "FREE DELIVERY",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 10.sp
                                                ),
                                                color = Color(0xFF4ADE80)
                                            )
                                        }
                                    }
                                }

                                // Quick Label Chips
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf("Home", "Office", "Shop", "Other").forEach { tag ->
                                        val isTagSelected = labelInput.equals(tag, ignoreCase = true)
                                        FilterChip(
                                            selected = isTagSelected,
                                            onClick = { labelInput = tag },
                                            label = {
                                                Text(
                                                    text = tag,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = LahoriGold,
                                                selectedLabelColor = LahoriBlack,
                                                containerColor = LahoriBlack,
                                                labelColor = LahoriWhite.copy(alpha = 0.85f)
                                            )
                                        )
                                    }
                                }

                                // Complete Delivery Address Input (Required)
                                OutlinedTextField(
                                    value = fullAddressInput,
                                    onValueChange = {
                                        fullAddressInput = it
                                        validationError = null
                                        saveSuccessMessage = null
                                    },
                                    label = { Text("Complete Delivery Address *") },
                                    placeholder = {
                                        Text(
                                            text = "Enter your complete delivery address (House No, Street, Area, Landmark)",
                                            color = LahoriWhite.copy(alpha = 0.55f),
                                            fontSize = 13.sp
                                        )
                                    },
                                    minLines = 2,
                                    maxLines = 4,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = LahoriWhite,
                                        unfocusedTextColor = LahoriWhite,
                                        focusedBorderColor = LahoriGold,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.28f),
                                        focusedLabelColor = LahoriGold,
                                        unfocusedLabelColor = LahoriWhite.copy(alpha = 0.72f),
                                        focusedContainerColor = LahoriBlack,
                                        unfocusedContainerColor = LahoriBlack,
                                        cursorColor = LahoriGold
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("address_street_input")
                                )

                                // Optional Nearby Landmark Input
                                OutlinedTextField(
                                    value = landmarkInput,
                                    onValueChange = {
                                        landmarkInput = it
                                        saveSuccessMessage = null
                                    },
                                    label = { Text("Nearby landmark (optional)") },
                                    placeholder = {
                                        Text(
                                            text = "Nearby landmark (optional)",
                                            color = LahoriWhite.copy(alpha = 0.55f),
                                            fontSize = 13.sp
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = LahoriWhite,
                                        unfocusedTextColor = LahoriWhite,
                                        focusedBorderColor = LahoriGold,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.24f),
                                        focusedLabelColor = LahoriGold,
                                        unfocusedLabelColor = LahoriWhite.copy(alpha = 0.68f),
                                        focusedContainerColor = LahoriBlack,
                                        unfocusedContainerColor = LahoriBlack,
                                        cursorColor = LahoriGold
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("address_landmark_input")
                                )

                                Text(
                                    text = "Serving Branch: ${previewBranch.displayShortLabel()} (${previewBranch.name}) • Rs. 0 Delivery",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LahoriGold
                                )

                                if (validationError != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = LahoriRed.copy(alpha = 0.18f),
                                        border = BorderStroke(1.dp, LahoriRed),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = validationError!!,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = LahoriWhite,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                        )
                                    }
                                }

                                if (saveSuccessMessage != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = LahoriSuccess.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, Color(0xFF4ADE80)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF4ADE80),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = saveSuccessMessage!!,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = Color(0xFF4ADE80)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (editingAddressId != null) {
                                        OutlinedButton(
                                            onClick = {
                                                editingAddressId = null
                                                labelInput = "Home"
                                                fullAddressInput = selectedAddressStreet
                                                landmarkInput = selectedLandmark
                                                validationError = null
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, LahoriWhite.copy(alpha = 0.45f)),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LahoriWhite),
                                            modifier = Modifier.height(48.dp)
                                        ) {
                                            Text("CANCEL", style = MaterialTheme.typography.labelLarge)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            val cleanAddr = fullAddressInput.trim()
                                            val cleanLandmark = landmarkInput.trim()
                                            if (cleanAddr.isBlank()) {
                                                validationError = "Please enter your complete delivery address (House No, Street, Area, City)."
                                            } else if (cleanAddr.length < 3) {
                                                validationError = "Please enter a complete delivery address (at least 3 characters)."
                                            } else {
                                                validationError = null
                                                val wasEdit = editingAddressId != null
                                                onSaveManualAddress(
                                                    editingAddressId,
                                                    labelInput.trim().ifBlank { "Home" },
                                                    cleanAddr,
                                                    cleanLandmark
                                                )
                                                editingAddressId = null
                                                saveSuccessMessage = if (wasEdit) {
                                                    "Address updated and saved for future orders!"
                                                } else {
                                                    "Address saved! It will auto-fill on your future orders."
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = LahoriRed,
                                            contentColor = LahoriWhite
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("save_new_address_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = if (editingAddressId != null) "UPDATE ADDRESS" else "SAVE ADDRESS",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Saved Addresses List with Edit / Delete / Select
                    if (filteredAddresses.isNotEmpty()) {
                        item(key = "saved_addresses_heading") {
                            Text(
                                text = "YOUR SAVED ADDRESSES (${filteredAddresses.size}) • TAP TO USE OR EDIT",
                                style = MaterialTheme.typography.labelMedium,
                                color = LahoriGold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        items(filteredAddresses.size, key = { idx -> "addr_${filteredAddresses[idx].id}" }) { idx ->
                            val addr = filteredAddresses[idx]
                            val isSelected = selectedAddressStreet.isNotBlank() &&
                                (selectedAddressStreet.equals(addr.streetAddress, ignoreCase = true) ||
                                    selectedAddressStreet.contains(addr.streetAddress, ignoreCase = true))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = LahoriDarkCard,
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) LahoriGold else Color.White.copy(alpha = 0.14f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        fullAddressInput = addr.streetAddress
                                        landmarkInput = addr.landmark
                                        labelInput = addr.label.ifBlank { "Home" }
                                        editingAddressId = null
                                        validationError = null
                                        saveSuccessMessage = "Selected '${addr.label}' address for delivery."
                                        onSelectAddress(addr)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.LocationOn,
                                            contentDescription = null,
                                            tint = if (isSelected) LahoriGold else LahoriRed,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = if (addr.area.isNotBlank()) "${addr.label} • ${addr.area}" else addr.label,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = LahoriWhite
                                            )
                                            Text(
                                                text = addr.streetAddress,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = LahoriWhite.copy(alpha = 0.84f)
                                            )
                                            if (addr.landmark.isNotBlank()) {
                                                Text(
                                                    text = "Landmark: ${addr.landmark}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = LahoriWhite.copy(alpha = 0.65f)
                                                )
                                            }
                                            Text(
                                                text = "${addr.branchName} • FREE Delivery",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = LahoriGold
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                editingAddressId = addr.id
                                                labelInput = addr.label.ifBlank { "Home" }
                                                fullAddressInput = addr.streetAddress
                                                landmarkInput = addr.landmark
                                                validationError = null
                                                saveSuccessMessage = null
                                            },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("edit_saved_address_${addr.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Edit,
                                                contentDescription = "Edit saved address",
                                                tint = LahoriGold,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onDeleteAddress(addr.id) },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("delete_saved_address_${addr.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.DeleteOutline,
                                                contentDescription = "Delete saved address",
                                                tint = LahoriRed,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Quick Delivery Areas
                    if (quickAreas.isNotEmpty()) {
                        item(key = "quick_zones_section") {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "QUICK DELIVERY AREAS (TAP TO FILL)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = LahoriGold
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    quickAreas.forEach { zone ->
                                        val isAreaSelected = fullAddressInput.contains(zone, ignoreCase = true)
                                        FilterChip(
                                            selected = isAreaSelected,
                                            onClick = {
                                                val matchedBranch = BranchDirectory.routeAddressToBranch(zone, activeBranches)
                                                if (fullAddressInput.isBlank()) {
                                                    fullAddressInput = zone
                                                } else if (!fullAddressInput.contains(zone, ignoreCase = true)) {
                                                    fullAddressInput = "${fullAddressInput.trim()}, $zone"
                                                }
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

                    // Secondary Optional GPS Button (never required)
                    item(key = "optional_gps_section") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
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
                                border = BorderStroke(1.dp, LahoriGold.copy(alpha = 0.55f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LahoriGold),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("use_gps_location_button")
                            ) {
                                if (isLocatingGps) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = LahoriGold,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("DETECTING GPS (OPTIONAL)...", style = MaterialTheme.typography.labelLarge)
                                } else {
                                    Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("USE CURRENT GPS LOCATION (OPTIONAL)", style = MaterialTheme.typography.labelLarge)
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
                                    shape = RoundedCornerShape(10.dp),
                                    color = LahoriSuccess.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, LahoriSuccess.copy(alpha = 0.45f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "OPTIONAL GPS DETECTED",
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
                                        TextButton(
                                            onClick = {
                                                fullAddressInput = detectedGpsAddress
                                                validationError = null
                                            }
                                        ) {
                                            Text("USE", color = LahoriGold, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // PICKUP MODE: Search + Optional GPS + Store List
                    item(key = "pickup_controls") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    }
                }

                // Branches List (available in both modes)
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
            }

            // Bottom Primary CTA
            Button(
                onClick = {
                    if (activeMode == OrderType.DELIVERY) {
                        val cleanAddr = fullAddressInput.trim()
                        if (cleanAddr.isBlank()) {
                            validationError = "Please enter your complete delivery address before confirming."
                            return@Button
                        }
                        if (cleanAddr != selectedAddressStreet || landmarkInput.trim() != selectedLandmark) {
                            onSaveManualAddress(
                                editingAddressId,
                                labelInput.trim().ifBlank { "Home" },
                                cleanAddr,
                                landmarkInput.trim()
                            )
                        }
                    }
                    onStartOrder()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LahoriRed,
                    contentColor = LahoriWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("start_your_order_button")
            ) {
                Text(
                    text = if (activeMode == OrderType.PICKUP) "CONFIRM PICKUP BRANCH" else "CONFIRM ADDRESS & ORDER",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Black),
                    maxLines = 1
                )
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
