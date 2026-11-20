package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LocationHelper
import com.example.data.MenuImageStorageHelper
import com.example.model.AdminDeliveryNotification
import com.example.model.BranchAdminEntry
import com.example.model.BranchBanner
import com.example.model.BranchDirectory
import com.example.model.CafeBranch
import com.example.model.CafeFeedback
import com.example.model.CafeOrder
import com.example.model.MenuItem
import com.example.model.OrderReview
import com.example.model.OrderStatus
import com.example.model.OrderType
import com.example.model.Promotion
import com.example.model.SUPER_ADMIN_EMAIL
import com.example.model.SeedMenuData
import com.example.model.UiState
import com.google.firebase.Timestamp
import com.example.model.UserRole
import com.example.ui.components.FoodArtworkBox
import com.example.ui.components.formatPkr
import com.example.ui.theme.LahoriBlack
import com.example.ui.theme.LahoriGold
import com.example.ui.theme.LahoriRed
import com.example.ui.theme.LahoriSuccess
import com.example.ui.theme.LahoriWarning
import com.example.ui.theme.LahoriWhite

@Composable
fun SuperAdminDashboardScreen(
    allOrdersState: UiState<List<CafeOrder>>,
    menuItems: List<MenuItem>,
    branchAdmins: List<BranchAdminEntry>,
    promotions: List<Promotion>,
    onUpdateOrderStatus: (orderId: String, newStatus: OrderStatus) -> Unit,
    onSaveMenuItem: (
        itemId: String?,
        name: String,
        description: String,
        category: String,
        pricePkr: Long,
        imageKey: String,
        imageUrl: String,
        pickedImageUri: Uri?,
        badgeText: String,
        available: Boolean,
        sortOrder: Long,
        onError: (String) -> Unit,
        onDone: () -> Unit
    ) -> Unit,
    onDeleteMenuItem: (String) -> Unit,
    onSeedMenu: () -> Unit,
    onAddBranchAdmin: (email: String, role: String, branchId: String, onDone: () -> Unit) -> Unit,
    onRemoveBranchAdmin: (String) -> Unit,
    onSavePromotion: (
        code: String,
        discountType: String,
        discountValue: Long,
        minOrderPkr: Long,
        active: Boolean,
        onDone: () -> Unit
    ) -> Unit,
    onDeletePromotion: (String) -> Unit,
    onBack: () -> Unit,
    reviews: List<OrderReview> = emptyList(),
    feedbackList: List<CafeFeedback> = emptyList(),
    onDeleteReview: (String) -> Unit = {},
    onDeleteFeedback: (String) -> Unit = {},
    isSavingStaff: Boolean = false,
    isSavingMenuItem: Boolean = false,
    branches: List<CafeBranch> = BranchDirectory.defaultBranches,
    isSavingBranch: Boolean = false,
    onSaveBranch: (
        branchId: String?,
        name: String,
        shortLabel: String,
        shortAddress: String,
        areas: List<String>,
        active: Boolean,
        sortOrder: Long?,
        latitude: Double,
        longitude: Double,
        mapUrl: String,
        openHours: String,
        onDone: (String) -> Unit
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onDeleteBranch: (String) -> Unit = {},
    onSeedBranches: () -> Unit = {},
    newOrderAlert: CafeOrder? = null,
    onDismissNewOrderAlert: () -> Unit = {},
    notificationsEnabled: Boolean = true,
    liveAlertsActive: Boolean = false,
    onRequestEnableAlerts: () -> Unit = {},
    adminNotifications: List<AdminDeliveryNotification> = emptyList(),
    unreadNotificationCount: Int = 0,
    deliveredPopupQueue: List<AdminDeliveryNotification> = emptyList(),
    soundEnabled: Boolean = true,
    onToggleSound: (Boolean) -> Unit = {},
    onMarkNotificationRead: (String) -> Unit = {},
    onMarkAllNotificationsRead: () -> Unit = {},
    onDismissDeliveredPopup: () -> Unit = {}
) {
    BackHandler(onBack = onBack)

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var branchFilter by remember { mutableStateOf("ALL") }
    var editingMenuItem by remember { mutableStateOf<MenuItem?>(null) }
    var showAddMenuDialog by remember { mutableStateOf(false) }
    var editingPromotion by remember { mutableStateOf<Promotion?>(null) }
    var showPromoDialog by remember { mutableStateOf(false) }
    var inspectedOrderDetail by remember { mutableStateOf<Pair<CafeOrder?, AdminDeliveryNotification?>?>(null) }

    val allOrdersList = (allOrdersState as? UiState.Success)?.data ?: emptyList()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(color = LahoriBlack, contentColor = LahoriWhite) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LahoriWhite)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SUPER ADMIN CONTROL", style = MaterialTheme.typography.titleLarge, color = LahoriGold)
                            Text(
                                text = "${branches.size} Branches • Live Menu • Staff RBAC",
                                style = MaterialTheme.typography.bodySmall,
                                color = LahoriWhite.copy(alpha = 0.75f)
                            )
                        }
                        AdminLiveAlertStatusPill(
                            notificationsEnabled = notificationsEnabled,
                            liveAlertsActive = liveAlertsActive,
                            onClick = onRequestEnableAlerts
                        )
                    }

                    ScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = LahoriBlack,
                        contentColor = LahoriWhite,
                        edgePadding = 8.dp,
                        indicator = { tabPositions ->
                            if (selectedTabIndex < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                    color = LahoriRed
                                )
                            }
                        }
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("ORDERS", fontSize = 11.sp, maxLines = 1)
                                    if (unreadNotificationCount > 0) {
                                        Surface(color = LahoriRed, shape = CircleShape) {
                                            Text(
                                                text = "$unreadNotificationCount",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = LahoriWhite,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            icon = { Icon(Icons.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("admin_tab_orders")
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = { Text("BRANCHES", fontSize = 11.sp, maxLines = 1) },
                            icon = { Icon(Icons.Filled.Storefront, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("admin_tab_branches")
                        )
                        Tab(
                            selected = selectedTabIndex == 2,
                            onClick = { selectedTabIndex = 2 },
                            text = { Text("MENU", fontSize = 11.sp, maxLines = 1) },
                            icon = { Icon(Icons.Filled.RestaurantMenu, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = selectedTabIndex == 3,
                            onClick = { selectedTabIndex = 3 },
                            text = { Text("STAFF", fontSize = 11.sp, maxLines = 1) },
                            icon = { Icon(Icons.Filled.People, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("admin_tab_staff")
                        )
                        Tab(
                            selected = selectedTabIndex == 4,
                            onClick = { selectedTabIndex = 4 },
                            text = { Text("PROMOS", fontSize = 11.sp, maxLines = 1) },
                            icon = { Icon(Icons.Filled.LocalOffer, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = selectedTabIndex == 5,
                            onClick = { selectedTabIndex = 5 },
                            text = { Text("REVIEWS", fontSize = 11.sp, maxLines = 1) },
                            icon = { Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = selectedTabIndex == 6,
                            onClick = { selectedTabIndex = 6 },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("ALERTS", fontSize = 11.sp, maxLines = 1)
                                    if (unreadNotificationCount > 0) {
                                        Surface(color = LahoriGold, shape = CircleShape) {
                                            Text(
                                                text = "$unreadNotificationCount",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = LahoriBlack,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            icon = { Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("admin_tab_notifications")
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
        ) {
            when (selectedTabIndex) {
                0 -> {
                    when (allOrdersState) {
                        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = LahoriRed)
                        }
                        is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(allOrdersState.message, color = LahoriRed)
                        }
                        is UiState.Success -> {
                            val filteredOrders = remember(allOrdersState.data, branchFilter) {
                                if (branchFilter == "ALL") allOrdersState.data
                                else allOrdersState.data.filter { it.branchId == branchFilter }
                            }
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item(key = "super_new_order_alert") {
                                    IncomingOrderAlertBanner(
                                        incoming = newOrderAlert,
                                        onDismiss = onDismissNewOrderAlert
                                    )
                                }
                                item(key = "branch_filter_chips") {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        item(key = "filter_ALL") {
                                            FilterChip(
                                                selected = branchFilter == "ALL",
                                                onClick = { branchFilter = "ALL" },
                                                label = { Text("All Branches (${allOrdersState.data.size})", fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = LahoriRed,
                                                    selectedLabelColor = LahoriWhite
                                                )
                                            )
                                        }
                                        items(branches, key = { "filter_${it.id}" }) { branch ->
                                            val count = allOrdersState.data.count { it.branchId == branch.id }
                                            FilterChip(
                                                selected = branchFilter == branch.id,
                                                onClick = { branchFilter = branch.id },
                                                label = { Text("${branch.displayShortLabel()} ($count)", fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = LahoriRed,
                                                    selectedLabelColor = LahoriWhite
                                                )
                                            )
                                        }
                                    }
                                }
                                if (filteredOrders.isEmpty()) {
                                    item(key = "no_admin_orders") {
                                        Text(
                                            text = "No incoming orders yet for this filter.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    items(filteredOrders, key = { it.id }) { order ->
                                        AdminOrderControlCard(
                                            order = order,
                                            onChangeStatus = { status -> onUpdateOrderStatus(order.id, status) },
                                            onViewDetails = { inspectedOrderDetail = order to null }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    BranchManagementTab(
                        branches = branches,
                        branchAdmins = branchAdmins,
                        isSavingBranch = isSavingBranch,
                        onSaveBranch = onSaveBranch,
                        onDeleteBranch = onDeleteBranch,
                        onSeedBranches = onSeedBranches
                    )
                }

                2 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item(key = "menu_manager_actions") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showAddMenuDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = LahoriRed),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("admin_add_menu_item_btn")
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("ADD MENU ITEM", style = MaterialTheme.typography.labelLarge)
                                }

                                OutlinedButton(
                                    onClick = onSeedMenu,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text("SEED CLOUD MENU", style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onBackground)
                                }
                            }
                        }

                        items(menuItems, key = { it.id }) { item ->
                            val isCloudDoc = !item.id.startsWith("seed_")
                            Surface(
                                onClick = {
                                    editingMenuItem = if (isCloudDoc) item else item.copy(id = item.id.removePrefix("seed_"))
                                    showAddMenuDialog = true
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    FoodArtworkBox(
                                        imageKey = item.imageKey,
                                        imageUrl = item.imageUrl,
                                        contentDescription = item.name,
                                        compact = true,
                                        modifier = Modifier
                                            .size(62.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${item.category} • ${formatPkr(item.pricePkr)}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = LahoriRed,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = if (item.available) "In Stock" else "Sold Out",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (item.available) LahoriSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (item.imageUrl.isNotBlank()) {
                                                Surface(
                                                    color = LahoriGold.copy(alpha = 0.22f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "PHOTO",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.ExtraBold
                                                        ),
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Row {
                                        IconButton(
                                            onClick = {
                                                editingMenuItem = if (isCloudDoc) item else item.copy(id = item.id.removePrefix("seed_"))
                                                showAddMenuDialog = true
                                            },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("admin_edit_menu_item_${item.id}")
                                        ) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Edit menu item", tint = MaterialTheme.colorScheme.onSurface)
                                        }
                                        if (isCloudDoc) {
                                            IconButton(
                                                onClick = { onDeleteMenuItem(item.id) },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("admin_delete_menu_item_${item.id}")
                                            ) {
                                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete menu item", tint = LahoriRed)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    StaffManagementTab(
                        branches = branches,
                        branchAdmins = branchAdmins,
                        isSavingStaff = isSavingStaff,
                        onAddBranchAdmin = onAddBranchAdmin,
                        onRemoveBranchAdmin = onRemoveBranchAdmin
                    )
                }

                4 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item(key = "add_promo_btn") {
                            Button(
                                onClick = {
                                    editingPromotion = null
                                    showPromoDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LahoriRed),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("admin_add_promo_btn")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("CREATE PROMO CODE", style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        if (promotions.isEmpty()) {
                            item(key = "no_promos") {
                                Text(
                                    text = "No promo codes in Firestore yet. Tap CREATE PROMO CODE to add a percentage or flat discount.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(promotions, key = { it.id }) { promo ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
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
                                        Column(modifier = Modifier.weight(1f)) {
                                            val discountLabel = if (promo.discountType.equals("PERCENT", ignoreCase = true)) {
                                                "${promo.discountValue}% OFF"
                                            } else {
                                                "${formatPkr(promo.discountValue)} OFF"
                                            }
                                            val scopeLabel = if (promo.isGlobal()) {
                                                "ALL BRANCHES"
                                            } else {
                                                BranchDirectory.shortPickerLabel(promo.branchId, branches)
                                                    .ifBlank { promo.branchName.ifBlank { promo.branchId } }
                                                    .uppercase()
                                            }
                                            val expiryLabel = formatPromoExpiryLabel(promo.expiresAt)
                                            val isExpired = promo.isExpired()
                                            Text(
                                                text = "${promo.code} • $discountLabel",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "$scopeLabel • Min ${formatPkr(promo.minOrderPkr)} • ${if (isExpired) "Expired" else if (promo.active) "Active" else "Paused"} ($expiryLabel)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = when {
                                                    isExpired -> LahoriRed
                                                    promo.active -> LahoriSuccess
                                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    editingPromotion = promo
                                                    showPromoDialog = true
                                                },
                                                modifier = Modifier.size(44.dp)
                                            ) {
                                                Icon(Icons.Filled.Edit, contentDescription = "Edit promo", tint = MaterialTheme.colorScheme.onSurface)
                                            }
                                            IconButton(
                                                onClick = { onDeletePromotion(promo.id) },
                                                modifier = Modifier.size(44.dp)
                                            ) {
                                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete promo", tint = LahoriRed)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                5 -> {
                    SuperAdminReviewsTab(
                        reviews = reviews,
                        feedbackList = feedbackList,
                        onDeleteReview = onDeleteReview,
                        onDeleteFeedback = onDeleteFeedback
                    )
                }

                6 -> {
                    AdminNotificationsHistoryTab(
                        notifications = adminNotifications,
                        soundEnabled = soundEnabled,
                        onToggleSound = onToggleSound,
                        onOpenNotification = { notif ->
                            if (!notif.read) onMarkNotificationRead(notif.id)
                            val matched = allOrdersList.firstOrNull { it.id == notif.orderId }
                            inspectedOrderDetail = matched to notif
                        },
                        onMarkAllRead = onMarkAllNotificationsRead
                    )
                }
            }
        }
    }

    // Instant Real-Time Order Delivered Popup (triggers on ANY Super Admin tab)
    val activePopup = deliveredPopupQueue.firstOrNull()
    if (activePopup != null) {
        OrderDeliveredPopupModal(
            notification = activePopup,
            remainingInQueue = (deliveredPopupQueue.size - 1).coerceAtLeast(0),
            onViewOrder = { notif ->
                if (!notif.read) onMarkNotificationRead(notif.id)
                onDismissDeliveredPopup()
                val matched = allOrdersList.firstOrNull { it.id == notif.orderId }
                inspectedOrderDetail = matched to notif
            },
            onDismiss = onDismissDeliveredPopup
        )
    }

    inspectedOrderDetail?.let { (order, fallbackNotif) ->
        AdminOrderDetailDialog(
            order = order,
            notificationFallback = fallbackNotif,
            onDismiss = { inspectedOrderDetail = null }
        )
    }

    if (showAddMenuDialog) {
        MenuEditorDialog(
            initialItem = editingMenuItem,
            isSaving = isSavingMenuItem,
            onDismiss = {
                if (!isSavingMenuItem) {
                    showAddMenuDialog = false
                    editingMenuItem = null
                }
            },
            onSave = { id, name, desc, cat, price, imgKey, imgUrl, pickedUri, badge, available, sortOrder, onError ->
                onSaveMenuItem(
                    id,
                    name,
                    desc,
                    cat,
                    price,
                    imgKey,
                    imgUrl,
                    pickedUri,
                    badge,
                    available,
                    sortOrder,
                    onError
                ) {
                    showAddMenuDialog = false
                    editingMenuItem = null
                }
            }
        )
    }

    if (showPromoDialog) {
        PromoEditorDialog(
            initialPromo = editingPromotion,
            onDismiss = {
                showPromoDialog = false
                editingPromotion = null
            },
            onSave = { code, discountType, discountValue, minOrderPkr, active ->
                onSavePromotion(code, discountType, discountValue, minOrderPkr, active) {
                    showPromoDialog = false
                    editingPromotion = null
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuperAdminReviewsTab(
    reviews: List<OrderReview>,
    feedbackList: List<CafeFeedback>,
    onDeleteReview: (String) -> Unit,
    onDeleteFeedback: (String) -> Unit
) {
    var filterMode by remember { mutableStateOf("ALL") }

    val avgOrderRating = remember(reviews) {
        if (reviews.isEmpty()) 0.0 else reviews.map { it.rating }.average()
    }
    val avgFoodServiceRating = remember(feedbackList) {
        if (feedbackList.isEmpty()) 0.0
        else feedbackList.map { (it.foodRating + it.serviceRating) / 2.0 }.average()
    }

    val showOrderReviews = filterMode == "ALL" || filterMode == "ORDERS"
    val showCafeFeedback = filterMode == "ALL" || filterMode == "FEEDBACK"

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "reviews_summary_card") {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = LahoriBlack,
                border = BorderStroke(1.dp, LahoriGold.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CUSTOMER SENTIMENT OVERVIEW",
                            style = MaterialTheme.typography.labelSmall,
                            color = LahoriGold
                        )
                        Text(
                            text = "${reviews.size} Order Reviews • ${feedbackList.size} Food & Service",
                            style = MaterialTheme.typography.titleSmall,
                            color = LahoriWhite
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (reviews.isEmpty()) "Orders: —" else String.format(java.util.Locale.US, "Orders: %.1f★", avgOrderRating),
                            style = MaterialTheme.typography.labelMedium,
                            color = LahoriGold
                        )
                        Text(
                            text = if (feedbackList.isEmpty()) "Cafe: —" else String.format(java.util.Locale.US, "Cafe: %.1f★", avgFoodServiceRating),
                            style = MaterialTheme.typography.labelSmall,
                            color = LahoriWhite.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        item(key = "reviews_filter_chips") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "ALL" to "All (${reviews.size + feedbackList.size})",
                    "ORDERS" to "Order Reviews (${reviews.size})",
                    "FEEDBACK" to "Food & Service (${feedbackList.size})"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = filterMode == key,
                        onClick = { filterMode = key },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LahoriRed,
                            selectedLabelColor = LahoriWhite
                        )
                    )
                }
            }
        }

        if (reviews.isEmpty() && feedbackList.isEmpty()) {
            item(key = "no_reviews_at_all") {
                Text(
                    text = "No customer order reviews or account feedback submitted yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (showOrderReviews && reviews.isNotEmpty()) {
            item(key = "order_reviews_heading") {
                Text(
                    text = "ORDER REVIEWS (${reviews.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(reviews, key = { "rev_${it.id}" }) { review ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${review.customerName.ifBlank { "Customer" }} • #${review.orderId.takeLast(6).uppercase()}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = LahoriGold.copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Filled.Star, contentDescription = null, tint = LahoriGold, modifier = Modifier.size(14.dp))
                                        Text("${review.rating}/5", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                IconButton(
                                    onClick = { onDeleteReview(review.id) },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("delete_review_${review.id}")
                                ) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete order review", tint = LahoriRed)
                                }
                            }
                        }
                        Text(
                            text = "${review.branchName} • ${review.itemsSummary}",
                            style = MaterialTheme.typography.bodySmall,
                            color = LahoriRed
                        )
                        if (review.comment.isNotBlank()) {
                            Text(
                                text = "\"${review.comment}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = formatOrderDate(review.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (showCafeFeedback && feedbackList.isNotEmpty()) {
            item(key = "cafe_feedback_heading") {
                Text(
                    text = "FOOD & SERVICE FEEDBACK (${feedbackList.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(feedbackList, key = { "fb_${it.id}" }) { entry ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.customerName.ifBlank { entry.customerEmail.ifBlank { "Lahori Customer" } },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = entry.branchName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LahoriRed
                                )
                            }
                            IconButton(
                                onClick = { onDeleteFeedback(entry.id) },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("delete_feedback_${entry.id}")
                            ) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete feedback", tint = LahoriRed)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(color = LahoriGold.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = "Food ${entry.foodRating}/5 ★",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Surface(color = LahoriRed.copy(alpha = 0.14f), shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = "Service ${entry.serviceRating}/5 ★",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LahoriRed,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        if (entry.comment.isNotBlank()) {
                            Text(
                                text = "\"${entry.comment}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = formatOrderDate(entry.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BranchManagementTab(
    branches: List<CafeBranch>,
    branchAdmins: List<BranchAdminEntry>,
    isSavingBranch: Boolean,
    onSaveBranch: (
        branchId: String?,
        name: String,
        shortLabel: String,
        shortAddress: String,
        areas: List<String>,
        active: Boolean,
        sortOrder: Long?,
        latitude: Double,
        longitude: Double,
        mapUrl: String,
        openHours: String,
        onDone: (String) -> Unit
    ) -> Unit,
    onDeleteBranch: (String) -> Unit,
    onSeedBranches: () -> Unit
) {
    var editingBranchId by remember { mutableStateOf<String?>(null) }
    var branchName by remember { mutableStateOf("") }
    var shortLabel by remember { mutableStateOf("") }
    var shortAddress by remember { mutableStateOf("") }
    var areasInput by remember { mutableStateOf("") }
    var openHoursInput by remember { mutableStateOf("12:00 PM - 2:00 AM") }
    var mapUrlInput by remember { mutableStateOf("") }
    var latInput by remember { mutableStateOf("") }
    var lngInput by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(true) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var savedBanner by remember { mutableStateOf<String?>(null) }
    var confirmDeleteBranch by remember { mutableStateOf<CafeBranch?>(null) }

    fun resetForm() {
        editingBranchId = null
        branchName = ""
        shortLabel = ""
        shortAddress = ""
        areasInput = ""
        openHoursInput = "12:00 PM - 2:00 AM"
        mapUrlInput = ""
        latInput = ""
        lngInput = ""
        active = true
        validationError = null
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "branch_form_card") {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = LahoriRed.copy(alpha = 0.12f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddBusiness,
                                contentDescription = null,
                                tint = LahoriRed,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (editingBranchId == null) "CREATE NEW BRANCH" else "EDIT BRANCH (${editingBranchId!!.uppercase()})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Add unlimited branches (Branch 3, Branch 4, 10+ branches). Active branches immediately appear in Staff RBAC & Customer checkout.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = branchName,
                        onValueChange = { branchName = it; validationError = null; savedBanner = null },
                        label = { Text("Branch Full Name") },
                        placeholder = { Text("e.g. Lahori Food Cafe — Johar Town") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("branch_name_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = shortLabel,
                            onValueChange = { shortLabel = it; validationError = null; savedBanner = null },
                            label = { Text("Short Label") },
                            placeholder = { Text("Branch ${branches.size + 1} (Johar)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("branch_short_label_input")
                        )
                    }

                    OutlinedTextField(
                        value = shortAddress,
                        onValueChange = { shortAddress = it; validationError = null; savedBanner = null },
                        label = { Text("Branch Address") },
                        placeholder = { Text("e.g. Plot 14, Block G-1 Market, Johar Town") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("branch_address_input")
                    )

                    OutlinedTextField(
                        value = areasInput,
                        onValueChange = { areasInput = it; validationError = null; savedBanner = null },
                        label = { Text("Delivery Zones (comma-separated)") },
                        placeholder = { Text("e.g. Taranda Muhammad Panah, TMP, Main KLP Road") },
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("branch_areas_input")
                    )

                    OutlinedTextField(
                        value = openHoursInput,
                        onValueChange = { openHoursInput = it; validationError = null; savedBanner = null },
                        label = { Text("Operating Hours") },
                        placeholder = { Text("e.g. 12:00 PM - 2:00 AM") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = mapUrlInput,
                        onValueChange = { mapUrlInput = it; validationError = null; savedBanner = null },
                        label = { Text("Google Maps Share Link (optional)") },
                        placeholder = { Text("https://share.google/...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = latInput,
                            onValueChange = { latInput = it; validationError = null; savedBanner = null },
                            label = { Text("Latitude") },
                            placeholder = { Text("29.1003") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lngInput,
                            onValueChange = { lngInput = it; validationError = null; savedBanner = null },
                            label = { Text("Longitude") },
                            placeholder = { Text("70.9080") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Branch Open for Orders", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = if (active) "Visible to customers & active managers" else "Paused — hidden from new customer orders",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = active,
                            onCheckedChange = { active = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = LahoriRed),
                            modifier = Modifier.testTag("branch_active_switch")
                        )
                    }

                    if (validationError != null) {
                        Text(validationError!!, style = MaterialTheme.typography.bodySmall, color = LahoriRed)
                    }

                    if (savedBanner != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = LahoriSuccess.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, LahoriSuccess.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = LahoriSuccess, modifier = Modifier.size(18.dp))
                                Text(savedBanner!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (editingBranchId != null) {
                            OutlinedButton(
                                onClick = { resetForm() },
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("CANCEL", style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        Button(
                            onClick = {
                                val cleanName = branchName.trim()
                                val cleanLabel = shortLabel.trim().ifBlank { cleanName.substringAfter("— ").trim() }
                                val cleanAddress = shortAddress.trim()
                                val parsedAreas = areasInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                val parsedLat = latInput.trim().toDoubleOrNull() ?: 0.0
                                val parsedLng = lngInput.trim().toDoubleOrNull() ?: 0.0
                                val cleanHours = openHoursInput.trim().ifBlank { "12:00 PM - 2:00 AM" }
                                val cleanMapUrl = mapUrlInput.trim()

                                when {
                                    cleanName.length < 2 -> validationError = "Enter a branch name (at least 2 characters)."
                                    cleanLabel.length < 2 -> validationError = "Enter a short branch label (at least 2 characters)."
                                    cleanAddress.length < 3 -> validationError = "Enter a full branch address."
                                    else -> {
                                        validationError = null
                                        val isUpdate = editingBranchId != null
                                        val sortOrder = editingBranchId?.let { id -> branches.firstOrNull { it.id == id }?.sortOrder }
                                        onSaveBranch(
                                            editingBranchId,
                                            cleanName,
                                            cleanLabel,
                                            cleanAddress,
                                            parsedAreas,
                                            active,
                                            sortOrder,
                                            parsedLat,
                                            parsedLng,
                                            cleanMapUrl,
                                            cleanHours
                                        ) { savedId ->
                                            resetForm()
                                            savedBanner = if (isUpdate) {
                                                "Updated $cleanLabel ($savedId)"
                                            } else {
                                                "Created $cleanLabel ($savedId)"
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !isSavingBranch,
                            colors = ButtonDefaults.buttonColors(containerColor = LahoriRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("save_branch_btn")
                        ) {
                            if (isSavingBranch) {
                                CircularProgressIndicator(color = LahoriWhite, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("SAVING BRANCH...", style = MaterialTheme.typography.labelLarge)
                            } else {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (editingBranchId == null) "CREATE BRANCH" else "UPDATE BRANCH",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "branches_list_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ALL BRANCHES (${branches.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onSeedBranches) {
                    Text("SYNC STARTERS", style = MaterialTheme.typography.labelSmall, color = LahoriRed)
                }
            }
        }

        items(branches, key = { it.id }) { branch ->
            val adminCount = branchAdmins.count { it.branchId == branch.id }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("branch_card_${branch.id}")
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
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(color = LahoriBlack, shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        text = branch.id.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LahoriGold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    color = if (branch.active) LahoriSuccess.copy(alpha = 0.14f) else LahoriWarning.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (branch.active) "ACTIVE" else "PAUSED",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (branch.active) LahoriSuccess else LahoriWarning,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "$adminCount Admin${if (adminCount == 1) "" else "s"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LahoriRed
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = branch.displayShortLabel(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = branch.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row {
                            IconButton(
                                onClick = {
                                    editingBranchId = branch.id
                                    branchName = branch.name
                                    shortLabel = branch.shortLabel
                                    shortAddress = branch.shortAddress
                                    areasInput = branch.areas.joinToString(", ")
                                    openHoursInput = branch.openHours.ifBlank { "12:00 PM - 2:00 AM" }
                                    mapUrlInput = branch.mapUrl
                                    latInput = if (branch.latitude != 0.0) branch.latitude.toString() else ""
                                    lngInput = if (branch.longitude != 0.0) branch.longitude.toString() else ""
                                    active = branch.active
                                    validationError = null
                                    savedBanner = null
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("edit_branch_${branch.id}")
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit ${branch.name}", tint = MaterialTheme.colorScheme.onSurface)
                            }
                            if (branches.size > 1) {
                                IconButton(
                                    onClick = { confirmDeleteBranch = branch },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("delete_branch_${branch.id}")
                                ) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete ${branch.name}", tint = LahoriRed)
                                }
                            }
                        }
                    }

                    Text(
                        text = branch.shortAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (branch.areas.isNotEmpty()) {
                        Text(
                            text = "Delivery Zones: ${branch.areas.joinToString(" • ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    confirmDeleteBranch?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmDeleteBranch = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Delete ${target.displayShortLabel()}?", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    text = "This removes ${target.name} (${target.id}) and revokes any staff assigned exclusively to it.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteBranch(target.id)
                        if (editingBranchId == target.id) resetForm()
                        confirmDeleteBranch = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LahoriRed)
                ) {
                    Text("DELETE")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteBranch = null }) {
                    Text("CANCEL", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StaffManagementTab(
    branches: List<CafeBranch>,
    branchAdmins: List<BranchAdminEntry>,
    isSavingStaff: Boolean,
    onAddBranchAdmin: (email: String, role: String, branchId: String, onDone: () -> Unit) -> Unit,
    onRemoveBranchAdmin: (String) -> Unit
) {
    val availableBranches = remember(branches) {
        branches.ifEmpty { BranchDirectory.defaultBranches }
    }
    var staffEmail by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.BranchAdmin.name) }
    var selectedBranchId by remember(availableBranches) {
        mutableStateOf(availableBranches.first().id)
    }
    if (availableBranches.none { it.id == selectedBranchId }) {
        selectedBranchId = availableBranches.first().id
    }
    var validationError by remember { mutableStateOf<String?>(null) }
    var savedBanner by remember { mutableStateOf<String?>(null) }
    val emailPattern = remember { Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "add_staff_form_card") {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = LahoriRed.copy(alpha = 0.12f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PersonAdd,
                                contentDescription = null,
                                tint = LahoriRed,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ADD BRANCH ADMIN",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Lock each staff email to any branch. Multiple admins can manage the same or different branches; revoking takes effect instantly.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = staffEmail,
                        onValueChange = {
                            staffEmail = it
                            validationError = null
                            savedBanner = null
                        },
                        label = { Text("Staff Email Address") },
                        placeholder = { Text("manager@gmail.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("staff_email_input")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "ASSIGN ROLE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedRole == UserRole.BranchAdmin.name,
                                onClick = { selectedRole = UserRole.BranchAdmin.name },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.AdminPanelSettings,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text("BranchAdmin (Locked to Branch)", fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LahoriBlack,
                                    selectedLabelColor = LahoriGold,
                                    selectedLeadingIconColor = LahoriGold
                                ),
                                modifier = Modifier.testTag("staff_role_chip_branch_admin")
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "ASSIGN BRANCH (${availableBranches.size} AVAILABLE)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            availableBranches.forEach { branch ->
                                val isSelected = selectedBranchId == branch.id
                                val countForBranch = branchAdmins.count { it.branchId == branch.id }
                                val chipLabel = if (countForBranch > 0) {
                                    "${branch.displayShortLabel()} • $countForBranch"
                                } else {
                                    branch.displayShortLabel()
                                }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedBranchId = branch.id
                                        savedBanner = null
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Storefront,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = { Text(chipLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LahoriRed,
                                        selectedLabelColor = LahoriWhite,
                                        selectedLeadingIconColor = LahoriWhite
                                    ),
                                    modifier = Modifier.testTag("staff_${branch.id}_chip")
                                )
                            }
                        }
                    }

                    if (validationError != null) {
                        Text(
                            text = validationError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = LahoriRed
                        )
                    }

                    if (savedBanner != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = LahoriSuccess.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, LahoriSuccess.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = LahoriSuccess,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = savedBanner!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val cleanEmail = staffEmail.trim().lowercase()
                            when {
                                !emailPattern.matches(cleanEmail) -> {
                                    validationError = "Please enter a valid staff email address."
                                }
                                cleanEmail.equals(SUPER_ADMIN_EMAIL, ignoreCase = true) -> {
                                    validationError = "This email is already reserved for Super Admin."
                                }
                                else -> {
                                    validationError = null
                                    val targetBranchLabel = BranchDirectory.shortPickerLabel(selectedBranchId, availableBranches)
                                    onAddBranchAdmin(cleanEmail, selectedRole, selectedBranchId) {
                                        staffEmail = ""
                                        savedBanner = "Saved $selectedRole access for $cleanEmail → $targetBranchLabel"
                                    }
                                }
                            }
                        },
                        enabled = !isSavingStaff,
                        colors = ButtonDefaults.buttonColors(containerColor = LahoriRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_staff_btn")
                    ) {
                        if (isSavingStaff) {
                            CircularProgressIndicator(
                                color = LahoriWhite,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("SAVING TO FIRESTORE...", style = MaterialTheme.typography.labelLarge)
                        } else {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("SAVE STAFF ACCESS", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        item(key = "assigned_staff_header") {
            Text(
                text = "ASSIGNED BRANCH ADMINS (${branchAdmins.size})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (branchAdmins.isEmpty()) {
            item(key = "no_branch_admins") {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No Branch Managers assigned yet. Enter any manager Gmail above and select a branch to lock their access.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(branchAdmins, key = { it.id }) { admin ->
                val branchLabel = BranchDirectory.shortPickerLabel(admin.branchId, availableBranches)
                val roleLabel = admin.role.ifBlank { UserRole.BranchAdmin.name }
                Surface(
                    shape = RoundedCornerShape(12.dp),
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
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = admin.email,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = LahoriBlack,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = roleLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LahoriGold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "$branchLabel • ${admin.branchId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LahoriRed,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        IconButton(
                            onClick = { onRemoveBranchAdmin(admin.id) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("remove_staff_${admin.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Revoke access for ${admin.email}",
                                tint = LahoriRed
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BranchAdminDashboardScreen(
    branchId: String,
    branchName: String,
    ordersState: UiState<List<CafeOrder>>,
    onUpdateOrderStatus: (orderId: String, newStatus: OrderStatus) -> Unit,
    onBack: () -> Unit = {},
    managerEmail: String = "",
    onSignOut: (() -> Unit)? = null,
    branches: List<CafeBranch> = BranchDirectory.defaultBranches,
    newOrderAlert: CafeOrder? = null,
    onDismissNewOrderAlert: () -> Unit = {},
    branchPromotions: List<Promotion> = emptyList(),
    branchBanners: List<BranchBanner> = emptyList(),
    isSavingBranchPromo: Boolean = false,
    isSavingBranchBanner: Boolean = false,
    onSaveBranchPromo: (
        promoDocId: String?,
        code: String,
        discountType: String,
        discountValue: Long,
        minOrderPkr: Long,
        active: Boolean,
        expiresAt: Timestamp?,
        onError: (String) -> Unit,
        onDone: () -> Unit
    ) -> Unit = { _, _, _, _, _, _, _, _, onDone -> onDone() },
    onDeleteBranchPromo: (String) -> Unit = {},
    onSaveBranchBanner: (
        bannerId: String?,
        title: String,
        subtitle: String,
        promoCode: String,
        badgeText: String,
        imageUrl: String,
        pickedImageUri: Uri?,
        targetCategory: String,
        active: Boolean,
        expiresAt: Timestamp?,
        onError: (String) -> Unit,
        onDone: () -> Unit
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, onDone -> onDone() },
    onDeleteBranchBanner: (String) -> Unit = {},
    notificationsEnabled: Boolean = true,
    liveAlertsActive: Boolean = false,
    onRequestEnableAlerts: () -> Unit = {},
    adminNotifications: List<AdminDeliveryNotification> = emptyList(),
    unreadNotificationCount: Int = 0,
    deliveredPopupQueue: List<AdminDeliveryNotification> = emptyList(),
    soundEnabled: Boolean = true,
    onToggleSound: (Boolean) -> Unit = {},
    onMarkNotificationRead: (String) -> Unit = {},
    onMarkAllNotificationsRead: () -> Unit = {},
    onDismissDeliveredPopup: () -> Unit = {}
) {
    val isRestrictedRootMode = onSignOut != null
    if (!isRestrictedRootMode) {
        BackHandler(onBack = onBack)
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var orderFilter by remember { mutableStateOf("ALL") }
    var inspectedOrderDetail by remember { mutableStateOf<Pair<CafeOrder?, AdminDeliveryNotification?>?>(null) }
    val allOrdersList = (ordersState as? UiState.Success)?.data ?: emptyList()

    val branchLabel = remember(branchId, branches, branchName) {
        BranchDirectory.shortPickerLabel(branchId, branches)
            .ifBlank { branchName.substringAfter("— ").trim().ifBlank { branchId } }
    }
    val livePromoCount = remember(branchPromotions, branchBanners, branchId) {
        val now = System.currentTimeMillis()
        branchPromotions.count { it.isCurrentlyValidForBranch(branchId, now) } +
            branchBanners.count { it.isVisibleForBranch(branchId, now) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(color = LahoriBlack, contentColor = LahoriWhite) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isRestrictedRootMode) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("branch_admin_back_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = LahoriWhite
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = LahoriRed
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Storefront,
                                    contentDescription = null,
                                    tint = LahoriWhite,
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .size(22.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "BRANCH ADMIN DASHBOARD",
                                style = MaterialTheme.typography.titleMedium,
                                color = LahoriGold
                            )
                            Text(
                                text = "$branchLabel • $branchName",
                                style = MaterialTheme.typography.bodySmall,
                                color = LahoriWhite
                            )
                            if (managerEmail.isNotBlank()) {
                                Text(
                                    text = "$managerEmail • Branch Orders & Promos",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LahoriWhite.copy(alpha = 0.72f)
                                )
                            }
                        }

                        if (onSignOut != null) {
                            TextButton(
                                onClick = onSignOut,
                                colors = ButtonDefaults.textButtonColors(contentColor = LahoriWhite),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("branch_admin_sign_out_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "Sign out",
                                    tint = LahoriGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "SIGN OUT",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = LahoriWhite
                                )
                            }
                        }
                    }

                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = LahoriBlack,
                        contentColor = LahoriWhite,
                        indicator = { tabPositions ->
                            if (selectedTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = LahoriRed,
                                    height = 3.dp
                                )
                            }
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            modifier = Modifier.testTag("branch_admin_tab_orders"),
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ReceiptLong,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = if (selectedTab == 0) LahoriGold else LahoriWhite.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = "ORDERS",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (selectedTab == 0) LahoriWhite else LahoriWhite.copy(alpha = 0.7f),
                                        maxLines = 1
                                    )
                                    if (unreadNotificationCount > 0) {
                                        Surface(color = LahoriRed, shape = CircleShape) {
                                            Text(
                                                text = "$unreadNotificationCount",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = LahoriWhite,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            modifier = Modifier.testTag("branch_admin_tab_promos"),
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocalOffer,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = if (selectedTab == 1) LahoriGold else LahoriWhite.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = if (livePromoCount > 0) "PROMOS ($livePromoCount)" else "PROMOS",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (selectedTab == 1) LahoriWhite else LahoriWhite.copy(alpha = 0.7f),
                                        maxLines = 1
                                    )
                                }
                            }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            modifier = Modifier.testTag("branch_admin_tab_notifications"),
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.NotificationsActive,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = if (selectedTab == 2) LahoriGold else LahoriWhite.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = "ALERTS",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (selectedTab == 2) LahoriWhite else LahoriWhite.copy(alpha = 0.7f),
                                        maxLines = 1
                                    )
                                    if (unreadNotificationCount > 0) {
                                        Surface(color = LahoriGold, shape = CircleShape) {
                                            Text(
                                                text = "$unreadNotificationCount",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = LahoriBlack,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
        ) {
            if (selectedTab == 1) {
                BranchPromoManagerTab(
                    branchId = branchId,
                    branchName = branchName,
                    branchLabel = branchLabel,
                    branchPromotions = branchPromotions,
                    branchBanners = branchBanners,
                    isSavingPromo = isSavingBranchPromo,
                    isSavingBanner = isSavingBranchBanner,
                    onSaveBranchPromo = onSaveBranchPromo,
                    onDeleteBranchPromo = onDeleteBranchPromo,
                    onSaveBranchBanner = onSaveBranchBanner,
                    onDeleteBranchBanner = onDeleteBranchBanner
                )
            } else if (selectedTab == 2) {
                AdminNotificationsHistoryTab(
                    notifications = adminNotifications,
                    soundEnabled = soundEnabled,
                    onToggleSound = onToggleSound,
                    onOpenNotification = { notif ->
                        if (!notif.read) onMarkNotificationRead(notif.id)
                        val matched = allOrdersList.firstOrNull { it.id == notif.orderId }
                        inspectedOrderDetail = matched to notif
                    },
                    onMarkAllRead = onMarkAllNotificationsRead
                )
            } else when (ordersState) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = LahoriRed)
                }
                is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(ordersState.message, color = LahoriRed)
                }
                is UiState.Success -> {
                    val allBranchOrders = ordersState.data
                    val activeCount = remember(allBranchOrders) {
                        allBranchOrders.count { OrderStatus.fromString(it.status) != OrderStatus.Delivered }
                    }
                    val completedCount = remember(allBranchOrders) {
                        allBranchOrders.count { OrderStatus.fromString(it.status) == OrderStatus.Delivered }
                    }
                    val visibleOrders = remember(allBranchOrders, orderFilter) {
                        when (orderFilter) {
                            "ACTIVE" -> allBranchOrders.filter { OrderStatus.fromString(it.status) != OrderStatus.Delivered }
                            "COMPLETED" -> allBranchOrders.filter { OrderStatus.fromString(it.status) == OrderStatus.Delivered }
                            else -> allBranchOrders
                        }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(key = "branch_new_order_alert") {
                            IncomingOrderAlertBanner(
                                incoming = newOrderAlert,
                                onDismiss = onDismissNewOrderAlert
                            )
                        }
                        item(key = "branch_live_alert_bar") {
                            if (!notificationsEnabled || !liveAlertsActive) {
                                Surface(
                                    onClick = onRequestEnableAlerts,
                                    shape = RoundedCornerShape(12.dp),
                                    color = LahoriBlack,
                                    border = BorderStroke(1.2.dp, LahoriGold),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("branch_enable_bg_alerts_btn")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.NotificationsOff,
                                            contentDescription = null,
                                            tint = LahoriGold,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (!notificationsEnabled) "ENABLE OUTSIDE-APP NOTIFICATIONS" else "START BACKGROUND ORDER ALERTS",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = LahoriGold
                                            )
                                            Text(
                                                text = "Tap to receive instant order alerts with sound even when the app is closed or minimized.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = LahoriWhite.copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = LahoriSuccess.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, LahoriSuccess.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.NotificationsActive,
                                            contentDescription = null,
                                            tint = LahoriSuccess,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Background Push & Sound Alerts Active (outside app)",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = LahoriSuccess
                                        )
                                    }
                                }
                            }
                        }

                        item(key = "branch_orders_header") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = LahoriRed.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, LahoriRed.copy(alpha = 0.25f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "REAL-TIME ${branchLabel.uppercase()} QUEUE",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = LahoriRed
                                            )
                                            Text(
                                                text = "Restricted staff view: incoming orders for your branch only.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = LahoriRed
                                        ) {
                                            Text(
                                                text = "$activeCount LIVE",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = LahoriWhite,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(
                                        "ALL" to "All (${allBranchOrders.size})",
                                        "ACTIVE" to "Active ($activeCount)",
                                        "COMPLETED" to "Completed ($completedCount)"
                                    ).forEach { (key, label) ->
                                        FilterChip(
                                            selected = orderFilter == key,
                                            onClick = { orderFilter = key },
                                            label = { Text(label, fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = LahoriRed,
                                                selectedLabelColor = LahoriWhite
                                            ),
                                            modifier = Modifier.testTag("branch_orders_filter_${key.lowercase()}")
                                        )
                                    }
                                }
                            }
                        }

                        if (visibleOrders.isEmpty()) {
                            item(key = "no_branch_orders_empty") {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ReceiptLong,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Text(
                                            text = "No incoming orders for $branchLabel",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "New customer orders routed to $branchName appear here automatically in real time.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(visibleOrders, key = { it.id }) { order ->
                                AdminOrderControlCard(
                                    order = order,
                                    onChangeStatus = { status -> onUpdateOrderStatus(order.id, status) },
                                    onViewDetails = { inspectedOrderDetail = order to null }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Instant Real-Time Order Delivered Popup (triggers on ANY Branch Admin tab)
    val activePopup = deliveredPopupQueue.firstOrNull()
    if (activePopup != null) {
        OrderDeliveredPopupModal(
            notification = activePopup,
            remainingInQueue = (deliveredPopupQueue.size - 1).coerceAtLeast(0),
            onViewOrder = { notif ->
                if (!notif.read) onMarkNotificationRead(notif.id)
                onDismissDeliveredPopup()
                val matched = allOrdersList.firstOrNull { it.id == notif.orderId }
                inspectedOrderDetail = matched to notif
            },
            onDismiss = onDismissDeliveredPopup
        )
    }

    inspectedOrderDetail?.let { (order, fallbackNotif) ->
        AdminOrderDetailDialog(
            order = order,
            notificationFallback = fallbackNotif,
            onDismiss = { inspectedOrderDetail = null }
        )
    }
}

@Composable
private fun AdminLiveAlertStatusPill(
    notificationsEnabled: Boolean,
    liveAlertsActive: Boolean,
    onClick: () -> Unit
) {
    val active = notificationsEnabled && liveAlertsActive
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (active) LahoriSuccess.copy(alpha = 0.22f) else LahoriRed,
        border = BorderStroke(1.dp, if (active) LahoriSuccess else LahoriGold),
        modifier = Modifier
            .padding(end = 6.dp)
            .testTag("admin_live_alerts_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = if (active) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                contentDescription = null,
                tint = if (active) LahoriGold else LahoriWhite,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = if (active) "ALERTS ON" else "ENABLE ALERTS",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                color = LahoriWhite
            )
        }
    }
}

@Composable
private fun IncomingOrderAlertBanner(
    incoming: CafeOrder?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AnimatedVisibility(visible = incoming != null) {
        incoming?.let { order ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = LahoriBlack,
                border = BorderStroke(2.dp, LahoriGold),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("branch_new_order_alert_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = LahoriGold,
                            modifier = Modifier.size(26.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "NEW ORDER RECEIVED • #${order.id.takeLast(6).uppercase()}",
                                style = MaterialTheme.typography.labelLarge,
                                color = LahoriGold
                            )
                            Text(
                                text = "${order.customerName} (${order.orderType}) • ${formatPkr(order.totalPkr)}",
                                style = MaterialTheme.typography.titleSmall,
                                color = LahoriWhite
                            )
                            Text(
                                text = order.itemsSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = LahoriWhite.copy(alpha = 0.85f)
                            )
                            if (order.orderType.equals(OrderType.DELIVERY.name, ignoreCase = true) && order.deliveryAddress.isNotBlank()) {
                                Text(
                                    text = "Location: ${order.deliveryAddress}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LahoriGold.copy(alpha = 0.9f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Dismiss new order alert",
                                tint = LahoriWhite
                            )
                        }
                    }

                    if (order.orderType.equals(OrderType.DELIVERY.name, ignoreCase = true) &&
                        (order.hasDeliveryLocation() || order.deliveryAddress.isNotBlank())
                    ) {
                        Button(
                            onClick = {
                                LocationHelper.openCustomerLocationInMaps(
                                    context = context,
                                    latitude = order.deliveryLatitude,
                                    longitude = order.deliveryLongitude,
                                    address = order.deliveryAddress,
                                    customerName = order.customerName
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LahoriRed, contentColor = LahoriWhite),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (order.hasDeliveryLocation()) "OPEN LIVE GPS LOCATION ON MAP" else "VIEW ADDRESS ON MAP",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminOrderControlCard(
    order: CafeOrder,
    onChangeStatus: (OrderStatus) -> Unit,
    onViewDetails: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val currentStatus = OrderStatus.fromString(order.status)
    val isDelivery = order.orderType.equals(OrderType.DELIVERY.name, ignoreCase = true)
    val hasGpsPin = isDelivery && order.hasDeliveryLocation()

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "#${order.id.takeLast(6).uppercase()} • ${order.branchId.uppercase()}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${order.customerName} (${order.orderType}) • ${formatOrderDate(order.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatPkr(order.totalPkr),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = LahoriRed
                    )
                    Text(
                        text = if (order.deliveryFeePkr == 0L) "Delivery: Rs. 0 (FREE)" else "Delivery: ${formatPkr(order.deliveryFeePkr)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = LahoriSuccess
                    )
                }
            }

            Text(order.itemsSummary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)

            if (onViewDetails != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Subtotal: ${formatPkr(order.subtotalPkr)} • Delivery Charges: Rs. 0",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = onViewDetails,
                        modifier = Modifier.testTag("admin_view_order_details_${order.id}")
                    ) {
                        Text("VIEW DETAILS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = LahoriRed)
                    }
                }
            }

            if (order.customerPhone.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Phone: ${order.customerPhone}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = { LocationHelper.dialCustomerPhone(context, order.customerPhone) },
                        modifier = Modifier.testTag("admin_call_customer_${order.id}")
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = null, tint = LahoriSuccess, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("CALL", style = MaterialTheme.typography.labelMedium, color = LahoriSuccess)
                    }
                }
            }

            if (isDelivery) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (hasGpsPin) LahoriSuccess.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        1.dp,
                        if (hasGpsPin) LahoriSuccess.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = if (hasGpsPin) LahoriSuccess else LahoriRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (hasGpsPin) "CUSTOMER LIVE GPS LOCATION" else "DELIVERY ADDRESS",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (hasGpsPin) LahoriSuccess else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (order.deliveryAddress.isNotBlank()) {
                            Text(
                                text = order.deliveryAddress,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (hasGpsPin) {
                            Text(
                                text = "GPS Pin: ${order.formattedCoordinates()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (hasGpsPin || order.deliveryAddress.isNotBlank()) {
                            Button(
                                onClick = {
                                    LocationHelper.openCustomerLocationInMaps(
                                        context = context,
                                        latitude = order.deliveryLatitude,
                                        longitude = order.deliveryLongitude,
                                        address = order.deliveryAddress,
                                        customerName = order.customerName
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (hasGpsPin) LahoriBlack else LahoriRed,
                                    contentColor = LahoriWhite
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("admin_open_order_map_${order.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Map,
                                    contentDescription = null,
                                    tint = if (hasGpsPin) LahoriGold else LahoriWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (hasGpsPin) "OPEN CUSTOMER LOCATION ON MAP" else "SEARCH ADDRESS ON MAP",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = order.deliveryAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            Text("UPDATE STATUS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val isPickup = order.orderType.equals(OrderType.PICKUP.name, ignoreCase = true)
                OrderStatus.entries.forEach { st ->
                    val active = st == currentStatus
                    val chipLabel = when (st) {
                        OrderStatus.Dispatched -> if (isPickup) "Ready for Pickup" else st.name
                        OrderStatus.Delivered -> if (isPickup) "Picked Up" else st.name
                        else -> st.name
                    }
                    FilterChip(
                        selected = active,
                        onClick = { if (!active) onChangeStatus(st) },
                        label = { Text(chipLabel, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (st) {
                                OrderStatus.Confirmed -> LahoriRed
                                OrderStatus.Preparing -> LahoriWarning
                                OrderStatus.Dispatched -> LahoriBlack
                                OrderStatus.Delivered -> LahoriSuccess
                            },
                            selectedLabelColor = LahoriWhite
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MenuEditorDialog(
    initialItem: MenuItem?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        itemId: String?,
        name: String,
        description: String,
        category: String,
        pricePkr: Long,
        imageKey: String,
        imageUrl: String,
        pickedImageUri: Uri?,
        badgeText: String,
        available: Boolean,
        sortOrder: Long,
        onError: (String) -> Unit
    ) -> Unit
) {
    var name by remember(initialItem?.id) { mutableStateOf(initialItem?.name ?: "") }
    var description by remember(initialItem?.id) { mutableStateOf(initialItem?.description ?: "") }
    var category by remember(initialItem?.id) { mutableStateOf(initialItem?.category ?: "BURGERS") }
    var priceStr by remember(initialItem?.id) { mutableStateOf(initialItem?.pricePkr?.toString() ?: "450") }
    var imageKey by remember(initialItem?.id) { mutableStateOf(initialItem?.imageKey ?: "zinger_burger") }
    var imageUrl by remember(initialItem?.id) { mutableStateOf(initialItem?.imageUrl ?: "") }
    var pickedImageUri by remember(initialItem?.id) { mutableStateOf<Uri?>(null) }
    var badgeText by remember(initialItem?.id) { mutableStateOf(initialItem?.badgeText ?: "NEW") }
    var available by remember(initialItem?.id) { mutableStateOf(initialItem?.available ?: true) }
    var errorMsg by remember(initialItem?.id) { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedImageUri = uri
            errorMsg = null
        }
    }

    val hasCustomImage = pickedImageUri != null || imageUrl.isNotBlank()
    val categories = SeedMenuData.categories.filter { it != "ALL" }
    val imageKeys = listOf(
        "pasta_bowl",
        "combo_deal",
        "birthday_deal",
        "zinger_burger",
        "extreme_pizza",
        "wrap_roll",
        "signature_sandwich",
        "crispy_fries",
        "hot_wings",
        "pickled_crunch",
        "soft_drink"
    )

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = if (initialItem?.id.isNullOrBlank()) "Add Menu Item" else "Edit Menu Item",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Item Image Upload & Live Preview Section
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Item Image",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when {
                                    pickedImageUri != null -> LahoriRed
                                    imageUrl.isNotBlank() -> LahoriSuccess
                                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                                }
                            ) {
                                Text(
                                    text = when {
                                        pickedImageUri != null -> "READY TO UPLOAD"
                                        imageUrl.isNotBlank() -> "CUSTOM PHOTO"
                                        else -> "ARTWORK THEME"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = if (hasCustomImage) LahoriWhite else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Live Preview Thumbnail (tappable to pick photo from gallery)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(152.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !isSaving) {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .testTag("menu_image_preview_box")
                        ) {
                            FoodArtworkBox(
                                imageKey = imageKey,
                                imageUrl = imageUrl,
                                previewModel = pickedImageUri,
                                contentDescription = name.ifBlank { "Menu item photo preview" },
                                modifier = Modifier.fillMaxSize()
                            )
                            if (!isSaving) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = LahoriBlack.copy(alpha = 0.72f),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.AddPhotoAlternate,
                                            contentDescription = null,
                                            tint = LahoriGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (hasCustomImage) "Tap to change" else "Tap to upload",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = LahoriWhite
                                        )
                                    }
                                }
                            }
                            if (isSaving && pickedImageUri != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(LahoriBlack.copy(alpha = 0.55f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            color = LahoriGold,
                                            strokeWidth = 3.dp,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Text(
                                            text = "Uploading to cloud...",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LahoriWhite
                                        )
                                    }
                                }
                            }
                        }

                        // Direct Gallery Upload + Remove Image buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                enabled = !isSaving,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LahoriRed,
                                    contentColor = LahoriWhite
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("menu_upload_image_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = LahoriWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (hasCustomImage) "CHANGE PHOTO" else "UPLOAD PHOTO",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = LahoriWhite,
                                    maxLines = 1
                                )
                            }

                            if (hasCustomImage) {
                                OutlinedButton(
                                    onClick = {
                                        pickedImageUri = null
                                        imageUrl = ""
                                        errorMsg = null
                                    },
                                    enabled = !isSaving,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("menu_remove_image_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Remove Image",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "REMOVE",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Select a photo directly from your gallery. When you tap SAVE, it uploads automatically to the cloud and appears live for all customers.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    enabled = !isSaving,
                    label = { Text("Item Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("menu_item_name_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    enabled = !isSaving,
                    label = { Text("Description") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("menu_item_desc_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it.filter { c -> c.isDigit() }; errorMsg = null },
                        enabled = !isSaving,
                        label = { Text("Price (PKR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("menu_item_price_input")
                    )
                    OutlinedTextField(
                        value = badgeText,
                        onValueChange = { badgeText = it },
                        enabled = !isSaving,
                        label = { Text("Badge") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("menu_item_badge_input")
                    )
                }

                Text("Category", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            enabled = !isSaving,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 10.sp) }
                        )
                    }
                }

                Text("Artwork Theme (Fallback)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    imageKeys.forEach { key ->
                        FilterChip(
                            selected = imageKey == key,
                            enabled = !isSaving,
                            onClick = { imageKey = key },
                            label = { Text(key.replace("_", " "), fontSize = 10.sp) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Available in Menu", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = available,
                        enabled = !isSaving,
                        onCheckedChange = { available = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = LahoriRed)
                    )
                }

                if (errorMsg != null) {
                    Text(errorMsg!!, style = MaterialTheme.typography.bodySmall, color = LahoriRed)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                onClick = {
                    val price = priceStr.toLongOrNull() ?: 0L
                    val trimmedUrl = imageUrl.trim()
                    if (name.isBlank() || price <= 0L) {
                        errorMsg = "Enter a valid name and PKR price > 0."
                    } else {
                        errorMsg = null
                        onSave(
                            initialItem?.id?.takeIf { it.isNotBlank() },
                            name,
                            description,
                            category,
                            price,
                            imageKey,
                            trimmedUrl,
                            pickedImageUri,
                            badgeText,
                            available,
                            initialItem?.sortOrder ?: 10L
                        ) { saveErr ->
                            errorMsg = saveErr
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LahoriRed),
                modifier = Modifier.testTag("menu_editor_save_btn")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = LahoriWhite,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (pickedImageUri != null) "UPLOADING..." else "SAVING...")
                } else {
                    Text("SAVE")
                }
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isSaving,
                onClick = onDismiss
            ) {
                Text("CANCEL", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    )
}

@Composable
private fun PromoEditorDialog(
    initialPromo: Promotion?,
    onDismiss: () -> Unit,
    onSave: (
        code: String,
        discountType: String,
        discountValue: Long,
        minOrderPkr: Long,
        active: Boolean
    ) -> Unit
) {
    var code by remember { mutableStateOf(initialPromo?.code ?: "") }
    var discountType by remember { mutableStateOf(initialPromo?.discountType ?: "PERCENT") }
    var discountValueStr by remember { mutableStateOf(initialPromo?.discountValue?.toString() ?: "20") }
    var minOrderStr by remember { mutableStateOf(initialPromo?.minOrderPkr?.toString() ?: "500") }
    var active by remember { mutableStateOf(initialPromo?.active ?: true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = if (initialPromo == null) "Create Promo Code" else "Edit Promo Code",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().filter { ch -> ch.isLetterOrDigit() || ch == '_' || ch == '-' }; errorMsg = null },
                    label = { Text("Promo Code (e.g. LAHORI25)") },
                    enabled = initialPromo == null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("PERCENT" to "% Percent", "FLAT" to "PKR Flat").forEach { (type, label) ->
                        FilterChip(
                            selected = discountType == type,
                            onClick = { discountType = type; errorMsg = null },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = discountValueStr,
                        onValueChange = { discountValueStr = it.filter { c -> c.isDigit() }; errorMsg = null },
                        label = { Text(if (discountType == "PERCENT") "Percent (1-90)" else "Flat (PKR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minOrderStr,
                        onValueChange = { minOrderStr = it.filter { c -> c.isDigit() }; errorMsg = null },
                        label = { Text("Min Order PKR") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Promo Active", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = active,
                        onCheckedChange = { active = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = LahoriRed)
                    )
                }
                if (errorMsg != null) {
                    Text(errorMsg!!, style = MaterialTheme.typography.bodySmall, color = LahoriRed)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = code.trim().uppercase()
                    val valNum = discountValueStr.toLongOrNull() ?: 0L
                    val minOrder = minOrderStr.toLongOrNull() ?: 0L
                    when {
                        clean.length !in 2..30 -> errorMsg = "Code must be 2-30 characters."
                        discountType == "PERCENT" && valNum !in 1..90 -> errorMsg = "Percent must be between 1 and 90."
                        discountType == "FLAT" && valNum !in 1..100000 -> errorMsg = "Flat discount must be > 0 PKR."
                        else -> onSave(clean, discountType, valNum, minOrder, active)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LahoriRed)
            ) {
                Text("SAVE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = MaterialTheme.colorScheme.onSurface) }
        }
    )
}
