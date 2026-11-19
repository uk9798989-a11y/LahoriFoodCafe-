package com.example.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
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
    const val CHANNEL_BRANCH_ORDERS_ID = "lahori_admin_instant_orders_v2"
    const val CHANNEL_ADMIN_MONITOR_ID = "lahori_admin_live_monitor"
    const val EXTRA_TRACK_ORDER_ID = "track_order_id"
    const val EXTRA_NOTIFICATION_EVENT_TYPE = "notification_event_type"
    const val EXTRA_BRANCH_ID = "branch_id"
    const val EVENT_NEW_ORDER = "NEW_ORDER"
    const val EVENT_ORDER_STATUS = "ORDER_STATUS"

    private const val CHANNEL_NAME = "Order Status Updates"
    private const val CHANNEL_DESC = "Real-time Lahori Food Cafe order & pickup push alerts"
    private const val BRANCH_CHANNEL_NAME = "Instant New Order Alerts (Admin)"
    private const val BRANCH_CHANNEL_DESC = "Loud heads-up alerts with sound & vibration when customers place orders"
    private const val MONITOR_CHANNEL_NAME = "Admin Live Order Listener"
    private const val MONITOR_CHANNEL_DESC = "Keeps real-time new order alerts active when the app is in the background"

    private const val PREFS_NOTIFIED = "lahori_notified_orders_prefs"
    private const val KEY_NOTIFIED_IDS = "notified_order_ids_csv"
    private const val MAX_TRACKED_NOTIFIED_IDS = 120
    private val ADMIN_VIBRATION_PATTERN = longArrayOf(0L, 350L, 160L, 350L, 160L, 480L)

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESC
                    enableVibration(true)
                    setSound(defaultSound, audioAttrs)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
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
                    enableLights(true)
                    enableVibration(true)
                    vibrationPattern = ADMIN_VIBRATION_PATTERN
                    setSound(defaultSound, audioAttrs)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                manager.createNotificationChannel(branchChannel)
            }

            if (manager.getNotificationChannel(CHANNEL_ADMIN_MONITOR_ID) == null) {
                val monitorChannel = NotificationChannel(
                    CHANNEL_ADMIN_MONITOR_ID,
                    MONITOR_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = MONITOR_CHANNEL_DESC
                    setShowBadge(false)
                    enableVibration(false)
                    setSound(null, null)
                }
                manager.createNotificationChannel(monitorChannel)
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

    @Synchronized
    fun markOrderNotifiedIfNew(context: Context, orderId: String): Boolean {
        val cleanId = orderId.trim()
        if (cleanId.isBlank()) return true
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NOTIFIED, Context.MODE_PRIVATE)
        val existingList = prefs.getString(KEY_NOTIFIED_IDS, "")
            .orEmpty()
            .split(",")
            .filter { it.isNotBlank() }
            .toMutableList()
        if (existingList.contains(cleanId)) {
            return false
        }
        existingList.add(cleanId)
        val trimmed = if (existingList.size > MAX_TRACKED_NOTIFIED_IDS) {
            existingList.takeLast(MAX_TRACKED_NOTIFIED_IDS)
        } else {
            existingList
        }
        prefs.edit().putString(KEY_NOTIFIED_IDS, trimmed.joinToString(",")).apply()
        return true
    }

    fun buildAdminMonitorForegroundNotification(
        context: Context,
        roleLabel: String,
        branchLabel: String,
        stopPendingIntent: PendingIntent
    ): Notification {
        ensureChannel(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            9001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_ADMIN_MONITOR_ID)
            .setSmallIcon(R.drawable.ic_stat_lahori_notification)
            .setColor(ContextCompat.getColor(context, R.color.lahori_red))
            .setContentTitle("Lahori Food Cafe • Live Order Alerts Active")
            .setContentText("$roleLabel ($branchLabel) — Ready for instant order notifications")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openPendingIntent)
            .addAction(0, "Stop Alerts", stopPendingIntent)
            .build()
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
        customerPhone: String = "",
        deliveryAddress: String = "",
        deliveryLatitude: Double = 0.0,
        deliveryLongitude: Double = 0.0,
        customTitle: String? = null,
        customBody: String? = null,
        skipDedupe: Boolean = false
    ) {
        if (!skipDedupe && orderId.isNotBlank() && !markOrderNotifiedIfNew(context, orderId)) {
            return
        }
        val shortId = orderId.takeLast(6).uppercase()
        val branchDisplay = branchName.substringAfter("— ").trim().ifBlank { branchName.ifBlank { branchId } }
        val isDelivery = orderType.equals(OrderType.DELIVERY.name, ignoreCase = true)
        val hasGps = isDelivery &&
            (deliveryLatitude != 0.0 || deliveryLongitude != 0.0) &&
            deliveryLatitude in -90.0..90.0 &&
            deliveryLongitude in -180.0..180.0

        val resolvedTitle = customTitle?.takeIf { it.isNotBlank() }
            ?: "NEW $orderType ORDER #$shortId — $branchDisplay"
        val shortBody = customBody?.takeIf { it.isNotBlank() }
            ?: "${customerName.ifBlank { "Customer" }} • Rs. $totalPkr • ${itemsSummary.ifBlank { "New branch order" }}"

        val locationLine = buildString {
            if (isDelivery && deliveryAddress.isNotBlank()) {
                append("\nAddress: ")
                append(deliveryAddress)
            }
            if (hasGps) {
                append("\nGPS Location Attached (Tap Open Map)")
            }
            if (customerPhone.isNotBlank()) {
                append("\nPhone: ")
                append(customerPhone)
            }
        }
        val expandedBody = if (customBody.isNullOrBlank()) {
            "$shortBody ($branchName)$locationLine"
        } else {
            "$customBody ($branchName • #$shortId)$locationLine"
        }

        val mapActionPendingIntent = if (hasGps) {
            val mapUri = Uri.parse(
                "https://www.google.com/maps/search/?api=1&query=$deliveryLatitude,$deliveryLongitude"
            )
            val mapIntent = Intent(Intent.ACTION_VIEW, mapUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            PendingIntent.getActivity(
                context,
                ("map_action_$orderId").hashCode(),
                mapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            null
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
            expandedText = expandedBody,
            isAdminUrgent = true,
            mapActionPendingIntent = mapActionPendingIntent
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
        totalPkr: Long = 0L,
        customerPhone: String = "",
        deliveryAddress: String = "",
        deliveryLatitude: Double = 0.0,
        deliveryLongitude: Double = 0.0
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
                customerPhone = customerPhone,
                deliveryAddress = deliveryAddress,
                deliveryLatitude = deliveryLatitude,
                deliveryLongitude = deliveryLongitude,
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
        expandedText: String,
        isAdminUrgent: Boolean = false,
        mapActionPendingIntent: PendingIntent? = null
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

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_lahori_notification)
            .setColor(ContextCompat.getColor(context, R.color.lahori_red))
            .setContentTitle(title)
            .setContentText(shortText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setPriority(if (isAdminUrgent) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (isAdminUrgent) NotificationCompat.CATEGORY_MESSAGE else NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
            .setSound(defaultSound)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (isAdminUrgent) {
            builder.setVibrate(ADMIN_VIBRATION_PATTERN)
            builder.addAction(0, "Open Orders", pendingIntent)
        }
        if (mapActionPendingIntent != null) {
            builder.addAction(0, "Open Map", mapActionPendingIntent)
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Permission revoked concurrently
        }
    }
}
