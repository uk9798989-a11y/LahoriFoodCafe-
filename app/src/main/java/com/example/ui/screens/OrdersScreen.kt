package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CafeOrder
import com.example.model.OrderReview
import com.example.model.OrderStatus
import com.example.model.OrderType
import com.example.model.UiState
import com.example.ui.components.formatPkr
import com.example.ui.theme.LahoriGold
import com.example.ui.theme.LahoriRed
import com.example.ui.theme.LahoriSuccess
import com.example.ui.theme.LahoriWarning
import com.example.ui.theme.LahoriWhite
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

private enum class OrderFilterTab {
    All,
    Active,
    Delivered
}

fun formatOrderDate(timestamp: Timestamp?): String {
    val date = (timestamp ?: Timestamp.now()).toDate()
    return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(date)
}

@Composable
fun OrdersScreen(
    ordersState: UiState<List<CafeOrder>>,
    isSignedIn: Boolean,
    onSignInRequested: () -> Unit,
    reviewsByOrderId: Map<String, OrderReview> = emptyMap(),
    onRateOrder: ((CafeOrder) -> Unit)? = null,
    onTrackOrder: ((CafeOrder) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedOrder by remember { mutableStateOf<CafeOrder?>(null) }
    var selectedFilter by remember { mutableStateOf(OrderFilterTab.All) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (!isSignedIn) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = LahoriRed, modifier = Modifier.size(44.dp))
                        Text("Track Your Live Orders", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = "Place a Guest Order from your Bucket or sign in with Google to view live kitchen preparation and delivery tracking.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        androidx.compose.material3.Button(
                            onClick = onSignInRequested,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = LahoriRed,
                                contentColor = LahoriWhite
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("SIGN IN WITH GOOGLE", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        } else {
            when (ordersState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = LahoriRed)
                    }
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(ordersState.message, color = LahoriRed, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is UiState.Success -> {
                    val orders = ordersState.data
                    val activeOrders = remember(orders) {
                        orders.filter { OrderStatus.fromString(it.status) != OrderStatus.Delivered }
                    }
                    val deliveredOrders = remember(orders) {
                        orders.filter { OrderStatus.fromString(it.status) == OrderStatus.Delivered }
                    }
                    val filteredOrders = remember(orders, activeOrders, deliveredOrders, selectedFilter) {
                        when (selectedFilter) {
                            OrderFilterTab.All -> orders
                            OrderFilterTab.Active -> activeOrders
                            OrderFilterTab.Delivered -> deliveredOrders
                        }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 110.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(key = "orders_header") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "LIVE ORDER TRACKER",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.weight(1f)
                                    )
                                    LiveSyncBadge(isActive = activeOrders.isNotEmpty())
                                }
                                Text(
                                    text = if (activeOrders.isNotEmpty()) {
                                        "${activeOrders.size} active order(s) syncing in real time • Tap Track Live for full milestone view"
                                    } else {
                                        "Real-time Firestore updates • Tap any order for full receipt & timeline"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (orders.isNotEmpty()) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val filterOptions = listOf(
                                            OrderFilterTab.All to "All (${orders.size})",
                                            OrderFilterTab.Active to "Active (${activeOrders.size})",
                                            OrderFilterTab.Delivered to "Delivered (${deliveredOrders.size})"
                                        )
                                        items(filterOptions, key = { it.first.name }) { (tab, label) ->
                                            FilterChip(
                                                selected = selectedFilter == tab,
                                                onClick = { selectedFilter = tab },
                                                label = {
                                                    Text(
                                                        text = label,
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                                    )
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = LahoriRed,
                                                    selectedLabelColor = LahoriWhite
                                                ),
                                                modifier = Modifier.testTag("orders_filter_${tab.name.lowercase()}")
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (orders.isEmpty()) {
                            item(key = "no_orders") {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Filled.DeliveryDining, contentDescription = null, tint = LahoriRed, modifier = Modifier.size(44.dp))
                                        Text("No orders placed yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                        Text(
                                            text = "Add a Zinger Burger or Extreme Pizza to your Bucket to see live branch tracking here.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else if (filteredOrders.isEmpty()) {
                            item(key = "no_filtered_orders") {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = LahoriSuccess, modifier = Modifier.size(38.dp))
                                        Text(
                                            text = if (selectedFilter == OrderFilterTab.Active) "No active orders right now" else "No delivered orders yet",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        TextButton(onClick = { selectedFilter = OrderFilterTab.All }) {
                                            Text("VIEW ALL ORDERS", color = LahoriRed)
                                        }
                                    }
                                }
                            }
                        } else {
                            items(filteredOrders, key = { it.id }) { order ->
                                OrderSummaryCard(
                                    order = order,
                                    showStepper = true,
                                    existingReview = reviewsByOrderId[order.id],
                                    onRateOrder = onRateOrder?.let { cb -> { cb(order) } },
                                    onTrackOrder = onTrackOrder?.let { cb -> { cb(order) } },
                                    onClick = { selectedOrder = order }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedOrder?.let { order ->
        OrderDetailsBottomSheet(
            order = order,
            existingReview = reviewsByOrderId[order.id],
            onRateOrder = onRateOrder?.let { cb ->
                {
                    selectedOrder = null
                    cb(order)
                }
            },
            onTrackLive = onTrackOrder?.let { cb ->
                {
                    selectedOrder = null
                    cb(order)
                }
            },
            onDismiss = { selectedOrder = null }
        )
    }
}

@Composable
fun OrderSummaryCard(
    order: CafeOrder,
    showStepper: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    existingReview: OrderReview? = null,
    onRateOrder: (() -> Unit)? = null,
    onTrackOrder: (() -> Unit)? = null
) {
    val status = OrderStatus.fromString(order.status)
    val isPickup = order.orderType.equals(OrderType.PICKUP.name, ignoreCase = true)
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val badgeColor = when (status) {
        OrderStatus.Confirmed -> if (isDark) Color(0xFFFF6B81) else LahoriRed
        OrderStatus.Preparing -> if (isDark) Color(0xFFFBBF24) else LahoriWarning
        OrderStatus.Dispatched -> if (isDark) Color(0xFF60A5FA) else Color(0xFF0066CC)
        OrderStatus.Delivered -> if (isDark) Color(0xFF4ADE80) else LahoriSuccess
    }
    val statusBadgeText = when (status) {
        OrderStatus.Dispatched -> if (isPickup) "READY FOR PICKUP" else status.displayTitle.uppercase()
        OrderStatus.Delivered -> if (isPickup) "PICKED UP" else status.displayTitle.uppercase()
        else -> status.displayTitle.uppercase()
    }
    val targetProgress = when (status) {
        OrderStatus.Confirmed -> 0.25f
        OrderStatus.Preparing -> 0.52f
        OrderStatus.Dispatched -> 0.80f
        OrderStatus.Delivered -> 1.0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "card_progress_${order.id}"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (status != OrderStatus.Delivered && showStepper) 1.5.dp else 1.dp,
            color = if (status != OrderStatus.Delivered && showStepper) LahoriRed.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline
        ),
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("order_card_${order.id}")
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "ORDER #${order.id.takeLast(6).uppercase()}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (order.guestCheckout && order.guestSessionId.isNotBlank()) {
                            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = order.guestSessionId.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = formatOrderDate(order.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = badgeColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = statusBadgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Text(
                text = order.itemsSummary,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (showStepper) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    color = if (status == OrderStatus.Delivered) LahoriSuccess else LahoriRed,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                )
                OrderStatusProgressBar(currentStatus = status, isPickup = isPickup)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Filled.Storefront, contentDescription = null, tint = LahoriRed, modifier = Modifier.size(15.dp))
                    Text(
                        text = "${order.orderType} • ${order.branchName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatPkr(order.totalPkr),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) Color(0xFFFF6B81) else LahoriRed
                )
            }

            if (onTrackOrder != null && status != OrderStatus.Delivered) {
                Button(
                    onClick = onTrackOrder,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LahoriRed,
                        contentColor = LahoriWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("track_live_order_${order.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TRACK LIVE PROGRESS",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (status == OrderStatus.Delivered && (existingReview != null || onRateOrder != null)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (existingReview != null) {
                        Surface(
                            color = LahoriGold.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = LahoriGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "RATED ${existingReview.rating}/5",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (onRateOrder != null) {
                            TextButton(
                                onClick = onRateOrder,
                                modifier = Modifier.testTag("edit_rate_order_${order.id}")
                            ) {
                                Text(
                                    text = "Edit Feedback",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) Color(0xFFFF8599) else LahoriRed
                                )
                            }
                        }
                    } else if (onRateOrder != null) {
                        OutlinedButton(
                            onClick = onRateOrder,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, LahoriRed),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isDark) MaterialTheme.colorScheme.onSurface else LahoriRed
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("rate_order_${order.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = LahoriGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = "RATE YOUR ORDER",
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderStatusProgressBar(
    currentStatus: OrderStatus,
    isPickup: Boolean = false
) {
    val steps = OrderStatus.entries
    val currentIndex = steps.indexOf(currentStatus)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { idx, step ->
            val reached = idx <= currentIndex
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (reached) LahoriRed else MaterialTheme.colorScheme.outline),
                    contentAlignment = Alignment.Center
                ) {
                    if (reached) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = LahoriWhite,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = when (step) {
                        OrderStatus.Confirmed -> "Confirmed"
                        OrderStatus.Preparing -> "Preparing"
                        OrderStatus.Dispatched -> if (isPickup) "Ready" else "Out"
                        OrderStatus.Delivered -> if (isPickup) "Picked Up" else "Delivered"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = if (reached) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsBottomSheet(
    order: CafeOrder,
    onDismiss: () -> Unit,
    existingReview: OrderReview? = null,
    onRateOrder: (() -> Unit)? = null,
    onTrackLive: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val status = OrderStatus.fromString(order.status)
    val isPickup = order.orderType.equals(OrderType.PICKUP.name, ignoreCase = true)
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

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
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ORDER #${order.id.takeLast(6).uppercase()}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatOrderDate(order.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close order details", tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            OrderStatusProgressBar(currentStatus = status, isPickup = isPickup)

            if (onTrackLive != null && status != OrderStatus.Delivered) {
                Button(
                    onClick = onTrackLive,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LahoriRed, contentColor = LahoriWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("details_track_live_button")
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("OPEN LIVE TRACKER", style = MaterialTheme.typography.labelLarge)
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "FULFILLMENT & BRANCH",
                        style = MaterialTheme.typography.labelSmall,
                        color = LahoriRed
                    )
                    Text(
                        text = "${order.orderType} • ${order.branchName} (${order.branchId})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = order.deliveryAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Recipient: ${order.customerName} (${order.customerPhone.ifBlank { "No phone" }})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (order.guestCheckout && order.guestSessionId.isNotBlank()) {
                        Text(
                            text = "Guest Reference ID: ${order.guestSessionId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = LahoriRed
                        )
                    }
                }
            }

            Text("ITEMS ORDERED", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            order.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${item.quantity}x ${item.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatPkr(item.lineTotalPkr),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Subtotal", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatPkr(order.subtotalPkr), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Delivery Charges", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = if (order.deliveryFeePkr == 0L) "Rs. 0 (FREE)" else formatPkr(order.deliveryFeePkr),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (order.deliveryFeePkr == 0L) {
                        if (isDark) Color(0xFF4ADE80) else LahoriSuccess
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            if (order.discountPkr > 0L) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Promo (${order.promoCode.ifBlank { "DISCOUNT" }})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) Color(0xFF4ADE80) else LahoriSuccess
                    )
                    Text(
                        text = "- ${formatPkr(order.discountPkr)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isDark) Color(0xFF4ADE80) else LahoriSuccess
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("TOTAL PAID", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(formatPkr(order.totalPkr), style = MaterialTheme.typography.titleLarge, color = LahoriRed)
            }

            if (status == OrderStatus.Delivered && (existingReview != null || onRateOrder != null)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                if (existingReview != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, LahoriGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Filled.Star, contentDescription = null, tint = LahoriGold, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "YOUR RATING: ${existingReview.rating} / 5",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (onRateOrder != null) {
                                    TextButton(onClick = onRateOrder) {
                                        Text("EDIT", style = MaterialTheme.typography.labelSmall, color = LahoriRed)
                                    }
                                }
                            }
                            if (existingReview.comment.isNotBlank()) {
                                Text(
                                    text = "\"${existingReview.comment}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else if (onRateOrder != null) {
                    Button(
                        onClick = onRateOrder,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LahoriRed, contentColor = LahoriWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("details_rate_order_button")
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = LahoriGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("RATE THIS DELIVERED ORDER", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
