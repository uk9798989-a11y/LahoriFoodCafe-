package com.example.notifications

import android.util.Log
import com.example.data.CafeRepository
import com.example.model.OrderStatus
import com.example.model.OrderType
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

data class FcmOrderPushEvent(
    val orderId: String,
    val status: OrderStatus,
    val title: String,
    val body: String,
    val eventType: String = OrderNotificationHelper.EVENT_ORDER_STATUS,
    val branchId: String = "",
    val branchName: String = "",
    val itemsSummary: String = ""
)

object FcmOrderEventBus {
    private val _events = MutableSharedFlow<FcmOrderPushEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<FcmOrderPushEvent> = _events.asSharedFlow()

    fun emit(event: FcmOrderPushEvent) {
        _events.tryEmit(event)
    }
}

class LahoriFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM registration token refreshed")
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            serviceScope.launch {
                CafeRepository(applicationContext).registerFcmToken(token)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        val eventType = data["eventType"] ?: OrderNotificationHelper.EVENT_ORDER_STATUS
        val isNewOrder = eventType.equals(OrderNotificationHelper.EVENT_NEW_ORDER, ignoreCase = true)
        val orderId = data["orderId"].orEmpty()
        val statusRaw = data["status"].orEmpty()
        val orderType = data["orderType"] ?: if (isNewOrder) OrderType.DELIVERY.name else OrderType.PICKUP.name
        val branchId = data["branchId"].orEmpty()
        val branchName = data["branchName"].orEmpty()
        val customerName = data["customerName"].orEmpty()
        val totalPkr = data["totalPkr"]?.toLongOrNull() ?: 0L
        val itemsSummary = data["itemsSummary"].orEmpty()

        val resolvedStatus = if (isNewOrder) {
            OrderStatus.fromString(statusRaw.ifBlank { OrderStatus.Confirmed.name })
        } else {
            OrderStatus.fromString(statusRaw.ifBlank { OrderStatus.Dispatched.name })
        }
        val shortId = orderId.takeLast(6).uppercase()
        val title = remoteMessage.notification?.title
            ?: data["title"]
            ?: if (isNewOrder) {
                "New Order #$shortId"
            } else {
                "Lahori Food Cafe — ${resolvedStatus.notificationTitleFor(orderType)}"
            }
        val body = remoteMessage.notification?.body
            ?: data["body"]
            ?: if (isNewOrder) {
                "${customerName.ifBlank { "Customer" }} • ${itemsSummary.ifBlank { "New branch order" }}"
            } else {
                resolvedStatus.notificationMessageFor(orderType)
            }

        OrderNotificationHelper.notifyRemoteMessage(
            context = applicationContext,
            title = title,
            body = body,
            orderId = orderId,
            orderType = orderType,
            statusRaw = statusRaw,
            branchName = branchName,
            itemsSummary = itemsSummary,
            eventType = eventType,
            branchId = branchId,
            customerName = customerName,
            totalPkr = totalPkr
        )

        if (orderId.isNotBlank()) {
            FcmOrderEventBus.emit(
                FcmOrderPushEvent(
                    orderId = orderId,
                    status = resolvedStatus,
                    title = title,
                    body = body,
                    eventType = eventType,
                    branchId = branchId,
                    branchName = branchName,
                    itemsSummary = itemsSummary
                )
            )
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "LahoriFCM"
    }
}
