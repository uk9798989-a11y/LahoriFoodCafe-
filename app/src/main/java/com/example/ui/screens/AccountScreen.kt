package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BranchAdminEntry
import com.example.model.BranchDirectory
import com.example.model.CafeFeedback
import com.example.model.CafeOrder
import com.example.model.OrderReview
import com.example.model.SUPER_ADMIN_EMAIL
import com.example.model.UiState
import com.example.model.UserRole
import com.example.ui.components.AccountFeedbackCard
import com.example.ui.theme.LahoriBlack
import com.example.ui.theme.LahoriGold
import com.example.ui.theme.LahoriRed
import com.example.ui.theme.LahoriSuccess
import com.example.ui.theme.LahoriWhite

@Composable
fun AccountScreen(
    isSignedIn: Boolean,
    displayName: String,
    email: String,
    effectiveRole: UserRole,
    branchAdminAssignment: BranchAdminEntry?,
    ordersState: UiState<List<CafeOrder>>,
    notificationsEnabled: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onOpenSuperAdminDashboard: () -> Unit,
    onOpenBranchAdminDashboard: () -> Unit,
    reviewsByOrderId: Map<String, OrderReview> = emptyMap(),
    onRateOrder: ((CafeOrder) -> Unit)? = null,
    isDarkMode: Boolean = false,
    onToggleDarkMode: (Boolean) -> Unit = {},
    fcmTokenRegistered: Boolean = false,
    defaultFeedbackBranchId: String = BranchDirectory.branch1.id,
    recentFeedback: List<CafeFeedback> = emptyList(),
    isSubmittingFeedback: Boolean = false,
    onSubmitCafeFeedback: (
        branchId: String,
        foodRating: Int,
        serviceRating: Int,
        comment: String,
        onComplete: () -> Unit
    ) -> Unit = { _, _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var selectedHistoryOrder by remember { mutableStateOf<CafeOrder?>(null) }

    // Strict Hidden Super Admin UI check:
    // ONLY visible if logged-in email is exactly 'uk9798989@gmail.com'
    val canSeeSuperAdminButton = isSignedIn && email.equals(SUPER_ADMIN_EMAIL, ignoreCase = true)
    val canSeeBranchAdminButton = isSignedIn &&
        (effectiveRole == UserRole.BranchAdmin || branchAdminAssignment != null)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. User Profile Header
        item(key = "profile_card") {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = LahoriRed,
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "User avatar",
                                tint = LahoriWhite,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSignedIn) displayName.ifBlank { "Lahori Customer" } else "Guest Mode",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isSignedIn) email else "Browse & order without signing up",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isSignedIn) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = LahoriBlack,
                                border = BorderStroke(1.dp, LahoriGold.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = effectiveRole.name.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LahoriGold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    if (!isSignedIn) {
                        Button(
                            onClick = onSignInClick,
                            colors = ButtonDefaults.buttonColors(containerColor = LahoriRed, contentColor = LahoriWhite),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("SIGN IN", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        // 1b. Appearance / High-Contrast Dark Mode Toggle Card
        item(key = "theme_toggle_card") {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    if (isDarkMode) LahoriRed.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleDarkMode(!isDarkMode) }
                    .testTag("dark_mode_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDarkMode) LahoriRed else MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isDarkMode) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                    contentDescription = if (isDarkMode) "Dark Mode active" else "Light Mode active",
                                    tint = if (isDarkMode) LahoriWhite else LahoriRed,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Dark Mode",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isDarkMode) {
                                    "High-contrast obsidian dark theme active"
                                } else {
                                    "Switch from KFC-inspired light theme to dark mode"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = onToggleDarkMode,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = LahoriWhite,
                            checkedTrackColor = LahoriRed,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.testTag("dark_mode_switch")
                    )
                }
            }
        }

        // 2. Notification Status Card
        item(key = "notification_card") {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
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
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = if (notificationsEnabled) LahoriSuccess else LahoriRed
                        )
                        Column {
                            Text(
                                text = "Real-Time Push Order Alerts",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when {
                                    notificationsEnabled && fcmTokenRegistered ->
                                        "FCM push active for Preparing → Ready for Pickup & Delivery"
                                    notificationsEnabled ->
                                        "Enabled for Confirmed, Preparing, Ready for Pickup & Delivered"
                                    else ->
                                        "Enable push alerts for live Ready for Pickup & Delivery updates"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (!notificationsEnabled) {
                        OutlinedButton(
                            onClick = onRequestNotificationPermission,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, LahoriRed),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LahoriRed)
                        ) {
                            Text("ENABLE", style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp))
                        }
                    }
                }
            }
        }

        // 2b. Food & Service Feedback Form
        item(key = "feedback_form_card") {
            AccountFeedbackCard(
                isSignedIn = isSignedIn,
                defaultBranchId = defaultFeedbackBranchId,
                isSubmitting = isSubmittingFeedback,
                recentFeedback = recentFeedback,
                onSignInClick = onSignInClick,
                onSubmitFeedback = onSubmitCafeFeedback
            )
        }

        // 3. Hidden Super Admin Dashboard Button (100% invisible for non-SuperAdmin users!)
        if (canSeeSuperAdminButton) {
            item(key = "super_admin_entry") {
                Button(
                    onClick = onOpenSuperAdminDashboard,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, LahoriGold.copy(alpha = 0.65f)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LahoriBlack,
                        contentColor = LahoriWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("cafe_owner_admin_dashboard_btn")
                ) {
                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = LahoriGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cafe Owner Admin Dashboard",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // 4. Branch Manager Dashboard Button (ONLY for assigned BranchAdmin)
        if (canSeeBranchAdminButton) {
            item(key = "branch_admin_entry") {
                Button(
                    onClick = onOpenBranchAdminDashboard,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LahoriRed,
                        contentColor = LahoriWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("branch_manager_dashboard_btn")
                ) {
                    Icon(Icons.Filled.Storefront, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Branch Manager Dashboard (${branchAdminAssignment?.branchId ?: "Branch"})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // 5. ORDER HISTORY SECTION (Tappable cards for full order details)
        item(key = "order_history_header") {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.History, contentDescription = null, tint = LahoriRed)
                Column {
                    Text(
                        text = "ORDER HISTORY",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Tap any previous order to inspect full item & branch details",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (!isSignedIn) {
            item(key = "guest_history_prompt") {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Sign in with Google to view your saved Order History across devices.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            when (ordersState) {
                is UiState.Loading -> {
                    item(key = "history_loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = LahoriRed)
                        }
                    }
                }
                is UiState.Error -> {
                    item(key = "history_error") {
                        Text(
                            text = ordersState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = LahoriRed
                        )
                    }
                }
                is UiState.Success -> {
                    val history = ordersState.data
                    if (history.isEmpty()) {
                        item(key = "history_empty") {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No previous orders yet. Your completed and active orders will appear here.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(18.dp)
                                )
                            }
                        }
                    } else {
                        items(history, key = { "history_${it.id}" }) { order ->
                            OrderSummaryCard(
                                order = order,
                                showStepper = false,
                                existingReview = reviewsByOrderId[order.id],
                                onRateOrder = onRateOrder?.let { cb -> { cb(order) } },
                                onClick = { selectedHistoryOrder = order }
                            )
                        }
                    }
                }
            }
        }

        if (isSignedIn) {
            item(key = "sign_out_btn") {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onSignOutClick,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LahoriRed),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LahoriRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SIGN OUT", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    selectedHistoryOrder?.let { order ->
        OrderDetailsBottomSheet(
            order = order,
            existingReview = reviewsByOrderId[order.id],
            onRateOrder = onRateOrder?.let { cb ->
                {
                    selectedHistoryOrder = null
                    cb(order)
                }
            },
            onDismiss = { selectedHistoryOrder = null }
        )
    }
}
