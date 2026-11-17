package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CafeOrder
import com.example.model.OrderReview
import com.example.model.OrderStatus
import com.example.model.OrderType
import com.example.model.UiState
import com.example.ui.components.formatPkr
import com.example.ui.theme.LahoriBlack
import com.example.ui.theme.LahoriGold
import com.example.ui.theme.LahoriRed
import com.example.ui.theme.LahoriRedDark
import com.example.ui.theme.LahoriSuccess
import com.example.ui.theme.LahoriWhite

@Composable
fun LiveOrderTrackingScreen(
    trackedOrderState: UiState<CafeOrder>,
    allActiveOrders: List<CafeOrder>,
    reviewsByOrderId: Map<String, OrderReview>,
    onSelectTrackedOrder: (String) -> Unit,
    onRateOrder: (CafeOrder) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp
            ) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("live_tracker_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Orders",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column {
                                Text(
                                    text = "LIVE ORDER TRACKER",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Real-time Firestore branch sync",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (trackedOrderState is UiState.Success) {
                            val active = OrderStatus.fromString(trackedOrderState.data.status) != OrderStatus.Delivered
                            LiveSyncBadge(isActive = active)
                        }
                    }

                    if (allActiveOrders.size > 1 && trackedOrderState is UiState.Success) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(allActiveOrders, key = { it.id }) { candidate ->
                                val isCurrent = candidate.id == trackedOrderState.data.id
                                FilterChip(
                                    selected = isCurrent,
                                    onClick = { onSelectTrackedOrder(candidate.id) },
                                    label = {
                                        Text(
                                            text = "#${candidate.id.takeLast(6).uppercase()} • ${OrderStatus.fromString(candidate.status).name}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LahoriRed,
                                        selectedLabelColor = LahoriWhite
                                    )
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            when (trackedOrderState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = LahoriRed)
                            Text(
                                text = "Connecting to real-time order updates...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, LahoriRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Unable to Track Order",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = LahoriRed
                                )
                                Text(
                                    text = trackedOrderState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = onBack,
                                    colors = ButtonDefaults.buttonColors(containerColor = LahoriRed, contentColor = LahoriWhite),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text("BACK TO ORDERS")
                                }
                            }
                        }
                    }
                }

                is UiState.Success -> {
                    val order = trackedOrderState.data
                    val status = OrderStatus.fromString(order.status)
                    val isPickup = order.orderType.equals(OrderType.PICKUP.name, ignoreCase = true)
                    val existingReview = reviewsByOrderId[order.id]

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("live_order_tracking_list"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Hero Live Status & Animated Progress Card
                        item(key = "hero_live_status") {
                            LiveOrderHeroCard(
                                order = order,
                                status = status,
                                isPickup = isPickup
                            )
                        }

                        // 2. Step-by-Step Vertical Live Timeline
                        item(key = "vertical_milestone_timeline") {
                            LiveMilestoneTimelineCard(
                                currentStatus = status,
                                isPickup = isPickup,
                                isDark = isDark
                            )
                        }

                        // 3. Branch & Fulfillment Details
                        item(key = "branch_fulfillment_card") {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Storefront,
                                            contentDescription = null,
                                            tint = LahoriRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "BRANCH & DELIVERY INFO",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = "${order.orderType} • ${order.branchName}",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = order.deliveryAddress,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Customer: ${order.customerName} (${order.customerPhone.ifBlank { "Verified account" }})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Itemized Order Summary
                        item(key = "order_items_receipt") {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "ORDER SUMMARY (#${order.id.takeLast(6).uppercase()})",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                                    order.items.forEach { line ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${line.quantity}x ${line.name}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = formatPkr(line.lineTotalPkr),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Subtotal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(formatPkr(order.subtotalPkr), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Delivery Fee", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = if (order.deliveryFeePkr == 0L) "FREE" else formatPkr(order.deliveryFeePkr),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (order.discountPkr > 0L) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Promo (${order.promoCode.ifBlank { "DISCOUNT" }})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isDark) Color(0xFF4ADE80) else LahoriSuccess
                                            )
                                            Text(
                                                text = "- ${formatPkr(order.discountPkr)}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (isDark) Color(0xFF4ADE80) else LahoriSuccess
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "TOTAL",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = formatPkr(order.totalPkr),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                            color = LahoriRed
                                        )
                                    }
                                }
                            }
                        }

                        // 5. Delivered Rating Card
                        if (status == OrderStatus.Delivered) {
                            item(key = "delivered_review_section") {
                                if (existingReview != null) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, LahoriGold),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                                    Icon(Icons.Filled.Star, contentDescription = null, tint = LahoriGold)
                                                    Text(
                                                        text = "RATED ${existingReview.rating}/5",
                                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                TextButton(onClick = { onRateOrder(order) }) {
                                                    Text("EDIT", color = LahoriRed)
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
                                } else {
                                    Button(
                                        onClick = { onRateOrder(order) },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = LahoriRed, contentColor = LahoriWhite),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("tracker_rate_order_button")
                                    ) {
                                        Icon(Icons.Filled.Star, contentDescription = null, tint = LahoriGold)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "RATE YOUR DELIVERED ORDER",
                                            style = MaterialTheme.typography.labelLarge
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
}

@Composable
fun LiveSyncBadge(isActive: Boolean) {
    val transition = rememberInfiniteTransition(label = "live_pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val badgeTint = if (isActive) LahoriSuccess else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = CircleShape,
        color = badgeTint.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, badgeTint.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .scale(if (isActive) pulseScale else 1f)
                    .alpha(if (isActive) pulseAlpha else 1f)
                    .clip(CircleShape)
                    .background(badgeTint)
            )
            Text(
                text = if (isActive) "LIVE" else "COMPLETED",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp
                ),
                color = badgeTint
            )
        }
    }
}

