package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.OrderStatus

object OrderNotificationHelper {
    private const val CHANNEL_ID = "lahori_order_updates"
    private const val CHANNEL_NAME = "Order Status Updates"
    private const val CHANNEL_DESC = "Real-time Lahori Food Cafe order status updates"

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
        itemsSummary: String
    ) {
        ensureChannel(context)
        if (!hasNotificationPermission(context)) return

        val shortId = orderId.takeLast(6).uppercase()
        val body = "${status.notificationMessage} ($branchName • #$shortId: $itemsSummary)"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Lahori Food Cafe — ${status.notificationTitle}")
            .setContentText(status.notificationMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(orderId.hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission revoked concurrently
        }
    }
}
