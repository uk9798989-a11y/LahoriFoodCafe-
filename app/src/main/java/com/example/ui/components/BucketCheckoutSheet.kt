package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BranchDirectory
import com.example.model.CafeBranch
import com.example.model.MenuItem
import com.example.model.OrderType
import com.example.model.Promotion
import com.example.ui.SharedSessionState
import com.example.ui.screens.formatPromoExpiryLabel
import com.example.ui.theme.LahoriBlack
import com.example.ui.theme.LahoriDarkCard
import com.example.ui.theme.LahoriGold
import com.example.ui.theme.LahoriRed
import com.example.ui.theme.LahoriSuccess
import com.example.ui.theme.LahoriWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BucketCheckoutSheet(
    sessionState: SharedSessionState,
    isSignedIn: Boolean,
    defaultCustomerName: String,
    isSubmitting: Boolean,
    onAddItem: (MenuItem) -> Unit,
    onRemoveItem: (String) -> Unit,
    onApplyPromoCode: (code: String, onResult: (success: Boolean, message: String) -> Unit) -> Unit,
    onClearPromoCode: () -> Unit,
    onPlaceOrder: (
        name: String,
        phone: String,
        deliveryAddress: String,
        deliveryLatitude: Double,
        deliveryLongitude: Double,
        guestCheckout: Boolean
    ) -> Unit,
    onDismiss: () -> Unit,
    branches: List<CafeBranch> = BranchDirectory.defaultBranches,
    branchPromos: List<Promotion> = emptyList(),
    isLocatingGps: Boolean = false,
    onRequestLiveLocation: () -> Unit = {},
    onSaveAddressFromCheckout: (fullAddress: String, landmark: String) -> Unit = { _, _ -> }
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var customerName by remember(defaultCustomerName) {
        mutableStateOf(defaultCustomerName.ifBlank { "Lahori Guest" })
    }
    var customerPhone by remember { mutableStateOf("0300-1234567") }
    var deliveryAddressInput by remember {
        mutableStateOf(
            sessionState.selectedAddressStreet.ifBlank { sessionState.detectedGpsAddress }
        )
    }
    var landmarkInput by remember {
        mutableStateOf(sessionState.selectedLandmark)
    }
    var addressSavedBanner by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(sessionState.selectedAddressStreet, sessionState.selectedLandmark) {
        if (sessionState.selectedAddressStreet.isNotBlank() && deliveryAddressInput.isBlank()) {
            deliveryAddressInput = sessionState.selectedAddressStreet
        }
        if (sessionState.selectedLandmark.isNotBlank() && landmarkInput.isBlank()) {
            landmarkInput = sessionState.selectedLandmark
        }
    }
    LaunchedEffect(sessionState.detectedGpsAddress) {
        if (sessionState.detectedGpsAddress.isNotBlank() && deliveryAddressInput.isBlank()) {
            deliveryAddressInput = sessionState.detectedGpsAddress
        }
    }
    var guestCheckoutToggle by remember(isSignedIn) { mutableStateOf(!isSignedIn) }
    var promoInput by remember { mutableStateOf(sessionState.appliedPromoLabel) }
    var promoStatusMsg by remember { mutableStateOf<String?>(null) }
    var promoIsSuccess by remember { mutableStateOf(false) }
    var isValidatingPromo by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    // FREE DELIVERY permanently: Rs. 0 for every customer and every order
    val deliveryFee = 0L
    val grandTotal = (sessionState.subtotalPkr - sessionState.appliedDiscountPkr).coerceAtLeast(0L)
    val branch = BranchDirectory.byId(sessionState.selectedBranchId, branches)
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val successText = if (isDark) Color(0xFF4ADE80) else LahoriSuccess

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.LocalMall, contentDescription = null, tint = LahoriRed)
                    Text(
                        text = "YOUR BUCKET",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close bucket", tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            if (sessionState.cartItems.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalMall,
                            contentDescription = null,
                            tint = LahoriRed.copy(alpha = 0.7f),
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "Your Lahori Bucket is empty",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap ADD TO BUCKET on any Zinger, Pizza, Wings, or Sandwich to start your feast.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // FREE DELIVERY Banner (Lahori Food Cafe Black + Gold + Red branding)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LahoriBlack,
                    border = BorderStroke(1.2.dp, LahoriGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("checkout_free_delivery_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalOffer,
                                contentDescription = null,
                                tint = LahoriGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "FREE DELIVERY — Rs. 0",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                    color = LahoriGold
                                )
                                Text(
                                    text = "Zero delivery charges on every Lahori Food Cafe order!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LahoriWhite.copy(alpha = 0.85f)
                                )
                            }
                        }
                        Surface(
                            color = LahoriRed,
                            shape = CircleShape
                        ) {
                            Text(
                                text = "FREE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = LahoriWhite,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Branch & Fulfillment chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.Storefront, contentDescription = null, tint = LahoriRed)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${sessionState.orderType.label} • ${branch.name}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (sessionState.orderType == OrderType.DELIVERY) {
                                    deliveryAddressInput.ifBlank {
                                        "Enter your complete delivery address below"
                                    }
                                } else {
                                    branch.shortAddress
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Manual-First Delivery Address Card (Default, No GPS Required)
                if (sessionState.orderType == OrderType.DELIVERY) {
                    val hasGps = sessionState.hasGpsFix
                    val lat = sessionState.deviceLatitude ?: 0.0
                    val lng = sessionState.deviceLongitude ?: 0.0
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = LahoriBlack,
                        border = BorderStroke(1.5.dp, LahoriGold.copy(alpha = 0.75f)),
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
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = null,
                                        tint = LahoriGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "DELIVERY ADDRESS (MANUAL)",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                        color = LahoriGold
                                    )
                                }
                                Text(
                                    text = "No GPS needed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LahoriWhite.copy(alpha = 0.65f)
                                )
                            }

                            OutlinedTextField(
                                value = deliveryAddressInput,
                                onValueChange = {
                                    deliveryAddressInput = it
                                    errorText = null
                                    addressSavedBanner = null
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
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LahoriGold,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.28f),
                                    focusedContainerColor = LahoriDarkCard,
                                    unfocusedContainerColor = LahoriDarkCard,
                                    focusedTextColor = LahoriWhite,
                                    unfocusedTextColor = LahoriWhite,
                                    focusedLabelColor = LahoriGold,
                                    unfocusedLabelColor = LahoriWhite.copy(alpha = 0.72f),
                                    cursorColor = LahoriGold
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("checkout_delivery_address_input")
                            )

                            OutlinedTextField(
                                value = landmarkInput,
                                onValueChange = {
                                    landmarkInput = it
                                    addressSavedBanner = null
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
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LahoriGold,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.24f),
                                    focusedContainerColor = LahoriDarkCard,
                                    unfocusedContainerColor = LahoriDarkCard,
                                    focusedTextColor = LahoriWhite,
                                    unfocusedTextColor = LahoriWhite,
                                    focusedLabelColor = LahoriGold,
                                    unfocusedLabelColor = LahoriWhite.copy(alpha = 0.68f),
                                    cursorColor = LahoriGold
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("checkout_landmark_input")
                            )

                            if (addressSavedBanner != null) {
                                Row(
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
                                        text = addressSavedBanner!!,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color(0xFF4ADE80)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val clean = deliveryAddressInput.trim()
                                        if (clean.isBlank()) {
                                            errorText = "Please enter your complete delivery address."
                                        } else {
                                            errorText = null
                                            onSaveAddressFromCheckout(clean, landmarkInput.trim())
                                            addressSavedBanner = "Address saved for future orders!"
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LahoriGold,
                                        contentColor = LahoriBlack
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("checkout_save_address_btn")
                                ) {
                                    Text(
                                        text = "SAVE ADDRESS",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black)
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        errorText = null
                                        onRequestLiveLocation()
                                    },
                                    enabled = !isLocatingGps && !isSubmitting,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (hasGps) Color(0xFF4ADE80) else LahoriWhite.copy(alpha = 0.4f)
                                    ),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = if (hasGps) Color(0xFF4ADE80) else LahoriWhite
                                    ),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("checkout_send_location_btn")
                                ) {
                                    if (isLocatingGps) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = LahoriGold,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.MyLocation,
                                            contentDescription = "Optional GPS Pin",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = if (hasGps) "GPS PIN ✓" else "GPS (OPT)",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }

                            if (hasGps) {
                                Text(
                                    text = "Optional GPS Pin attached: ${String.format(java.util.Locale.US, "%.5f, %.5f", lat, lng)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF4ADE80)
                                )
                            }
                        }
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 70.dp, max = 165.dp)
                ) {
                    items(sessionState.cartItems, key = { it.menuItem.id }) { cart ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cart.menuItem.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${formatPkr(cart.menuItem.pricePkr)} each",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onRemoveItem(cart.menuItem.id) },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (cart.quantity == 1) Icons.Filled.DeleteOutline else Icons.Filled.Remove,
                                            contentDescription = "Decrease quantity",
                                            tint = LahoriRed
                                        )
                                    }
                                    Text(
                                        text = "${cart.quantity}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )
                                    IconButton(
                                        onClick = { onAddItem(cart.menuItem) },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = "Increase quantity",
                                            tint = LahoriRed
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = formatPkr(cart.lineTotalPkr),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Promo Codes Input Field (validated against Firestore 'promotions' collection)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        1.dp,
                        if (sessionState.appliedDiscountPkr > 0L) successText else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.LocalOffer, contentDescription = null, tint = LahoriRed, modifier = Modifier.size(18.dp))
                                Text("PROMO CODES", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                            }
                            if (sessionState.appliedPromoLabel.isNotBlank()) {
                                TextButton(
                                    onClick = {
                                        onClearPromoCode()
                                        promoInput = ""
                                        promoStatusMsg = null
                                    }
                                ) {
                                    Text("REMOVE", style = MaterialTheme.typography.labelSmall, color = LahoriRed)
                                }
                            }
                        }

                        if (branchPromos.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "${branch.displayShortLabel()} Exclusive Offers — tap to apply:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = LahoriRed
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(branchPromos, key = { it.id }) { promo ->
                                        val isApplied = sessionState.appliedPromoLabel.equals(promo.code, ignoreCase = true)
                                        val discLabel = if (promo.discountType.equals("PERCENT", ignoreCase = true)) {
                                            "${promo.discountValue}% OFF"
                                        } else {
                                            "Rs. ${promo.discountValue} OFF"
                                        }
                                        val expiryShort = formatPromoExpiryLabel(promo.expiresAt)
                                        Surface(
                                            onClick = {
                                                promoInput = promo.code
                                                isValidatingPromo = true
                                                onApplyPromoCode(promo.code) { success, message ->
                                                    isValidatingPromo = false
                                                    promoIsSuccess = success
                                                    promoStatusMsg = message
                                                }
                                            },
                                            shape = CircleShape,
                                            color = if (isApplied) successText else LahoriBlack,
                                            border = BorderStroke(
                                                1.dp,
                                                if (isApplied) successText else LahoriGold
                                            ),
                                            modifier = Modifier.testTag("checkout_branch_promo_${promo.code}")
                                        ) {
                                            Text(
                                                text = if (isApplied) {
                                                    "${promo.code} ✓"
                                                } else {
                                                    "${promo.code} • $discLabel ($expiryShort)"
                                                },
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (isApplied) LahoriWhite else LahoriGold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = promoInput,
                                onValueChange = {
                                    promoInput = it.uppercase()
                                    promoStatusMsg = null
                                },
                                placeholder = {
                                    Text(
                                        text = "Try LAHORI20 or ZINGER150",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LahoriRed,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("promo_code_input")
                            )

                            OutlinedButton(
                                onClick = {
                                    isValidatingPromo = true
                                    onApplyPromoCode(promoInput) { success, message ->
                                        isValidatingPromo = false
                                        promoIsSuccess = success
                                        promoStatusMsg = message
                                    }
                                },
                                enabled = !isValidatingPromo && promoInput.isNotBlank(),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.5.dp, LahoriRed),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (isDark) MaterialTheme.colorScheme.onSurface else LahoriRed
                                ),
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("apply_promo_button")
                            ) {
                                if (isValidatingPromo) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = LahoriRed, strokeWidth = 2.dp)
                                } else {
                                    Text("APPLY", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }

                        if (promoStatusMsg != null) {
                            Text(
                                text = promoStatusMsg!!,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (promoIsSuccess) successText else LahoriRed
                            )
                        }
                    }
                }

                // Guest Checkout Mode card (temporary guestSessionId)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (guestCheckoutToggle) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (guestCheckoutToggle) LahoriRed else MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.PersonOutline, contentDescription = null, tint = LahoriRed)
                                Column {
                                    Text(
                                        text = "Guest Checkout",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (guestCheckoutToggle) {
                                            "Temp ID: ${sessionState.guestSessionId}"
                                        } else {
                                            "Order linked to your Google account"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = guestCheckoutToggle,
                                onCheckedChange = { guestCheckoutToggle = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = LahoriWhite,
                                    checkedTrackColor = LahoriRed
                                ),
                                modifier = Modifier.testTag("guest_checkout_toggle")
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = customerName,
                                onValueChange = {
                                    customerName = it
                                    errorText = null
                                },
                                label = { Text("Recipient Name") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LahoriRed,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = customerPhone,
                                onValueChange = {
                                    customerPhone = it
                                    errorText = null
                                },
                                label = { Text("Phone") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LahoriRed,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (!isSignedIn) {
                            Text(
                                text = "No account setup needed — 1-tap Google verification records your temporary Guest Order (${sessionState.guestSessionId}) securely in Cloud Firestore.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Totals Breakdown (FREE DELIVERY Rs. 0 permanently)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LahoriBlack,
                    border = BorderStroke(1.dp, LahoriGold.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal", style = MaterialTheme.typography.bodyMedium, color = LahoriWhite.copy(alpha = 0.78f))
                            Text(formatPkr(sessionState.subtotalPkr), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = LahoriWhite)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Delivery Charges", style = MaterialTheme.typography.bodyMedium, color = LahoriWhite.copy(alpha = 0.78f))
                            Text(
                                text = "Rs. 0 (FREE)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                color = LahoriGold,
                                modifier = Modifier.testTag("checkout_delivery_charges_value")
                            )
                        }
                        if (sessionState.appliedDiscountPkr > 0L) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Promo (${sessionState.appliedPromoLabel})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF4ADE80)
                                )
                                Text(
                                    text = "- ${formatPkr(sessionState.appliedDiscountPkr)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF4ADE80)
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = LahoriWhite.copy(alpha = 0.16f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = LahoriWhite)
                            Text(
                                text = formatPkr(grandTotal),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                color = LahoriGold
                            )
                        }
                    }
                }

                if (errorText != null) {
                    Text(errorText!!, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = LahoriRed)
                }

                Button(
                    onClick = {
                        val trimmedName = customerName.trim()
                        val trimmedPhone = customerPhone.trim()
                        val trimmedAddr = deliveryAddressInput.trim()
                        val trimmedLandmark = landmarkInput.trim()
                        val isDelivery = sessionState.orderType == OrderType.DELIVERY
                        val gpsLat = if (isDelivery) (sessionState.deviceLatitude ?: 0.0) else 0.0
                        val gpsLng = if (isDelivery) (sessionState.deviceLongitude ?: 0.0) else 0.0
                        if (trimmedName.isEmpty()) {
                            errorText = "Please enter recipient name."
                        } else if (isDelivery && trimmedAddr.isEmpty()) {
                            errorText = "Please enter your complete delivery address before placing your order."
                        } else {
                            if (isDelivery) {
                                // Persist customer's manual address automatically for future orders
                                onSaveAddressFromCheckout(trimmedAddr, trimmedLandmark)
                            }
                            val finalAddr = if (isDelivery) {
                                if (trimmedLandmark.isNotBlank() && !trimmedAddr.contains(trimmedLandmark, ignoreCase = true)) {
                                    "$trimmedAddr (Near: $trimmedLandmark)"
                                } else {
                                    trimmedAddr
                                }
                            } else {
                                "Self Pickup — ${branch.shortAddress}"
                            }
                            onPlaceOrder(
                                trimmedName,
                                trimmedPhone,
                                finalAddr,
                                gpsLat,
                                gpsLng,
                                guestCheckoutToggle
                            )
                        }
                    },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LahoriRed, contentColor = LahoriWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("place_order_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = LahoriWhite, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = if (!isSignedIn) "VERIFY & PLACE GUEST ORDER • ${formatPkr(grandTotal)}"
                            else if (guestCheckoutToggle) "PLACE GUEST ORDER • ${formatPkr(grandTotal)}"
                            else "PLACE ORDER • ${formatPkr(grandTotal)}",
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp)
                        )
                    }
                }
            }
        }
    }
}
