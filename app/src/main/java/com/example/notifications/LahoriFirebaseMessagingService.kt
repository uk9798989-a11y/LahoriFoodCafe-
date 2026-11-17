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
    val body: String
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
        val orderId = data["orderId"].orEmpty()
        val statusRaw = data["status"].orEmpty()
        val orderType = data["orderType"] ?: OrderType.PICKUP.name
        val branchName = data["branchName"].orEmpty()
        val itemsSummary = data["itemsSummary"].orEmpty()

        val resolvedStatus = OrderStatus.fromString(
            statusRaw.ifBlank { OrderStatus.Dispatched.name }
        )
        val title = remoteMessage.notification?.title
            ?: data["title"]
            ?: "Lahori Food Cafe — ${resolvedStatus.notificationTitleFor(orderType)}"
        val body = remoteMessage.notification?.body
            ?: data["body"]
            ?: resolvedStatus.notificationMessageFor(orderType)

        OrderNotificationHelper.notifyRemoteMessage(
            context = applicationContext,
            title = title,
            body = body,
            orderId = orderId,
            orderType = orderType,
            statusRaw = statusRaw,
            branchName = branchName,
            itemsSummary = itemsSummary
        )

        if (orderId.isNotBlank()) {
            FcmOrderEventBus.emit(
                FcmOrderPushEvent(
                    orderId = orderId,
                    status = resolvedStatus,
                    title = title,
                    body = body
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
