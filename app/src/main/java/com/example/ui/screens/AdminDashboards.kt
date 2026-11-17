package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BranchAdminEntry
import com.example.model.BranchDirectory
import com.example.model.CafeOrder
import com.example.model.MenuItem
import com.example.model.OrderReview
import com.example.model.OrderStatus
import com.example.model.Promotion
import com.example.model.SeedMenuData
import com.example.model.UiState
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
        badgeText: String,
        available: Boolean,
        sortOrder: Long,
        onDone: () -> Unit
    ) -> Unit,
    onDeleteMenuItem: (String) -> Unit,
    onSeedMenu: () -> Unit,
    onAddBranchAdmin: (email: String, branchId: String, onDone: () -> Unit) -> Unit,
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
    reviews: List<OrderReview> = emptyList()
) {
    BackHandler(onBack = onBack)

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var branchFilter by remember { mutableStateOf("ALL") }
    var editingMenuItem by remember { mutableStateOf<MenuItem?>(null) }
    var showAddMenuDialog by remember { mutableStateOf(false) }
    var showAddManagerDialog by remember { mutableStateOf(false) }
    var editingPromotion by remember { mutableStateOf<Promotion?>(null) }
    var showPromoDialog by remember { mutableStateOf(false) }

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
                            Text("All Branches • Live Menu Manager • Staff RBAC", style = MaterialTheme.typography.bodySmall, color = LahoriWhite.copy(alpha = 0.75f))
                        }
                    }

                    ScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = LahoriBlack,
                        contentColor = LahoriWhite,
                        edgePadding = 8.dp,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = LahoriRed
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = { Text("ORDERS", fontSize = 11.sp, maxLines = 1) },
                            icon = { Icon(Icons.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = { Text("MENU", fontSize = 11.sp, maxLines = 1) },
                            icon = { Icon(Icons.Filled.RestaurantMenu, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = selectedTabIndex == 2,
                            onClick = { selectedTabIndex = 2 },
                            text = { Text("STAFF", fontSize = 11.sp, maxLines = 1) },
                            icon = { Icon(Icons.Filled.People, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = selectedTabIndex == 3,
                            onClick = { selectedTabIndex = 3 },
                            text = { Text("PROMOS", fontSize = 11.sp, maxLines = 1) },
                            icon = { Icon(Icons.Filled.LocalOffer, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = selectedTabIndex == 4,
                            onClick = { selectedTabIndex = 4 },
                            text = { Text("REVIEWS", fontSize = 11.sp, maxLines = 1) },
                            icon = { Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(18.dp)) }
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
                                item(key = "branch_filter_chips") {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("ALL" to "All Branches", "branch_1" to "Branch 1 (Gulberg)", "branch_2" to "Branch 2 (DHA)").forEach { (id, label) ->
                                            FilterChip(
                                                selected = branchFilter == id,
                                                onClick = { branchFilter = id },
                                                label = { Text(label, fontSize = 11.sp) },
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
                                            onChangeStatus = { status -> onUpdateOrderStatus(order.id, status) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                        Text(
                                            text = "${item.category} • ${formatPkr(item.pricePkr)}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = LahoriRed
                                        )
                                        Text(
                                            text = if (item.available) "In Stock" else "Sold Out",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (item.available) LahoriSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row {
                                        IconButton(
                                            onClick = {
                                                editingMenuItem = if (isCloudDoc) item else item.copy(id = "")
                                                showAddMenuDialog = true
                                            },
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Edit menu item", tint = MaterialTheme.colorScheme.onSurface)
                                        }
                                        if (isCloudDoc) {
                                            IconButton(
                                                onClick = { onDeleteMenuItem(item.id) },
                                                modifier = Modifier.size(44.dp)
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

                2 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item(key = "add_manager_btn") {
                            Button(
                                onClick = { showAddManagerDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = LahoriRed),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("ASSIGN BRANCH MANAGER", style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        if (branchAdmins.isEmpty()) {
                            item(key = "no_branch_admins") {
                                Text(
                                    text = "No Branch Managers assigned yet. Assign a Gmail address to branch_1 or branch_2 so managers can log in and see only their branch orders.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(branchAdmins, key = { it.id }) { admin ->
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
                                            Text(admin.email, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                            Text("${admin.branchId} • ${admin.branchName}", style = MaterialTheme.typography.bodySmall, color = LahoriRed)
                                        }
                                        IconButton(onClick = { onRemoveBranchAdmin(admin.id) }) {
                                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove manager", tint = LahoriRed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
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
                                            Text(
                                                text = "${promo.code} • $discountLabel",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Min order: ${formatPkr(promo.minOrderPkr)} • ${if (promo.active) "Active" else "Paused"}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (promo.active) LahoriSuccess else MaterialTheme.colorScheme.onSurfaceVariant
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

                4 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (reviews.isEmpty()) {
                            item(key = "no_reviews") {
                                Text(
                                    text = "No customer order reviews yet. Ratings appear automatically after delivered orders are reviewed.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(reviews, key = { it.id }) { review ->
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
                                                text = "${review.customerName} • #${review.orderId.takeLast(6).uppercase()}",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
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
                                                    Text(
                                                        text = "${review.rating}/5",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
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
                    }
                }
            }
        }
    }

    if (showAddMenuDialog) {
        MenuEditorDialog(
            initialItem = editingMenuItem,
            onDismiss = {
                showAddMenuDialog = false
                editingMenuItem = null
            },
            onSave = { id, name, desc, cat, price, imgKey, badge, available, sortOrder ->
                onSaveMenuItem(id, name, desc, cat, price, imgKey, badge, available, sortOrder) {
                    showAddMenuDialog = false
                    editingMenuItem = null
                }
            }
        )
    }

    if (showAddManagerDialog) {
        AddBranchManagerDialog(
            onDismiss = { showAddManagerDialog = false },
            onSave = { email, branchId ->
                onAddBranchAdmin(email, branchId) {
                    showAddManagerDialog = false
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

@Composable
fun BranchAdminDashboardScreen(
    branchId: String,
    branchName: String,
    ordersState: UiState<List<CafeOrder>>,
    onUpdateOrderStatus: (orderId: String, newStatus: OrderStatus) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(color = LahoriRed, contentColor = LahoriWhite) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LahoriWhite)
                    }
                    Column {
                        Text("BRANCH MANAGER (${branchId.uppercase()})", style = MaterialTheme.typography.titleLarge, color = LahoriWhite)
                        Text(branchName, style = MaterialTheme.typography.bodySmall, color = LahoriWhite.copy(alpha = 0.85f))
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
            when (ordersState) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = LahoriRed)
                }
                is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(ordersState.message, color = LahoriRed)
                }
                is UiState.Success -> {
                    if (ordersState.data.isEmpty()) {
                        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No incoming orders assigned to $branchId yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(ordersState.data, key = { it.id }) { order ->
                                AdminOrderControlCard(
                                    order = order,
                                    onChangeStatus = { status -> onUpdateOrderStatus(order.id, status) }
                                )
                            }
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
    onChangeStatus: (OrderStatus) -> Unit
) {
    val currentStatus = OrderStatus.fromString(order.status)
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
                Column {
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
                Text(
                    text = formatPkr(order.totalPkr),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = LahoriRed
                )
            }

            Text(order.itemsSummary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(order.deliveryAddress, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            Text("UPDATE STATUS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OrderStatus.entries.forEach { st ->
                    val active = st == currentStatus
                    FilterChip(
                        selected = active,
                        onClick = { if (!active) onChangeStatus(st) },
                        label = { Text(st.name, fontSize = 11.sp) },
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
    onDismiss: () -> Unit,
    onSave: (
        itemId: String?,
        name: String,
        description: String,
        category: String,
        pricePkr: Long,
        imageKey: String,
        badgeText: String,
        available: Boolean,
        sortOrder: Long
    ) -> Unit
) {
    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var description by remember { mutableStateOf(initialItem?.description ?: "") }
    var category by remember { mutableStateOf(initialItem?.category ?: "SIGNATURE BURGERS") }
    var priceStr by remember { mutableStateOf(initialItem?.pricePkr?.toString() ?: "650") }
    var imageKey by remember { mutableStateOf(initialItem?.imageKey ?: "zinger_burger") }
    var badgeText by remember { mutableStateOf(initialItem?.badgeText ?: "NEW") }
    var available by remember { mutableStateOf(initialItem?.available ?: true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val categories = SeedMenuData.categories.filter { it != "ALL" }
    val imageKeys = listOf("zinger_burger", "extreme_pizza", "hot_wings", "signature_sandwich", "pickled_crunch", "everyday_value")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = if (initialItem?.id.isNullOrBlank()) "Add Menu Item" else "Edit Menu Item",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text("Item Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it.filter { c -> c.isDigit() }; errorMsg = null },
                        label = { Text("Price (PKR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = badgeText,
                        onValueChange = { badgeText = it },
                        label = { Text("Badge") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Category", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 10.sp) }
                        )
                    }
                }

                Text("Artwork Theme", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    imageKeys.forEach { key ->
                        FilterChip(
                            selected = imageKey == key,
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
                onClick = {
                    val price = priceStr.toLongOrNull() ?: 0L
                    if (name.isBlank() || price <= 0L) {
                        errorMsg = "Enter a valid name and PKR price > 0."
                    } else {
                        onSave(
                            initialItem?.id?.takeIf { it.isNotBlank() },
                            name,
                            description,
                            category,
                            price,
                            imageKey,
                            badgeText,
                            available,
                            initialItem?.sortOrder ?: 10L
                        )
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

@Composable
private fun AddBranchManagerDialog(
    onDismiss: () -> Unit,
    onSave: (email: String, branchId: String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var selectedBranchId by remember { mutableStateOf(BranchDirectory.branch1.id) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Assign Branch Manager", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMsg = null },
                    label = { Text("Manager Gmail Address") },
                    placeholder = { Text("manager@gmail.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BranchDirectory.allBranches.forEach { branch ->
                        FilterChip(
                            selected = selectedBranchId == branch.id,
                            onClick = { selectedBranchId = branch.id },
                            label = { Text(branch.id.uppercase(), fontSize = 11.sp) }
                        )
                    }
                }
                if (errorMsg != null) {
                    Text(errorMsg!!, color = LahoriRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!email.contains("@")) errorMsg = "Enter a valid email"
                    else onSave(email.trim().lowercase(), selectedBranchId)
                },
                colors = ButtonDefaults.buttonColors(containerColor = LahoriRed)
            ) {
                Text("ASSIGN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = MaterialTheme.colorScheme.onSurface) }
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
