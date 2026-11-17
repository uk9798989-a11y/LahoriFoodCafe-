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
    const val EXTRA_TRACK_ORDER_ID = "track_order_id"

    private const val CHANNEL_NAME = "Order Status Updates"
    private const val CHANNEL_DESC = "Real-time Lahori Food Cafe order & pickup push alerts"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
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
            orderId = orderId,
            title = fullTitle,
            shortText = statusMsg,
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
        itemsSummary: String
    ) {
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
            orderId = orderId,
            title = resolvedTitle,
            shortText = resolvedBody,
            expandedText = expanded
        )
    }

    private fun postNotification(
        context: Context,
        orderId: String,
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
        }
        val requestCode = if (orderId.isNotBlank()) orderId.hashCode() else 1001
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
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
            NotificationManagerCompat.from(context).notify(requestCode, notification)
        } catch (_: SecurityException) {
            // Permission revoked concurrently
        }
    }
}
