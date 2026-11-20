package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.LocationHelper
import com.example.model.AdminDeliveryNotification
import com.example.model.CafeOrder
import com.example.model.OrderStatus
import com.example.ui.components.formatPkr
import com.example.ui.theme.LahoriBlack
import com.example.ui.theme.LahoriDarkCard
import com.example.ui.theme.LahoriGold
import com.example.ui.theme.LahoriRed
import com.example.ui.theme.LahoriSuccess
import com.example.ui.theme.LahoriWhite

/**
 * Instant Real-Time Admin Popup displayed immediately when any order transitions to "Delivered".
 * Appears across every tab of Super Admin & Branch Admin dashboards and queues multiple deliveries cleanly.
 */
@Composable
fun OrderDeliveredPopupModal(
    notification: AdminDeliveryNotification,
    remainingInQueue: Int,
    onViewOrder: (AdminDeliveryNotification) -> Unit,
    onDismiss: () -> Unit
) {
    var visible by remember(notification.id) { mutableStateOf(false) }
    LaunchedEffect(notification.id) {
        visible = true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + scaleIn(
                    initialScale = 0.88f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = LahoriBlack,
                    border = BorderStroke(2.dp, LahoriGold),
                    shadowElevation = 18.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("order_delivered_popup_modal")
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Top Lahori Festive Header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(LahoriBlack, Color(0xFF3A0710), LahoriRed)
                                    )
                                )
                                .padding(horizontal = 18.dp, vertical = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = LahoriGold,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.CheckCircle,
                                                contentDescription = null,
                                                tint = LahoriBlack,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = "🎉 Order Successfully Delivered!",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 16.sp
                                            ),
                                            color = LahoriWhite
                                        )
                                        if (remainingInQueue > 0) {
                                            Text(
                                                text = "+$remainingInQueue more delivered notification${if (remainingInQueue == 1) "" else "s"} waiting",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = LahoriGold
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Close popup",
                                        tint = LahoriWhite
                                    )
                                }
                            }
                        }

                        // Details Body
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "The order has been successfully delivered to the customer.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = LahoriWhite.copy(alpha = 0.9f)
                            )

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = LahoriDarkCard,
                                border = BorderStroke(1.dp, LahoriGold.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PopupInfoRow(
                                        icon = Icons.Filled.ReceiptLong,
                                        label = "Order Number",
                                        value = notification.orderNumber.ifBlank { "#${notification.orderId.takeLast(6).uppercase()}" },
                                        valueColor = LahoriGold
                                    )
                                    PopupInfoRow(
                                        icon = Icons.Filled.Person,
                                        label = "Customer Name",
                                        value = notification.customerName.ifBlank { "Customer" }
                                    )
                                    PopupInfoRow(
                                        icon = Icons.Filled.Phone,
                                        label = "Customer Phone",
                                        value = notification.customerPhone.ifBlank { "Not provided" }
                                    )
                                    PopupInfoRow(
                                        icon = Icons.Filled.Storefront,
                                        label = "Total Order Amount",
                                        value = "${formatPkr(notification.totalPkr)} • Delivery: Rs. 0 (FREE)",
                                        valueColor = Color(0xFF4ADE80)
                                    )
                                    PopupInfoRow(
                                        icon = Icons.Filled.LocationOn,
                                        label = "Delivery Address",
                                        value = notification.deliveryAddress.ifBlank { notification.branchName }
                                    )
                                    PopupInfoRow(
                                        icon = Icons.Filled.Schedule,
                                        label = "Delivery Time",
                                        value = formatOrderDate(notification.deliveredAt ?: notification.createdAt)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.5.dp, LahoriWhite.copy(alpha = 0.45f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LahoriWhite),
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .height(48.dp)
                                        .testTag("delivered_popup_dismiss_btn")
                                ) {
                                    Text(
                                        text = "Dismiss",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }

                                Button(
                                    onClick = { onViewOrder(notification) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LahoriRed,
                                        contentColor = LahoriWhite
                                    ),
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .height(48.dp)
                                        .testTag("delivered_popup_view_order_btn")
                                ) {
                                    Text(
                                        text = "View Order",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black)
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

@Composable
private fun PopupInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = LahoriWhite
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = LahoriGold,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = LahoriWhite.copy(alpha = 0.62f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = valueColor
            )
        }
    }
}

/**
 * Full Order Details dialog for SuperAdmin & BranchAdmin, reachable from any Order card,
 * the Delivered Popup "View Order" button, or any item in the Admin Notifications History.
 */
