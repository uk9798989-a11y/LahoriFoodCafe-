package com.example.notifications

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.CafeOrder
import com.example.model.OrderStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Play-Policy-compliant Android 14+ foreground service (foregroundServiceType="dataSync")
 * that keeps a real-time Firestore listener active for authenticated SuperAdmin & BranchAdmin
 * accounts so incoming customer orders fire immediate heads-up notifications outside the app
 * (status bar, lock screen, sound & vibration) even when the app is minimized or backgrounded.
 */
class AdminOrderMonitorService : Service() {

    private var ordersListener: ListenerRegistration? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    private val seenOrderIds = mutableSetOf<String>()
    private var initialSnapshotProcessed = false
    private var activeScopeKey: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val listener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser == null) {
                Log.d(TAG, "Admin signed out; stopping live order monitor service")
                clearSavedConfig(applicationContext)
                stopSelf()
            }
        }
        authStateListener = listener
        Firebase.auth.addAuthStateListener(listener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_MONITOR) {
            clearSavedConfig(applicationContext)
            stopListeningAndTerminate()
            return START_NOT_STICKY
        }

        val currentUser = Firebase.auth.currentUser
        if (currentUser == null) {
            clearSavedConfig(applicationContext)
            stopListeningAndTerminate()
            return START_NOT_STICKY
        }

        val prefs = getSharedPreferences(PREFS_MONITOR, Context.MODE_PRIVATE)
        val isSuperAdmin: Boolean
        val branchId: String
        val branchName: String

        if (intent != null && intent.hasExtra(EXTRA_IS_SUPER_ADMIN)) {
            isSuperAdmin = intent.getBooleanExtra(EXTRA_IS_SUPER_ADMIN, false)
            branchId = intent.getStringExtra(EXTRA_BRANCH_ID).orEmpty().trim()
            branchName = intent.getStringExtra(EXTRA_BRANCH_NAME).orEmpty().trim()
            prefs.edit()
                .putBoolean(KEY_IS_SUPER_ADMIN, isSuperAdmin)
                .putString(KEY_BRANCH_ID, branchId)
                .putString(KEY_BRANCH_NAME, branchName)
                .apply()
        } else {
            isSuperAdmin = prefs.getBoolean(KEY_IS_SUPER_ADMIN, false)
            branchId = prefs.getString(KEY_BRANCH_ID, "").orEmpty().trim()
            branchName = prefs.getString(KEY_BRANCH_NAME, "").orEmpty().trim()
        }

        if (!isSuperAdmin && branchId.isBlank()) {
            stopListeningAndTerminate()
            return START_NOT_STICKY
        }

        val roleLabel = if (isSuperAdmin) "Super Admin" else "Branch Admin"
        val branchLabel = if (isSuperAdmin) "All Branches" else branchName.ifBlank { branchId }

        val stopIntent = Intent(this, AdminOrderMonitorService::class.java).apply {
            action = ACTION_STOP_MONITOR
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            9002,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val foregroundNotification = OrderNotificationHelper.buildAdminMonitorForegroundNotification(
            context = applicationContext,
            roleLabel = roleLabel,
            branchLabel = branchLabel,
            stopPendingIntent = stopPendingIntent
        )

        try {
            val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
            ServiceCompat.startForeground(
                this,
                MONITOR_NOTIFICATION_ID,
                foregroundNotification,
                serviceType
            )
            _isRunning.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Unable to promote AdminOrderMonitorService to foreground: ${e.message}")
            _isRunning.value = false
            stopSelf()
            return START_NOT_STICKY
        }

        val newScopeKey = if (isSuperAdmin) "super_all" else "branch_$branchId"
        if (activeScopeKey != newScopeKey || ordersListener == null) {
            attachFirestoreOrderListener(isSuperAdmin, branchId)
            activeScopeKey = newScopeKey
        }

        return START_STICKY
    }

    private fun attachFirestoreOrderListener(isSuperAdmin: Boolean, branchId: String) {
        ordersListener?.remove()
        ordersListener = null
        seenOrderIds.clear()
        initialSnapshotProcessed = false

        val databaseId = applicationContext.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        val query = if (isSuperAdmin) {
            db.collection("orders")
        } else {
            db.collection("orders").whereEqualTo("branchId", branchId)
        }

        ordersListener = query.addSnapshotListener { snapshots, error ->
            if (error != null) {
                Log.w(TAG, "Live admin order listener error: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshots == null) return@addSnapshotListener

            if (!initialSnapshotProcessed) {
                snapshots.documents.forEach { doc ->
                    seenOrderIds.add(doc.id)
                }
                initialSnapshotProcessed = true
                return@addSnapshotListener
            }

            for (change in snapshots.documentChanges) {
                if (change.type == DocumentChange.Type.ADDED) {
                    val doc = change.document
                    if (seenOrderIds.add(doc.id)) {
                        val order = doc.toObject(
                            CafeOrder::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                        if (OrderStatus.fromString(order.status) == OrderStatus.Confirmed) {
                            OrderNotificationHelper.notifyNewBranchOrder(
                                context = applicationContext,
                                orderId = order.id.ifBlank { doc.id },
                                branchId = order.branchId,
                                branchName = order.branchName,
                                customerName = order.customerName,
                                orderType = order.orderType,
                                totalPkr = order.totalPkr,
                                itemsSummary = order.itemsSummary,
                                customerPhone = order.customerPhone,
                                deliveryAddress = order.deliveryAddress,
                                deliveryLatitude = order.deliveryLatitude,
                                deliveryLongitude = order.deliveryLongitude
                            )
                            FcmOrderEventBus.emit(
                                FcmOrderPushEvent(
                                    orderId = order.id.ifBlank { doc.id },
                                    status = OrderStatus.Confirmed,
                                    title = "New ${order.orderType} Order #${(order.id.ifBlank { doc.id }).takeLast(6).uppercase()}",
                                    body = "${order.customerName} • Rs. ${order.totalPkr} • ${order.itemsSummary}",
                                    eventType = OrderNotificationHelper.EVENT_NEW_ORDER,
                                    branchId = order.branchId,
                                    branchName = order.branchName,
                                    itemsSummary = order.itemsSummary
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun stopListeningAndTerminate() {
        ordersListener?.remove()
        ordersListener = null
        activeScopeKey = null
        _isRunning.value = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        ordersListener?.remove()
        ordersListener = null
        authStateListener?.let { Firebase.auth.removeAuthStateListener(it) }
        authStateListener = null
        activeScopeKey = null
        _isRunning.value = false
        super.onDestroy()
    }

    companion object {
        private const val TAG = "AdminOrderMonitorSvc"
        private const val MONITOR_NOTIFICATION_ID = 7701
        private const val PREFS_MONITOR = "lahori_admin_monitor_prefs"
        private const val KEY_IS_SUPER_ADMIN = "is_super_admin"
        private const val KEY_BRANCH_ID = "branch_id"
        private const val KEY_BRANCH_NAME = "branch_name"

        const val EXTRA_IS_SUPER_ADMIN = "extra_is_super_admin"
        const val EXTRA_BRANCH_ID = "extra_branch_id"
        const val EXTRA_BRANCH_NAME = "extra_branch_name"
        const val ACTION_STOP_MONITOR = "com.example.notifications.ACTION_STOP_ADMIN_MONITOR"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun startMonitoring(
            context: Context,
            isSuperAdmin: Boolean,
            branchId: String = "",
            branchName: String = ""
        ) {
            if (!isSuperAdmin && branchId.isBlank()) return
            if (!OrderNotificationHelper.hasNotificationPermission(context)) return
            val intent = Intent(context.applicationContext, AdminOrderMonitorService::class.java).apply {
                putExtra(EXTRA_IS_SUPER_ADMIN, isSuperAdmin)
                putExtra(EXTRA_BRANCH_ID, branchId)
                putExtra(EXTRA_BRANCH_NAME, branchName)
            }
            try {
                ContextCompat.startForegroundService(context.applicationContext, intent)
            } catch (e: Exception) {
                Log.w(TAG, "Deferred starting AdminOrderMonitorService: ${e.message}")
            }
        }

        fun stopMonitoring(context: Context) {
            clearSavedConfig(context.applicationContext)
            try {
                context.applicationContext.stopService(
                    Intent(context.applicationContext, AdminOrderMonitorService::class.java)
                )
            } catch (_: Exception) {
            }
            _isRunning.value = false
        }

        private fun clearSavedConfig(context: Context) {
            context.getSharedPreferences(PREFS_MONITOR, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        }
    }
}
