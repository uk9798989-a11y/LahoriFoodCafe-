package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.OrderStatus
import com.example.model.OrderType

object OrderNotificationHelper {
    const val CHANNEL_ID = "lahori_order_updates"
    const val CHANNEL_BRANCH_ORDERS_ID = "lahori_branch_orders"
    const val EXTRA_TRACK_ORDER_ID = "track_order_id"
    const val EXTRA_NOTIFICATION_EVENT_TYPE = "notification_event_type"
    const val EXTRA_BRANCH_ID = "branch_id"
    const val EVENT_NEW_ORDER = "NEW_ORDER"
    const val EVENT_ORDER_STATUS = "ORDER_STATUS"

    private const val CHANNEL_NAME = "Order Status Updates"
    private const val CHANNEL_DESC = "Real-time Lahori Food Cafe order & pickup push alerts"
    private const val BRANCH_CHANNEL_NAME = "New Branch Orders"
    private const val BRANCH_CHANNEL_DESC = "Instant incoming customer order alerts for branch admins"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESC
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
            if (manager.getNotificationChannel(CHANNEL_BRANCH_ORDERS_ID) == null) {
                val branchChannel = NotificationChannel(
                    CHANNEL_BRANCH_ORDERS_ID,
                    BRANCH_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = BRANCH_CHANNEL_DESC
                    enableVibration(true)
                }
                manager.createNotificationChannel(branchChannel)
            }
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun notifyOrderStatus(
        context: Context,
        orderId: String,
        status: OrderStatus,
        branchName: String,
        itemsSummary: String,
        orderType: String = OrderType.DELIVERY.name
    ) {
        val statusTitle = status.notificationTitleFor(orderType)
        val statusMsg = status.notificationMessageFor(orderType)
        val shortId = orderId.takeLast(6).uppercase()
        val fullTitle = "Lahori Food Cafe — $statusTitle"
        val expandedBody = "$statusMsg ($branchName • #$shortId: $itemsSummary)"

        postNotification(
            context = context,
            channelId = CHANNEL_ID,
            notificationId = if (orderId.isNotBlank()) "order_status_$orderId".hashCode() else 1001,
            orderId = orderId,
            eventType = EVENT_ORDER_STATUS,
            branchId = "",
            title = fullTitle,
            shortText = statusMsg,
            expandedText = expandedBody
        )
    }

    fun notifyNewBranchOrder(
        context: Context,
        orderId: String,
        branchId: String,
        branchName: String,
        customerName: String,
        orderType: String,
        totalPkr: Long,
        itemsSummary: String,
        customTitle: String? = null,
        customBody: String? = null
    ) {
        val shortId = orderId.takeLast(6).uppercase()
        val branchDisplay = branchName.substringAfter("— ").trim().ifBlank { branchName.ifBlank { branchId } }
        val resolvedTitle = customTitle?.takeIf { it.isNotBlank() }
            ?: "New $orderType Order #$shortId — $branchDisplay"
        val shortBody = customBody?.takeIf { it.isNotBlank() }
            ?: "${customerName.ifBlank { "Customer" }} • Rs. $totalPkr • ${itemsSummary.ifBlank { "New branch order" }}"
        val expandedBody = if (customBody.isNullOrBlank()) {
            "$shortBody ($branchName)"
        } else {
            "$customBody ($branchName • #$shortId)"
        }

        postNotification(
            context = context,
            channelId = CHANNEL_BRANCH_ORDERS_ID,
            notificationId = if (orderId.isNotBlank()) "new_order_$orderId".hashCode() else 2001,
            orderId = orderId,
            eventType = EVENT_NEW_ORDER,
            branchId = branchId,
            title = resolvedTitle,
            shortText = shortBody,
            expandedText = expandedBody
        )
    }

    fun notifyRemoteMessage(
        context: Context,
        title: String?,
        body: String?,
        orderId: String,
        orderType: String,
        statusRaw: String,
        branchName: String,
        itemsSummary: String,
        eventType: String = EVENT_ORDER_STATUS,
        branchId: String = "",
        customerName: String = "",
        totalPkr: Long = 0L
    ) {
        if (eventType.equals(EVENT_NEW_ORDER, ignoreCase = true)) {
            notifyNewBranchOrder(
                context = context,
                orderId = orderId,
                branchId = branchId,
                branchName = branchName,
                customerName = customerName,
                orderType = orderType.ifBlank { OrderType.DELIVERY.name },
                totalPkr = totalPkr,
                itemsSummary = itemsSummary,
                customTitle = title,
                customBody = body
            )
            return
        }

        val status = OrderStatus.fromString(statusRaw.ifBlank { OrderStatus.Dispatched.name })
        val resolvedTitle = title?.takeIf { it.isNotBlank() }
            ?: "Lahori Food Cafe — ${status.notificationTitleFor(orderType)}"
        val resolvedBody = body?.takeIf { it.isNotBlank() }
            ?: status.notificationMessageFor(orderType)
        val shortId = orderId.takeLast(6).uppercase()
        val detailsSuffix = buildList {
            if (branchName.isNotBlank()) add(branchName)
            if (shortId.isNotBlank()) add("#$shortId")
            if (itemsSummary.isNotBlank()) add(itemsSummary)
        }.joinToString(" • ")
        val expanded = if (detailsSuffix.isNotBlank()) "$resolvedBody ($detailsSuffix)" else resolvedBody

        postNotification(
            context = context,
            channelId = CHANNEL_ID,
            notificationId = if (orderId.isNotBlank()) "order_status_$orderId".hashCode() else 1001,
            orderId = orderId,
            eventType = EVENT_ORDER_STATUS,
            branchId = branchId,
            title = resolvedTitle,
            shortText = resolvedBody,
            expandedText = expanded
        )
    }

    private fun postNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        orderId: String,
        eventType: String,
        branchId: String,
        title: String,
        shortText: String,
        expandedText: String
    ) {
        ensureChannel(context)
        if (!hasNotificationPermission(context)) return

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (orderId.isNotBlank()) {
                putExtra(EXTRA_TRACK_ORDER_ID, orderId)
            }
            putExtra(EXTRA_NOTIFICATION_EVENT_TYPE, eventType)
            if (branchId.isNotBlank()) {
                putExtra(EXTRA_BRANCH_ID, branchId)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_lahori_notification)
            .setColor(ContextCompat.getColor(context, R.color.lahori_red))
            .setContentTitle(title)
            .setContentText(shortText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission revoked concurrently
        }
    }
}