@Composable
fun AdminOrderDetailDialog(
    order: CafeOrder?,
    notificationFallback: AdminDeliveryNotification?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val orderNumber = order?.let { "#${it.id.takeLast(6).uppercase()}" }
        ?: notificationFallback?.orderNumber
        ?: "#ORDER"
    val customerName = order?.customerName ?: notificationFallback?.customerName ?: "Customer"
    val customerPhone = order?.customerPhone ?: notificationFallback?.customerPhone ?: ""
    val branchName = order?.branchName ?: notificationFallback?.branchName ?: ""
    val deliveryAddress = order?.deliveryAddress ?: notificationFallback?.deliveryAddress ?: ""
    val statusText = order?.let { OrderStatus.fromString(it.status).displayTitle } ?: "Delivered"
    val timestamp = order?.updatedAt ?: order?.createdAt ?: notificationFallback?.deliveredAt ?: notificationFallback?.createdAt
    val totalPkr = order?.totalPkr ?: notificationFallback?.totalPkr ?: 0L
    val subtotalPkr = order?.subtotalPkr ?: totalPkr
    val deliveryFeePkr = order?.deliveryFeePkr ?: 0L
    val discountPkr = order?.discountPkr ?: 0L
    val promoCode = order?.promoCode ?: ""
    val lat = order?.deliveryLatitude ?: 0.0
    val lng = order?.deliveryLongitude ?: 0.0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = LahoriBlack,
            border = BorderStroke(1.5.dp, LahoriGold),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .testTag("admin_order_detail_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ORDER DETAILS $orderNumber",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = LahoriGold
                        )
                        Text(
                            text = "$statusText • ${formatOrderDate(timestamp)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4ADE80)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close order details", tint = LahoriWhite)
                    }
                }

                // Customer & Address Block
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = LahoriDarkCard,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PopupInfoRow(Icons.Filled.Storefront, "Branch", branchName.ifBlank { "Lahori Food Cafe" })
                        PopupInfoRow(Icons.Filled.Person, "Customer", customerName)
                        PopupInfoRow(Icons.Filled.Phone, "Phone", customerPhone.ifBlank { "Not provided" })
                        PopupInfoRow(Icons.Filled.LocationOn, "Delivery Address", deliveryAddress.ifBlank { "Pickup" })

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (customerPhone.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { LocationHelper.dialCustomerPhone(context, customerPhone) },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, LahoriGold),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LahoriGold),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                ) {
                                    Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("CALL", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                            if (deliveryAddress.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        LocationHelper.openCustomerLocationInMaps(
                                            context = context,
                                            latitude = lat,
                                            longitude = lng,
                                            address = deliveryAddress,
                                            customerName = customerName
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, LahoriWhite.copy(alpha = 0.45f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LahoriWhite),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                ) {
                                    Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("OPEN MAP", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }

                // Items & Pricing Block (always displaying Delivery Charges: Rs. 0 (FREE))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = LahoriDarkCard,
                    border = BorderStroke(1.dp, LahoriGold.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "ITEMS & PAYMENT SUMMARY",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                            color = LahoriGold
                        )

                        if (order != null && order.items.isNotEmpty()) {
                            order.items.forEach { line ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${line.quantity}x ${line.name}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LahoriWhite,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = formatPkr(line.lineTotalPkr),
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = LahoriWhite
                                    )
                                }
                            }
                        } else if (!notificationFallback?.itemsSummary.isNullOrBlank()) {
                            Text(
                                text = notificationFallback?.itemsSummary.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = LahoriWhite
                            )
                        }

                        HorizontalDivider(color = LahoriWhite.copy(alpha = 0.14f), modifier = Modifier.padding(vertical = 4.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal", style = MaterialTheme.typography.bodySmall, color = LahoriWhite.copy(alpha = 0.72f))
                            Text(formatPkr(subtotalPkr), style = MaterialTheme.typography.bodySmall, color = LahoriWhite)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Delivery Charges", style = MaterialTheme.typography.bodySmall, color = LahoriWhite.copy(alpha = 0.72f))
                            Text(
                                text = if (deliveryFeePkr == 0L) "Rs. 0 (FREE)" else formatPkr(deliveryFeePkr),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black),
                                color = LahoriGold
                            )
                        }
                        if (discountPkr > 0L) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Promo ($promoCode)", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4ADE80))
                                Text("- ${formatPkr(discountPkr)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF4ADE80))
                            }
                        }
                        HorizontalDivider(color = LahoriWhite.copy(alpha = 0.16f), modifier = Modifier.padding(vertical = 2.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TOTAL", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = LahoriWhite)
                            Text(formatPkr(totalPkr), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = LahoriGold)
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LahoriRed, contentColor = LahoriWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("CLOSE", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black))
                }
            }
        }
    }
}