@Composable
private fun LiveOrderHeroCard(
    order: CafeOrder,
    status: OrderStatus,
    isPickup: Boolean
) {
    val progressTarget = when (status) {
        OrderStatus.Confirmed -> 0.25f
        OrderStatus.Preparing -> 0.52f
        OrderStatus.Dispatched -> 0.80f
        OrderStatus.Delivered -> 1.0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "order_progress"
    )

    val stageHeadline = when (status) {
        OrderStatus.Confirmed -> "ORDER CONFIRMED"
        OrderStatus.Preparing -> "PREPARING IN KITCHEN"
        OrderStatus.Dispatched -> if (isPickup) "READY FOR PICKUP" else "OUT FOR DELIVERY"
        OrderStatus.Delivered -> if (isPickup) "PICKED UP" else "DELIVERED"
    }
    val stageSubtitle = when (status) {
        OrderStatus.Confirmed -> "Your order #${order.id.takeLast(6).uppercase()} is locked in at ${order.branchName}."
        OrderStatus.Preparing -> "Our kitchen team is freshly frying & baking your Lahori favorites."
        OrderStatus.Dispatched -> if (isPickup) {
            "Your meal is packed hot and waiting at the branch pickup counter!"
        } else {
            "Your rider has left the branch and is heading toward your address!"
        }
        OrderStatus.Delivered -> "Enjoy your Lahori Food Cafe feast! Thank you for ordering with us."
    }
    val stageIcon: ImageVector = when (status) {
        OrderStatus.Confirmed -> Icons.Filled.ReceiptLong
        OrderStatus.Preparing -> Icons.Filled.LocalFireDepartment
        OrderStatus.Dispatched -> if (isPickup) Icons.Filled.ShoppingBag else Icons.Filled.DeliveryDining
        OrderStatus.Delivered -> Icons.Filled.CheckCircle
    }

    val infiniteTransition = rememberInfiniteTransition(label = "hero_beacon")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (status == OrderStatus.Delivered) 1.0f else 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_scale"
    )

    val gradientColors = if (status == OrderStatus.Delivered) {
        listOf(Color(0xFF0E4D27), Color(0xFF167B40), LahoriSuccess)
    } else {
        listOf(LahoriBlack, Color(0xFF560615), LahoriRedDark, LahoriRed)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("live_order_hero_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(colors = gradientColors))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            color = LahoriGold,
                            shape = CircleShape
                        ) {
                            Text(
                                text = "ORDER #${order.id.takeLast(6).uppercase()}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = LahoriBlack,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = stageHeadline,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                            color = LahoriWhite
                        )
                        Text(
                            text = stageSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = LahoriWhite.copy(alpha = 0.88f)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Surface(
                        shape = CircleShape,
                        color = LahoriWhite.copy(alpha = 0.16f),
                        border = BorderStroke(1.5.dp, LahoriWhite.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .size(64.dp)
                            .scale(iconScale)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = stageIcon,
                                contentDescription = stageHeadline,
                                tint = LahoriWhite,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = LahoriGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Updated ${formatOrderDate(order.updatedAt ?: order.createdAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = LahoriWhite.copy(alpha = 0.85f)
                            )
                        }
                        Text(
                            text = "${(animatedProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                            color = LahoriGold
                        )
                    }

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        color = LahoriGold,
                        trackColor = LahoriWhite.copy(alpha = 0.25f),
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveMilestoneTimelineCard(
    currentStatus: OrderStatus,
    isPickup: Boolean,
    isDark: Boolean
) {
    val steps = OrderStatus.entries
    val currentIndex = steps.indexOf(currentStatus)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("live_milestone_timeline")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "REAL-TIME PROGRESS",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            steps.forEachIndexed { index, step ->
                val isCompleted = index < currentIndex || (index == currentIndex && currentStatus == OrderStatus.Delivered)
                val isCurrent = index == currentIndex && currentStatus != OrderStatus.Delivered
                val reached = index <= currentIndex

                val title = when (step) {
                    OrderStatus.Confirmed -> "Order Confirmed"
                    OrderStatus.Preparing -> "Preparing in Kitchen"
                    OrderStatus.Dispatched -> if (isPickup) "Ready for Pickup" else "Out for Delivery"
                    OrderStatus.Delivered -> if (isPickup) "Picked Up" else "Delivered"
                }
                val detail = when (step) {
                    OrderStatus.Confirmed -> "Order verified and sent to kitchen display."
                    OrderStatus.Preparing -> "Chefs are cooking your items piping hot."
                    OrderStatus.Dispatched -> if (isPickup) {
                        "Packed & waiting at the branch counter."
                    } else {
                        "Rider picked up your order and is en route."
                    }
                    OrderStatus.Delivered -> if (isPickup) {
                        "Order collected from branch."
                    } else {
                        "Delivered to your doorstep!"
                    }
                }
                val stepIcon = when (step) {
                    OrderStatus.Confirmed -> Icons.Filled.ReceiptLong
                    OrderStatus.Preparing -> Icons.Filled.LocalFireDepartment
                    OrderStatus.Dispatched -> if (isPickup) Icons.Filled.ShoppingBag else Icons.Filled.DeliveryDining
                    OrderStatus.Delivered -> Icons.Filled.CheckCircle
                }

                val circleColor by animateColorAsState(
                    targetValue = when {
                        isCompleted -> LahoriSuccess
                        isCurrent -> LahoriRed
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    label = "step_color_$index"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = circleColor,
                            border = if (reached) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = stepIcon,
                                    contentDescription = title,
                                    tint = if (reached) LahoriWhite else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (index < steps.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(34.dp)
                                    .background(
                                        if (index < currentIndex) LahoriSuccess
                                        else MaterialTheme.colorScheme.outline
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 2.dp, bottom = if (index < steps.lastIndex) 12.dp else 0.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = if (reached) FontWeight.ExtraBold else FontWeight.Medium
                                ),
                                color = if (reached) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isCurrent) {
                                Surface(
                                    color = LahoriRed.copy(alpha = 0.14f),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "IN PROGRESS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        ),
                                        color = if (isDark) Color(0xFFFF8599) else LahoriRed,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            } else if (isCompleted) {
                                Text(
                                    text = "DONE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (isDark) Color(0xFF4ADE80) else LahoriSuccess
                                )
                            }
                        }
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
