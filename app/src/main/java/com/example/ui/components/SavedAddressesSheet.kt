package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BranchDirectory
import com.example.model.SavedAddress
import com.example.ui.theme.LahoriDarkCard
import com.example.ui.theme.LahoriDarkSheet
import com.example.ui.theme.LahoriGold
import com.example.ui.theme.LahoriRed
import com.example.ui.theme.LahoriWhite

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SavedAddressesBottomSheet(
    savedAddresses: List<SavedAddress>,
    selectedAddressStreet: String,
    onSelectAddress: (SavedAddress) -> Unit,
    onAddNewAddress: (label: String, street: String, area: String) -> Unit,
    onStartOrder: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAddForm by remember { mutableStateOf(savedAddresses.isEmpty()) }
    var labelInput by remember { mutableStateOf("Home") }
    var streetInput by remember { mutableStateOf("") }
    var areaInput by remember { mutableStateOf("Gulberg III") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val quickAreas = listOf(
        "Gulberg III",
        "Model Town",
        "Garden Town",
        "DHA Phase 5",
        "Johar Town",
        "Bahria Town"
    )
    val previewBranch = remember(areaInput, streetInput) {
        BranchDirectory.routeAddressToBranch("$areaInput $streetInput")
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: SAVED ADDRESSES + Red X close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SAVED ADDRESSES",
                        style = MaterialTheme.typography.headlineMedium,
                        color = LahoriWhite
                    )
                    Text(
                        text = "Auto-routed to your nearest Lahori Food Cafe branch",
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
                            contentDescription = "Close Saved Addresses",
                            tint = LahoriWhite
                        )
                    }
                }
            }

            if (savedAddresses.isNotEmpty()) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (showAddForm) 130.dp else 210.dp)
                ) {
                    items(savedAddresses, key = { it.id }) { addr ->
                        val isSelected = selectedAddressStreet.contains(addr.streetAddress)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = LahoriDarkCard,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) LahoriRed else Color.White.copy(alpha = 0.12f)
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
                                            color = LahoriWhite.copy(alpha = 0.7f)
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Storefront,
                                                contentDescription = null,
                                                tint = LahoriGold,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "${addr.branchId.uppercase()}: ${addr.branchName}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = LahoriGold
                                            )
                                        }
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

            AnimatedVisibility(visible = showAddForm) {
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "ENTER DELIVERY ZONE & STREET",
                            style = MaterialTheme.typography.labelLarge,
                            color = LahoriGold
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            quickAreas.forEach { zone ->
                                FilterChip(
                                    selected = areaInput.equals(zone, ignoreCase = true),
                                    onClick = { areaInput = zone },
                                    label = { Text(zone, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LahoriRed,
                                        selectedLabelColor = LahoriWhite,
                                        containerColor = LahoriDarkSheet,
                                        labelColor = LahoriWhite.copy(alpha = 0.85f)
                                    )
                                )
                            }
                        }

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
                                label = { Text("Area / Zone") },
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
                            label = { Text("House / Street / Block") },
                            placeholder = { Text("e.g. House 14-B, Block K") },
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

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Storefront,
                                contentDescription = null,
                                tint = LahoriGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Auto-assigned closest branch: ${previewBranch.id} (${previewBranch.name})",
                                style = MaterialTheme.typography.bodySmall,
                                color = LahoriGold
                            )
                        }

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
                            Text("SAVE & ROUTE TO ${previewBranch.id.uppercase()}", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            // Bottom Action Buttons: ADD NEW ADDRESS & START YOUR ORDER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                        text = if (showAddForm) "HIDE FORM" else "ADD NEW ADDRESS",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                        maxLines = 1
                    )
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
                        text = "START YOUR ORDER",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