/**
 * Persistent Admin Notifications History tab for Super Admin & Branch Admin dashboards.
 * Shows delivered order notifications with read/unread indicators, sound toggle, and click-to-open order details.
 */
@Composable
fun AdminNotificationsHistoryTab(
    notifications: List<AdminDeliveryNotification>,
    soundEnabled: Boolean,
    onToggleSound: (Boolean) -> Unit,
    onOpenNotification: (AdminDeliveryNotification) -> Unit,
    onMarkAllRead: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unreadCount = remember(notifications) { notifications.count { !it.read } }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_notifications_history_list")
    ) {
        // Controls & Sound Toggle Card (Lahori Black + Gold branding)
        item(key = "notifications_header_card") {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = LahoriBlack,
                border = BorderStroke(1.2.dp, LahoriGold.copy(alpha = 0.7f)),
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = LahoriGold,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "DELIVERED ORDER ALERTS",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                    color = LahoriWhite
                                )
                                Text(
                                    text = if (unreadCount > 0) "$unreadCount unread • ${notifications.size} total" else "All ${notifications.size} notifications read",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LahoriGold
                                )
                            }
                        }

                        if (unreadCount > 0) {
                            TextButton(
                                onClick = onMarkAllRead,
                                modifier = Modifier.testTag("admin_mark_all_read_btn")
                            ) {
                                Icon(Icons.Filled.DoneAll, contentDescription = null, tint = LahoriGold, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "MARK ALL READ",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                    color = LahoriGold
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = LahoriWhite.copy(alpha = 0.14f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (soundEnabled) Icons.Filled.VolumeUp else Icons.Filled.NotificationsOff,
                                contentDescription = null,
                                tint = if (soundEnabled) Color(0xFF4ADE80) else LahoriWhite.copy(alpha = 0.55f),
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Notification Alert Sound",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = LahoriWhite
                                )
                                Text(
                                    text = if (soundEnabled) "Plays chime when an order is delivered" else "Sound alerts muted",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LahoriWhite.copy(alpha = 0.68f)
                                )
                            }
                        }
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = onToggleSound,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = LahoriBlack,
                                checkedTrackColor = LahoriGold,
                                uncheckedThumbColor = LahoriWhite,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.testTag("admin_sound_toggle")
                        )
                    }
                }
            }
        }

        if (notifications.isEmpty()) {
            item(key = "empty_notifications_card") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = LahoriBlack,
                    border = BorderStroke(1.dp, LahoriGold.copy(alpha = 0.28f)),
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
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = LahoriGold.copy(alpha = 0.7f),
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "No Delivered Order Notifications Yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LahoriWhite
                        )
                        Text(
                            text = "As soon as an order is marked Delivered, an instant popup and persistent alert record will appear here in real time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LahoriWhite.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        } else {
            items(notifications, key = { it.id }) { notif ->
                val isUnread = !notif.read
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isUnread) LahoriBlack else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (isUnread) 1.5.dp else 1.dp,
                        color = if (isUnread) LahoriGold else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenNotification(notif) }
                        .testTag("admin_notification_item_${notif.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isUnread) LahoriRed else LahoriSuccess.copy(alpha = 0.18f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = if (isUnread) LahoriWhite else LahoriSuccess,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Order ${notif.orderNumber.ifBlank { "#${notif.orderId.takeLast(6).uppercase()}" }} Delivered",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                    color = if (isUnread) LahoriGold else MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = if (isUnread) LahoriRed else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = if (isUnread) "UNREAD" else "READ",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        ),
                                        color = if (isUnread) LahoriWhite else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Text(
                                text = "${notif.customerName} • ${formatPkr(notif.totalPkr)} • Delivery Rs. 0",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isUnread) LahoriWhite else MaterialTheme.colorScheme.onSurface
                            )

                            if (notif.deliveryAddress.isNotBlank()) {
                                Text(
                                    text = notif.deliveryAddress,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isUnread) LahoriWhite.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatOrderDate(notif.deliveredAt ?: notif.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isUnread) LahoriGold.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Tap to view order →",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isUnread) LahoriGold else LahoriRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
