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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalOffer
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BranchAdminEntry
import com.example.model.BranchDirectory
import com.example.model.CafeBranch
import com.example.model.CafeOrder
import com.example.model.MenuItem
import com.example.model.OrderReview
import com.example.model.OrderStatus
import com.example.model.OrderType
import com.example.model.Promotion
import com.example.model.SUPER_ADMIN_EMAIL
import com.example.model.SeedMenuData
import com.example.model.UiState
import com.example.model.UserRole
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
    isSavingStaff: Boolean = false,
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
        onDone: (String) -> Unit
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onDeleteBranch: (String) -> Unit = {},
    onSeedBranches: () -> Unit = {}
) {
    BackHandler(onBack = onBack)

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var branchFilter by remember { mutableStateOf("ALL") }
    var editingMenuItem by remember { mutableStateOf<MenuItem?>(null) }
    var showAddMenuDialog by remember { mutableStateOf(false) }
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
                            Text(
                                text = "${branches.size} Branches • Live Menu • Staff RBAC",
                                style = MaterialTheme.typography.bodySmall,
                                color = LahoriWhite.copy(alpha = 0.75f)
                            )
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
                                            onChangeStatus = { status -> onUpdateOrderStatus(order.id, status) }
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

                5 -> {
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
                        placeholder = { Text("e.g. Johar Town, Wapda Town, Township") },
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("branch_areas_input")
                    )

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

                                when {
                                    cleanName.length < 2 -> validationError = "Enter a branch name (at least 2 characters)."
                                    cleanLabel.length < 2 -> validationError = "Enter a short branch label (at least 2 characters)."
                                    cleanAddress.length < 3 -> validationError = "Enter a full branch address."
                                    else -> {
                                        validationError = null
                                        val isUpdate = editingBranchId != null
                                        val sortOrder = editingBranchId?.let { id -> branches.firstOrNull { it.id == id }?.sortOrder }
                                        onSaveBranch(editingBranchId, cleanName, cleanLabel, cleanAddress, parsedAreas, active, sortOrder) { savedId ->
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
    branches: List<CafeBranch> = BranchDirectory.defaultBranches
) {
    val isRestrictedRootMode = onSignOut != null
    if (!isRestrictedRootMode) {
        BackHandler(onBack = onBack)
    }

    var orderFilter by remember { mutableStateOf("ALL") }
    val branchLabel = remember(branchId, branches, branchName) {
        BranchDirectory.shortPickerLabel(branchId, branches)
            .ifBlank { branchName.substringAfter("— ").trim().ifBlank { branchId } }
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
                                    text = "$managerEmail • Live Orders Only",
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
                    HorizontalDivider(color = LahoriRed, thickness = 2.dp)
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
    var category by remember { mutableStateOf(initialItem?.category ?: "BURGERS") }
    var priceStr by remember { mutableStateOf(initialItem?.pricePkr?.toString() ?: "450") }
    var imageKey by remember { mutableStateOf(initialItem?.imageKey ?: "zinger_burger") }
    var badgeText by remember { mutableStateOf(initialItem?.badgeText ?: "NEW") }
    var available by remember { mutableStateOf(initialItem?.available ?: true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

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
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
