package com.example.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CafeRepository
import com.example.data.MenuImageStorageHelper
import com.example.model.AdminDeliveryNotification
import com.example.model.BranchAdminEntry
import com.example.model.BranchBanner
import com.example.model.BranchDirectory
import com.example.model.CafeBranch
import com.example.model.CafeFeedback
import com.example.model.CafeOrder
import com.example.model.CartItem
import com.example.model.MenuItem
import com.example.model.OrderReview
import com.example.model.OrderStatus
import com.example.model.OrderType
import com.example.model.Promotion
import com.example.model.SUPER_ADMIN_EMAIL
import com.example.model.SavedAddress
import com.example.model.SeedMenuData
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.UserRole
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class PendingGuestOrder(
    val cartItems: List<CartItem>,
    val orderType: OrderType,
    val deliveryAddress: String,
    val deliveryLatitude: Double = 0.0,
    val deliveryLongitude: Double = 0.0,
    val branchId: String,
    val customerName: String,
    val customerPhone: String,
    val guestSessionId: String,
    val promoCode: String,
    val discountPkr: Long
)

data class SharedSessionState(
    val cartItems: List<CartItem> = emptyList(),
    val orderType: OrderType = OrderType.DELIVERY,
    val selectedBranchId: String = BranchDirectory.branch1.id,
    val selectedAddressLabel: String = "",
    val selectedAddressStreet: String = "",
    val selectedLandmark: String = "",
    val savedManualAddressId: String = "",
    val deviceLatitude: Double? = null,
    val deviceLongitude: Double? = null,
    val detectedGpsAddress: String = "",
    val detectedGpsArea: String = "",
    val guestAddresses: List<SavedAddress> = emptyList(),
    val guestFavorites: Set<String> = emptySet(),
    val guestSessionId: String = "guest_${UUID.randomUUID().toString().take(8)}",
    val appliedDiscountPkr: Long = 0L,
    val appliedPromoLabel: String = "",
    val appliedPromoBranchId: String = "",
    val pendingGuestOrder: PendingGuestOrder? = null
) {
    val totalItemCount: Int get() = cartItems.sumOf { it.quantity }
    val subtotalPkr: Long get() = cartItems.sumOf { it.lineTotalPkr }
    val hasGpsFix: Boolean get() = deviceLatitude != null && deviceLongitude != null

    fun combinedManualAddress(): String {
        val base = selectedAddressStreet.trim()
        val lm = selectedLandmark.trim()
        if (base.isBlank()) return ""
        return if (lm.isNotBlank() && !base.contains(lm, ignoreCase = true)) {
            "$base (Landmark: $lm)"
        } else {
            base
        }
    }
}

class SessionCartController {
    private val _state = MutableStateFlow(SharedSessionState())
    val state: StateFlow<SharedSessionState> = _state.asStateFlow()

    fun addToCart(item: MenuItem) {
        val current = _state.value.cartItems.toMutableList()
        val idx = current.indexOfFirst { it.menuItem.id == item.id }
        if (idx >= 0) {
            current[idx] = current[idx].copy(quantity = current[idx].quantity + 1)
        } else {
            current.add(CartItem(menuItem = item, quantity = 1))
        }
        _state.value = _state.value.copy(cartItems = current)
    }

    fun decrementCartItem(itemId: String) {
        val current = _state.value.cartItems.toMutableList()
        val idx = current.indexOfFirst { it.menuItem.id == itemId }
        if (idx >= 0) {
            val qty = current[idx].quantity
            if (qty <= 1) current.removeAt(idx) else current[idx] = current[idx].copy(quantity = qty - 1)
            _state.value = _state.value.copy(cartItems = current)
        }
    }

    fun clearCart() {
        _state.value = _state.value.copy(
            cartItems = emptyList(),
            appliedDiscountPkr = 0L,
            appliedPromoLabel = "",
            appliedPromoBranchId = ""
        )
    }

    private fun SharedSessionState.sanitizePromoForBranch(newBranchId: String): SharedSessionState =
        if (appliedPromoBranchId.isNotBlank() && appliedPromoBranchId != newBranchId) {
            copy(appliedPromoLabel = "", appliedDiscountPkr = 0L, appliedPromoBranchId = "")
        } else {
            this
        }

    fun setOrderType(type: OrderType) {
        _state.value = _state.value.copy(orderType = type)
    }

    fun selectDeliveryAddress(address: SavedAddress) {
        val displayStreet = if (address.area.isNotBlank() && !address.streetAddress.contains(address.area, ignoreCase = true)) {
            "${address.streetAddress}, ${address.area}"
        } else {
            address.streetAddress
        }
        _state.value = _state.value
            .sanitizePromoForBranch(address.branchId)
            .copy(
                orderType = OrderType.DELIVERY,
                selectedBranchId = address.branchId,
                selectedAddressLabel = address.label.ifBlank { "Home" },
                selectedAddressStreet = displayStreet.trim(),
                selectedLandmark = address.landmark.trim(),
                savedManualAddressId = address.id
            )
    }

    fun applyManualAddress(
        fullAddress: String,
        landmark: String = "",
        label: String = "Home",
        branchId: String,
        savedAddressId: String = "",
        keepOrderType: Boolean = false
    ) {
        val current = _state.value.sanitizePromoForBranch(branchId)
        _state.value = current.copy(
            orderType = if (keepOrderType) current.orderType else OrderType.DELIVERY,
            selectedBranchId = branchId.ifBlank { current.selectedBranchId },
            selectedAddressLabel = label.trim().ifBlank { "Home" },
            selectedAddressStreet = fullAddress.trim(),
            selectedLandmark = landmark.trim(),
            savedManualAddressId = savedAddressId
        )
    }

    fun restorePersistedAddressIfEmpty(
        fullAddress: String,
        landmark: String,
        label: String,
        branchId: String,
        savedAddressId: String = ""
    ) {
        val current = _state.value
        if (current.selectedAddressStreet.isBlank() && fullAddress.isNotBlank()) {
            _state.value = current.copy(
                selectedBranchId = branchId.ifBlank { current.selectedBranchId },
                selectedAddressLabel = label.ifBlank { "Home" },
                selectedAddressStreet = fullAddress.trim(),
                selectedLandmark = landmark.trim(),
                savedManualAddressId = savedAddressId
            )
        }
    }

    fun selectPickupBranch(branchId: String) {
        _state.value = _state.value
            .sanitizePromoForBranch(branchId)
            .copy(
                orderType = OrderType.PICKUP,
                selectedBranchId = branchId
            )
    }

    fun applyDetectedGpsLocation(
        latitude: Double,
        longitude: Double,
        formattedAddress: String,
        areaLabel: String,
        nearestBranchId: String,
        keepOrderType: Boolean = true
    ) {
        val current = _state.value.sanitizePromoForBranch(nearestBranchId)
        _state.value = current.copy(
            orderType = if (keepOrderType) current.orderType else OrderType.DELIVERY,
            selectedBranchId = nearestBranchId,
            selectedAddressLabel = current.selectedAddressLabel.ifBlank { areaLabel.ifBlank { "GPS Location" } },
            selectedAddressStreet = current.selectedAddressStreet.ifBlank { formattedAddress },
            deviceLatitude = latitude,
            deviceLongitude = longitude,
            detectedGpsAddress = formattedAddress,
            detectedGpsArea = areaLabel
        )
    }

    fun selectQuickDeliveryArea(
        area: String,
        branchId: String
    ) {
        _state.value = _state.value
            .sanitizePromoForBranch(branchId)
            .copy(
                orderType = OrderType.DELIVERY,
                selectedBranchId = branchId,
                selectedAddressLabel = _state.value.selectedAddressLabel.ifBlank { area },
                selectedAddressStreet = _state.value.selectedAddressStreet.ifBlank { area }
            )
    }

    fun clearGpsLocation() {
        _state.value = _state.value.copy(
            deviceLatitude = null,
            deviceLongitude = null,
            detectedGpsAddress = "",
            detectedGpsArea = ""
        )
    }

    fun addGuestAddress(
        label: String,
        streetAddress: String,
        area: String = "",
        landmark: String = "",
        branches: List<CafeBranch> = BranchDirectory.defaultBranches
    ): SavedAddress {
        val cleanStreet = streetAddress.trim()
        val cleanLandmark = landmark.trim()
        val branch = BranchDirectory.routeAddressToBranch("$area $cleanStreet $cleanLandmark", branches)
        val cleanArea = area.trim().ifBlank { branch.displayShortLabel() }
        val addr = SavedAddress(
            id = "guest_addr_${UUID.randomUUID().toString().take(6)}",
            label = label.trim().ifBlank { "Home" },
            streetAddress = cleanStreet,
            area = cleanArea,
            landmark = cleanLandmark,
            branchId = branch.id,
            branchName = branch.name
        )
        _state.value = _state.value
            .sanitizePromoForBranch(addr.branchId)
            .copy(
                guestAddresses = listOf(addr) + _state.value.guestAddresses,
                orderType = OrderType.DELIVERY,
                selectedBranchId = addr.branchId,
                selectedAddressLabel = addr.label,
                selectedAddressStreet = cleanStreet,
                selectedLandmark = cleanLandmark,
                savedManualAddressId = addr.id
            )
        return addr
    }

    fun updateGuestAddress(
        addressId: String,
        label: String,
        streetAddress: String,
        area: String = "",
        landmark: String = "",
        branches: List<CafeBranch> = BranchDirectory.defaultBranches
    ): SavedAddress {
        val cleanStreet = streetAddress.trim()
        val cleanLandmark = landmark.trim()
        val branch = BranchDirectory.routeAddressToBranch("$area $cleanStreet $cleanLandmark", branches)
        val cleanArea = area.trim().ifBlank { branch.displayShortLabel() }
        val updated = SavedAddress(
            id = addressId,
            label = label.trim().ifBlank { "Home" },
            streetAddress = cleanStreet,
            area = cleanArea,
            landmark = cleanLandmark,
            branchId = branch.id,
            branchName = branch.name
        )
        val updatedList = _state.value.guestAddresses.map { if (it.id == addressId) updated else it }
            .let { list -> if (list.any { it.id == addressId }) list else listOf(updated) + list }
        _state.value = _state.value
            .sanitizePromoForBranch(updated.branchId)
            .copy(
                guestAddresses = updatedList,
                orderType = OrderType.DELIVERY,
                selectedBranchId = updated.branchId,
                selectedAddressLabel = updated.label,
                selectedAddressStreet = cleanStreet,
                selectedLandmark = cleanLandmark,
                savedManualAddressId = updated.id
            )
        return updated
    }

    fun deleteGuestAddress(addressId: String) {
        val remaining = _state.value.guestAddresses.filterNot { it.id == addressId }
        _state.value = _state.value.copy(
            guestAddresses = remaining,
            savedManualAddressId = if (_state.value.savedManualAddressId == addressId) "" else _state.value.savedManualAddressId
        )
    }

    fun toggleGuestFavorite(itemId: String) {
        val favs = _state.value.guestFavorites
        _state.value = _state.value.copy(
            guestFavorites = if (favs.contains(itemId)) favs - itemId else favs + itemId
        )
    }

    fun applyPromo(label: String, discountPkr: Long, branchId: String = "") {
        _state.value = _state.value.copy(
            appliedPromoLabel = label,
            appliedDiscountPkr = discountPkr,
            appliedPromoBranchId = branchId
        )
    }

    fun clearPromo() {
        _state.value = _state.value.copy(
            appliedPromoLabel = "",
            appliedDiscountPkr = 0L,
            appliedPromoBranchId = ""
        )
    }

    fun setPendingGuestOrder(pending: PendingGuestOrder?) {
        _state.value = _state.value.copy(pendingGuestOrder = pending)
    }
}

class LahoriViewModel(
    private val repository: CafeRepository,
    val currentUserId: String,
    val currentUserEmail: String,
    val isEmailVerified: Boolean,
    val sessionCart: SessionCartController
) : ViewModel() {

    private val isSuperAdminByEmail: Boolean =
        currentUserEmail.equals(SUPER_ADMIN_EMAIL, ignoreCase = true) && isEmailVerified

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _statusBanner = MutableStateFlow<Pair<String, OrderStatus>?>(null)
    val statusBanner: StateFlow<Pair<String, OrderStatus>?> = _statusBanner.asStateFlow()

    private val _pendingSurveyOrder = MutableStateFlow<CafeOrder?>(null)
    val pendingSurveyOrder: StateFlow<CafeOrder?> = _pendingSurveyOrder.asStateFlow()

    private val _isSubmittingReview = MutableStateFlow(false)
    val isSubmittingReview: StateFlow<Boolean> = _isSubmittingReview.asStateFlow()

    private val _isSubmittingFeedback = MutableStateFlow(false)
    val isSubmittingFeedback: StateFlow<Boolean> = _isSubmittingFeedback.asStateFlow()

    private val _isSavingStaff = MutableStateFlow(false)
    val isSavingStaff: StateFlow<Boolean> = _isSavingStaff.asStateFlow()

    private val _isSavingBranch = MutableStateFlow(false)
    val isSavingBranch: StateFlow<Boolean> = _isSavingBranch.asStateFlow()

    private val _isSavingMenuItem = MutableStateFlow(false)
    val isSavingMenuItem: StateFlow<Boolean> = _isSavingMenuItem.asStateFlow()

    private val _isSavingBranchPromo = MutableStateFlow(false)
    val isSavingBranchPromo: StateFlow<Boolean> = _isSavingBranchPromo.asStateFlow()

    private val _isSavingBranchBanner = MutableStateFlow(false)
    val isSavingBranchBanner: StateFlow<Boolean> = _isSavingBranchBanner.asStateFlow()

    private val _branchAssignmentLoaded = MutableStateFlow(isSuperAdminByEmail || currentUserEmail.isBlank())
    val branchAssignmentLoaded: StateFlow<Boolean> = _branchAssignmentLoaded.asStateFlow()

    private val _trackedOrderId = MutableStateFlow<String?>(null)
    val trackedOrderId: StateFlow<String?> = _trackedOrderId.asStateFlow()

    private val _fcmTokenRegistered = MutableStateFlow(false)
    val fcmTokenRegistered: StateFlow<Boolean> = _fcmTokenRegistered.asStateFlow()

    private val _newBranchOrderAlert = MutableStateFlow<CafeOrder?>(null)
    val newBranchOrderAlert: StateFlow<CafeOrder?> = _newBranchOrderAlert.asStateFlow()

    private val _deliveredPopupQueue = MutableStateFlow<List<AdminDeliveryNotification>>(emptyList())
    val deliveredPopupQueue: StateFlow<List<AdminDeliveryNotification>> = _deliveredPopupQueue.asStateFlow()

    @Volatile
    private var currentFcmToken: String? = null

    @Volatile
    private var subscribedBranchTopic: String? = null

    private val lastSeenOrderStatuses = mutableMapOf<String, OrderStatus>()
    private val handledSurveyOrderIds = mutableSetOf<String>()
    private var initialOrderLoadComplete = false

    private val seenBranchOrderIds = mutableSetOf<String>()
    private var initialBranchOrderLoadComplete = false
    private var lastObservedBranchId: String? = null

    private val seenSuperAdminOrderIds = mutableSetOf<String>()
    private var initialSuperAdminOrderLoadComplete = false

    private val seenDeliveredNotificationIds = mutableSetOf<String>()
    private var initialAdminNotifLoadComplete = false
    private var lastAdminNotifScopeKey: String? = null

    val branchesList: StateFlow<List<CafeBranch>> = repository.observeBranches()
        .map { branches -> branches.ifEmpty { BranchDirectory.defaultBranches } }
        .catch { error ->
            val firestoreError = unwrapFirestoreException(error)
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Branches stream error", firestoreError ?: error)
            emit(BranchDirectory.defaultBranches)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), BranchDirectory.defaultBranches)

    val menuState: StateFlow<UiState<List<MenuItem>>> = repository.observeMenuItems()
        .map<List<MenuItem>, UiState<List<MenuItem>>> { items ->
            val resolved = when {
                items.isEmpty() -> SeedMenuData.initialItems
                items.none { it.id.startsWith("lfc_") } -> {
                    val customItems = items.filter { it.id !in SeedMenuData.legacyStarterIds }
                    (SeedMenuData.initialItems + customItems).sortedBy { it.sortOrder }
                }
                else -> items
            }
            UiState.Success(resolved)
        }
        .catch { error ->
            val firestoreError = unwrapFirestoreException(error)
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Menu stream error", firestoreError ?: error)
            emit(UiState.Success(SeedMenuData.initialItems))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { error ->
            val firestoreError = unwrapFirestoreException(error)
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Profile stream error", firestoreError ?: error)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val savedAddresses: StateFlow<List<SavedAddress>> = repository.observeSavedAddresses(currentUserId)
        .catch { error ->
            val firestoreError = unwrapFirestoreException(error)
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Addresses stream error", firestoreError ?: error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val branchAdminAssignment: StateFlow<BranchAdminEntry?> =
        if (currentUserEmail.isBlank()) {
            MutableStateFlow(null)
        } else {
            repository.observeMyBranchAdminAssignment(currentUserEmail)
                .onEach { _branchAssignmentLoaded.value = true }
                .catch { error ->
                    val firestoreError = unwrapFirestoreException(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(TAG, "Branch admin check error", firestoreError ?: error)
                    _branchAssignmentLoaded.value = true
                    emit(null)
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)
        }

    val effectiveRole: StateFlow<UserRole> = combine(userProfile, branchAdminAssignment) { profile, branchEntry ->
        when {
            isSuperAdminByEmail -> UserRole.SuperAdmin
            profile != null && UserRole.fromString(profile.role) == UserRole.SuperAdmin -> UserRole.SuperAdmin
            branchEntry != null -> UserRole.BranchAdmin
            profile != null -> UserRole.fromString(profile.role)
            else -> UserRole.Customer
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        if (isSuperAdminByEmail) UserRole.SuperAdmin else UserRole.Customer
    )

    val customerOrdersState: StateFlow<UiState<List<CafeOrder>>> = repository.observeCustomerOrders(currentUserId)
        .map<List<CafeOrder>, UiState<List<CafeOrder>>> { orders ->
            detectOrderStatusTransitions(orders)
            UiState.Success(orders)
        }
        .catch { error ->
            val firestoreError = unwrapFirestoreException(error)
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Customer orders error", firestoreError ?: error)
            emit(UiState.Error((firestoreError ?: error).message ?: "Failed to load orders"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    @Suppress("OPT_IN_USAGE")
    val trackedOrderState: StateFlow<UiState<CafeOrder>?> = _trackedOrderId
        .flatMapLatest { orderId ->
            if (orderId.isNullOrBlank()) {
                flowOf(null)
            } else {
                repository.observeOrder(orderId)
                    .map<CafeOrder?, UiState<CafeOrder>?> { order ->
                        if (order != null) UiState.Success(order)
                        else UiState.Error("Order #$orderId was not found.")
                    }
                    .catch { error ->
                        val firestoreError = unwrapFirestoreException(error)
                        if (firestoreError == null && error is CancellationException) throw error
                        Log.w(TAG, "Tracked order stream error", firestoreError ?: error)
                        emit(UiState.Error((firestoreError ?: error).message ?: "Failed to track order in real time"))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val customerReviewsState: StateFlow<UiState<List<OrderReview>>> = repository.observeCustomerReviews(currentUserId)
        .map<List<OrderReview>, UiState<List<OrderReview>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreException(error)
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Customer reviews error", firestoreError ?: error)
            emit(UiState.Error((firestoreError ?: error).message ?: "Failed to load reviews"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val customerFeedbackState: StateFlow<UiState<List<CafeFeedback>>> = repository.observeCustomerFeedback(currentUserId)
        .map<List<CafeFeedback>, UiState<List<CafeFeedback>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreException(error)
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Customer feedback error", firestoreError ?: error)
            emit(UiState.Error((firestoreError ?: error).message ?: "Failed to load feedback"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    @Suppress("OPT_IN_USAGE")
    val branchOrdersState: StateFlow<UiState<List<CafeOrder>>> = branchAdminAssignment
        .flatMapLatest { assignment ->
            if (assignment == null || currentUserEmail.isBlank()) {
                seenBranchOrderIds.clear()
                initialBranchOrderLoadComplete = false
                lastObservedBranchId = null
                flowOf(UiState.Success(emptyList()))
            } else {
                if (lastObservedBranchId != assignment.branchId) {
                    seenBranchOrderIds.clear()
                    initialBranchOrderLoadComplete = false
                    lastObservedBranchId = assignment.branchId
                }
                repository.observeBranchOrders(assignment.branchId)
                    .map<List<CafeOrder>, UiState<List<CafeOrder>>> { orders ->
                        detectBranchNewOrders(orders)
                        UiState.Success(orders)
                    }
                    .catch { error ->
                        val firestoreError = unwrapFirestoreException(error)
                        if (firestoreError == null && error is CancellationException) throw error
                        if (firestoreError?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            emit(UiState.Error("Your branch access was updated or revoked by the Super Admin."))
                        } else {
                            emit(UiState.Error((firestoreError ?: error).message ?: "Failed to load branch orders"))
                        }
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    @Suppress("OPT_IN_USAGE")
    val superAdminOrdersState: StateFlow<UiState<List<CafeOrder>>> = effectiveRole
        .flatMapLatest { role ->
            if (role != UserRole.SuperAdmin) {
                seenSuperAdminOrderIds.clear()
                initialSuperAdminOrderLoadComplete = false
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeAllOrders()
                    .map<List<CafeOrder>, UiState<List<CafeOrder>>> { orders ->
                        detectSuperAdminNewOrders(orders)
                        UiState.Success(orders)
                    }
                    .catch { error ->
                        val firestoreError = unwrapFirestoreException(error)
                        if (firestoreError == null && error is CancellationException) throw error
                        emit(UiState.Error((firestoreError ?: error).message ?: "Failed to load all-branch orders"))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    @Suppress("OPT_IN_USAGE")
    val allBranchAdmins: StateFlow<List<BranchAdminEntry>> = effectiveRole
        .flatMapLatest { role ->
            if (role != UserRole.SuperAdmin) {
                flowOf(emptyList())
            } else {
                repository.observeAllBranchAdmins()
                    .catch { error ->
                        val firestoreError = unwrapFirestoreException(error)
                        if (firestoreError == null && error is CancellationException) throw error
                        emit(emptyList())
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Live promotions readable by signed-in customers (branch-filtered in UI) and admins
    val promotionsList: StateFlow<List<Promotion>> = repository.observePromotions()
        .catch { error ->
            val firestoreError = unwrapFirestoreException(error)
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Promotions stream error", firestoreError ?: error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Live seasonal banners readable by signed-in customers (filtered strictly to selectedBranchId)
    val allBranchBanners: StateFlow<List<BranchBanner>> = repository.observeAllBranchBanners()
        .catch { error ->
            val firestoreError = unwrapFirestoreException(error)
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Branch banners stream error", firestoreError ?: error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @Suppress("OPT_IN_USAGE")
    val branchAdminPromotions: StateFlow<List<Promotion>> = branchAdminAssignment
        .flatMapLatest { assignment ->
            if (assignment == null) {
                flowOf(emptyList())
            } else {
                repository.observeBranchPromotions(assignment.branchId)
                    .catch { error ->
                        val firestoreError = unwrapFirestoreException(error)
                        if (firestoreError == null && error is CancellationException) throw error
                        Log.w(TAG, "Branch admin promotions stream error", firestoreError ?: error)
                        emit(emptyList())
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @Suppress("OPT_IN_USAGE")
    val branchAdminBanners: StateFlow<List<BranchBanner>> = branchAdminAssignment
        .flatMapLatest { assignment ->
            if (assignment == null) {
                flowOf(emptyList())
            } else {
                repository.observeBranchBanners(assignment.branchId)
                    .catch { error ->
                        val firestoreError = unwrapFirestoreException(error)
                        if (firestoreError == null && error is CancellationException) throw error
                        Log.w(TAG, "Branch admin banners stream error", firestoreError ?: error)
                        emit(emptyList())
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @Suppress("OPT_IN_USAGE")
    val allReviews: StateFlow<List<OrderReview>> = effectiveRole
        .flatMapLatest { role ->
            if (role != UserRole.SuperAdmin) {
                flowOf(emptyList())
            } else {
                repository.observeAllReviews()
                    .catch { error ->
                        val firestoreError = unwrapFirestoreException(error)
                        if (firestoreError == null && error is CancellationException) throw error
                        emit(emptyList())
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @Suppress("OPT_IN_USAGE")
    val allFeedback: StateFlow<List<CafeFeedback>> = effectiveRole
        .flatMapLatest { role ->
            if (role != UserRole.SuperAdmin) {
                flowOf(emptyList())
            } else {
                repository.observeAllFeedback()
                    .catch { error ->
                        val firestoreError = unwrapFirestoreException(error)
                        if (firestoreError == null && error is CancellationException) throw error
                        emit(emptyList())
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Persistent Real-Time Admin Delivery Notifications (SuperAdmin: all branches; BranchAdmin: assigned branch)
    @Suppress("OPT_IN_USAGE")
    val adminNotifications: StateFlow<List<AdminDeliveryNotification>> =
        combine(effectiveRole, branchAdminAssignment) { role, assignment ->
            role to assignment
        }.flatMapLatest { (role, assignment) ->
            val scopeKey = when {
                role == UserRole.SuperAdmin -> "super_all"
                assignment != null -> "branch_${assignment.branchId}"
                else -> "none"
            }
            if (lastAdminNotifScopeKey != scopeKey) {
                seenDeliveredNotificationIds.clear()
                initialAdminNotifLoadComplete = false
                lastAdminNotifScopeKey = scopeKey
            }
            when {
                role == UserRole.SuperAdmin -> repository.observeAllAdminNotifications()
                    .onEach { processAdminDeliveryNotifications(it) }
                    .catch { error ->
                        val firestoreError = unwrapFirestoreException(error)
                        if (firestoreError == null && error is CancellationException) throw error
                        Log.w(TAG, "SuperAdmin notifications stream error", firestoreError ?: error)
                        emit(emptyList())
                    }
                assignment != null -> repository.observeBranchAdminNotifications(assignment.branchId)
                    .onEach { processAdminDeliveryNotifications(it) }
                    .catch { error ->
                        val firestoreError = unwrapFirestoreException(error)
                        if (firestoreError == null && error is CancellationException) throw error
                        Log.w(TAG, "BranchAdmin notifications stream error", firestoreError ?: error)
                        emit(emptyList())
                    }
                else -> flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val unreadAdminNotificationCount: StateFlow<Int> = adminNotifications
        .map { list -> list.count { !it.read } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    init {
        viewModelScope.launch {
            repository.ensureUserProfile()
            if (isSuperAdminByEmail) {
                repository.seedInitialBranches()
                repository.seedInitialMenu()
                repository.seedInitialPromotions()
            }
            // Submit any pending guest order created right before 1-tap Google verification
            val pending = sessionCart.state.value.pendingGuestOrder
            if (pending != null) {
                sessionCart.setPendingGuestOrder(null)
                val res = repository.placeOrder(
                    cartItems = pending.cartItems,
                    orderType = pending.orderType,
                    deliveryAddress = pending.deliveryAddress,
                    branchId = pending.branchId,
                    customerName = pending.customerName,
                    customerPhone = pending.customerPhone,
                    guestCheckout = true,
                    guestSessionId = pending.guestSessionId,
                    promoCode = pending.promoCode,
                    discountPkr = pending.discountPkr,
                    deliveryLatitude = pending.deliveryLatitude,
                    deliveryLongitude = pending.deliveryLongitude
                )
                if (res.isSuccess) {
                    sessionCart.clearCart()
                    res.getOrNull()?.let { newOrderId ->
                        startTrackingOrder(newOrderId)
                    }
                } else {
                    _actionError.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to record guest order"
                }
            }
            syncFcmRegistrationToken()
        }

        // Sync FCM topic subscription for BranchAdmin push notifications on new orders
        viewModelScope.launch {
            branchAdminAssignment.collect { assignment ->
                val targetTopic = if (!isSuperAdminByEmail && assignment != null) {
                    "branch_${sanitizeTopicSegment(assignment.branchId)}_orders"
                } else {
                    null
                }
                val previous = subscribedBranchTopic
                if (previous != null && previous != targetTopic) {
                    try {
                        Firebase.messaging.unsubscribeFromTopic(previous).await()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "FCM topic unsubscribe skipped: ${e.message}")
                    }
                    subscribedBranchTopic = null
                }
                if (targetTopic != null && subscribedBranchTopic != targetTopic) {
                    try {
                        Firebase.messaging.subscribeToTopic(targetTopic).await()
                        subscribedBranchTopic = targetTopic
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "FCM topic subscribe skipped: ${e.message}")
                    }
                }
            }
        }

        // Trigger post-delivery rating survey automatically when a Delivered order has no review yet
        viewModelScope.launch {
            combine(customerOrdersState, customerReviewsState) { ordersUi, reviewsUi ->
                ordersUi to reviewsUi
            }.collect { (ordersUi, reviewsUi) ->
                if (ordersUi is UiState.Success && reviewsUi is UiState.Success && _pendingSurveyOrder.value == null) {
                    val reviewedIds = reviewsUi.data.mapTo(mutableSetOf()) { it.orderId }
                    val candidate = ordersUi.data.firstOrNull { order ->
                        OrderStatus.fromString(order.status) == OrderStatus.Delivered &&
                            !reviewedIds.contains(order.id) &&
                            !handledSurveyOrderIds.contains(order.id)
                    }
                    if (candidate != null) {
                        handledSurveyOrderIds.add(candidate.id)
                        _pendingSurveyOrder.value = candidate
                    }
                }
            }
        }
    }

    private fun detectOrderStatusTransitions(orders: List<CafeOrder>) {
        if (!initialOrderLoadComplete) {
            orders.forEach { order ->
                lastSeenOrderStatuses[order.id] = OrderStatus.fromString(order.status)
            }
            initialOrderLoadComplete = true
            return
        }
        orders.forEach { order ->
            val newStatus = OrderStatus.fromString(order.status)
            val prevStatus = lastSeenOrderStatuses[order.id]
            if (prevStatus != null && prevStatus != newStatus) {
                _statusBanner.value = order.id to newStatus
                if (newStatus == OrderStatus.Delivered) {
                    // Allow survey trigger even if previously seen in earlier status
                    handledSurveyOrderIds.remove(order.id)
                }
            }
            lastSeenOrderStatuses[order.id] = newStatus
        }
    }

    private fun detectBranchNewOrders(orders: List<CafeOrder>) {
        if (!initialBranchOrderLoadComplete) {
            orders.forEach { seenBranchOrderIds.add(it.id) }
            initialBranchOrderLoadComplete = true
            return
        }
        val incoming = orders.filter {
            !seenBranchOrderIds.contains(it.id) &&
                OrderStatus.fromString(it.status) == OrderStatus.Confirmed
        }
        orders.forEach { seenBranchOrderIds.add(it.id) }
        val newest = incoming.firstOrNull()
        if (newest != null) {
            _newBranchOrderAlert.value = newest
        }
    }

    private fun detectSuperAdminNewOrders(orders: List<CafeOrder>) {
        if (!initialSuperAdminOrderLoadComplete) {
            orders.forEach { seenSuperAdminOrderIds.add(it.id) }
            initialSuperAdminOrderLoadComplete = true
            return
        }
        val incoming = orders.filter {
            !seenSuperAdminOrderIds.contains(it.id) &&
                OrderStatus.fromString(it.status) == OrderStatus.Confirmed
        }
        orders.forEach { seenSuperAdminOrderIds.add(it.id) }
        val newest = incoming.firstOrNull()
        if (newest != null) {
            _newBranchOrderAlert.value = newest
        }
    }

    fun dismissNewBranchOrderAlert() {
        _newBranchOrderAlert.value = null
    }

    private fun processAdminDeliveryNotifications(notifications: List<AdminDeliveryNotification>) {
        if (!initialAdminNotifLoadComplete) {
            notifications.forEach { seenDeliveredNotificationIds.add(it.id) }
            initialAdminNotifLoadComplete = true
            return
        }
        val newlyDelivered = notifications.filter { notif ->
            notif.id.isNotBlank() &&
                !notif.read &&
                seenDeliveredNotificationIds.add(notif.id)
        }
        notifications.forEach { if (it.id.isNotBlank()) seenDeliveredNotificationIds.add(it.id) }
        if (newlyDelivered.isNotEmpty()) {
            val currentQueue = _deliveredPopupQueue.value
            val existingIds = currentQueue.mapTo(mutableSetOf()) { it.id }
            val toAppend = newlyDelivered.filterNot { existingIds.contains(it.id) }
            if (toAppend.isNotEmpty()) {
                _deliveredPopupQueue.value = currentQueue + toAppend
            }
        }
    }

    fun dismissCurrentDeliveredPopup() {
        val current = _deliveredPopupQueue.value
        if (current.isNotEmpty()) {
            _deliveredPopupQueue.value = current.drop(1)
        }
    }

    fun markAdminNotificationRead(notificationId: String) {
        if (notificationId.isBlank()) return
        // Remove from popup queue if present
        _deliveredPopupQueue.value = _deliveredPopupQueue.value.filterNot { it.id == notificationId }
        viewModelScope.launch {
            val res = repository.markAdminNotificationRead(notificationId)
            if (res.isFailure) {
                Log.w(TAG, "Failed to mark notification read: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun markAllAdminNotificationsRead() {
        val unreadIds = adminNotifications.value.filter { !it.read }.map { it.id }
        if (unreadIds.isEmpty()) return
        _deliveredPopupQueue.value = emptyList()
        viewModelScope.launch {
            val res = repository.markAllAdminNotificationsRead(unreadIds)
            if (res.isFailure) {
                _actionError.value = res.exceptionOrNull()?.localizedMessage ?: "Could not mark notifications read"
            }
        }
    }

    fun openOrderSurvey(order: CafeOrder) {
        if (OrderStatus.fromString(order.status) == OrderStatus.Delivered) {
            _pendingSurveyOrder.value = order
        } else {
            _actionError.value = "You can rate an order once it is delivered."
        }
    }

    fun dismissOrderSurvey(orderId: String) {
        handledSurveyOrderIds.add(orderId)
        _pendingSurveyOrder.value = null
    }

    fun submitOrderReview(
        order: CafeOrder,
        rating: Int,
        comment: String,
        onSuccess: () -> Unit
    ) {
        if (_isSubmittingReview.value) return
        _isSubmittingReview.value = true
        viewModelScope.launch {
            val res = repository.submitOrderReview(order, rating, comment)
            _isSubmittingReview.value = false
            res.onSuccess {
                handledSurveyOrderIds.add(order.id)
                _pendingSurveyOrder.value = null
                onSuccess()
            }.onFailure { err ->
                _actionError.value = err.localizedMessage ?: "Failed to submit review"
            }
        }
    }

    fun deleteReview(reviewId: String) {
        viewModelScope.launch {
            val res = repository.deleteReview(reviewId)
            if (res.isFailure) {
                _actionError.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete review"
            }
        }
    }

    fun submitCafeFeedback(
        branchId: String,
        foodRating: Int,
        serviceRating: Int,
        comment: String,
        onSuccess: () -> Unit
    ) {
        if (_isSubmittingFeedback.value) return
        _isSubmittingFeedback.value = true
        viewModelScope.launch {
            val res = repository.submitCafeFeedback(
                branchId = branchId,
                foodRating = foodRating,
                serviceRating = serviceRating,
                comment = comment
            )
            _isSubmittingFeedback.value = false
            res.onSuccess {
                onSuccess()
            }.onFailure { err ->
                _actionError.value = err.localizedMessage ?: "Failed to submit feedback"
            }
        }
    }

    fun deleteFeedback(feedbackId: String) {
        viewModelScope.launch {
            val res = repository.deleteFeedback(feedbackId)
            if (res.isFailure) {
                _actionError.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete feedback"
            }
        }
    }

    fun dismissStatusBanner() {
        _statusBanner.value = null
    }

    fun startTrackingOrder(orderId: String) {
        if (orderId.isNotBlank()) {
            _trackedOrderId.value = orderId
        }
    }

    fun stopTrackingOrder() {
        _trackedOrderId.value = null
    }

    fun syncFcmRegistrationToken() {
        viewModelScope.launch {
            try {
                val token = Firebase.messaging.token.await()
                if (!token.isNullOrBlank()) {
                    currentFcmToken = token
                    val res = repository.registerFcmToken(token)
                    _fcmTokenRegistered.value = res.isSuccess
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "FCM token sync skipped: ${e.message}")
            }
        }
    }

    suspend fun unregisterDeviceFcmToken() {
        try {
            subscribedBranchTopic?.let { topic ->
                try {
                    Firebase.messaging.unsubscribeFromTopic(topic).await()
                } catch (_: Exception) {
                }
                subscribedBranchTopic = null
            }
            val token = currentFcmToken ?: Firebase.messaging.token.await()
            if (!token.isNullOrBlank()) {
                repository.unregisterFcmToken(token)
                _fcmTokenRegistered.value = false
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "FCM token cleanup skipped: ${e.message}")
        }
    }

    fun clearError() {
        _actionError.value = null
    }

    fun toggleFavorite(itemId: String) {
        viewModelScope.launch {
            val currentFavs = userProfile.value?.favoriteItemIds ?: emptyList()
            val res = repository.toggleFavorite(currentUserId, itemId, currentFavs)
            if (res.isFailure) {
                _actionError.value = res.exceptionOrNull()?.localizedMessage ?: "Could not update favorite"
            }
        }
    }

    fun saveAddressAndRoute(
        label: String,
        streetAddress: String,
        area: String = "",
        landmark: String = "",
        onSaved: (SavedAddress) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.addSavedAddress(
                userId = currentUserId,
                label = label,
                streetAddress = streetAddress,
                area = area,
                landmark = landmark,
                knownBranches = branchesList.value
            )
            res.onSuccess { addr ->
                sessionCart.selectDeliveryAddress(addr)
                onSaved(addr)
            }.onFailure { err ->
                _actionError.value = err.localizedMessage ?: "Could not save address"
            }
        }
    }

    fun updateSavedAddressAndRoute(
        addressId: String,
        label: String,
        streetAddress: String,
        area: String = "",
        landmark: String = "",
        onSaved: (SavedAddress) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.updateSavedAddress(
                userId = currentUserId,
                addressId = addressId,
                label = label,
                streetAddress = streetAddress,
                area = area,
                landmark = landmark,
                knownBranches = branchesList.value
            )
            res.onSuccess { addr ->
                sessionCart.selectDeliveryAddress(addr)
                onSaved(addr)
            }.onFailure { err ->
                _actionError.value = err.localizedMessage ?: "Could not update address"
            }
        }
    }

    fun deleteSavedAddress(addressId: String) {
        viewModelScope.launch {
            val res = repository.deleteSavedAddress(currentUserId, addressId)
            if (res.isFailure) {
                _actionError.value = res.exceptionOrNull()?.localizedMessage ?: "Could not delete address"
            }
        }
    }

    fun submitOrder(
        customerName: String,
        customerPhone: String,
        deliveryAddressOverride: String = "",
        deliveryLatitude: Double = 0.0,
        deliveryLongitude: Double = 0.0,
        guestCheckout: Boolean,
        onError: (String) -> Unit = {},
        onSuccess: (String) -> Unit
    ) {
        val s = sessionCart.state.value
        val selectedBranch = BranchDirectory.byId(s.selectedBranchId, branchesList.value)
        val addressText = if (s.orderType == OrderType.DELIVERY) {
            deliveryAddressOverride.trim()
                .ifBlank { s.selectedAddressStreet.trim() }
                .ifBlank { s.detectedGpsAddress.trim() }
                .ifBlank { "${selectedBranch.displayShortLabel()} Delivery" }
        } else {
            "Self Pickup — ${selectedBranch.shortAddress}"
        }
        val resolvedLat = if (s.orderType == OrderType.DELIVERY) {
            if (deliveryLatitude != 0.0) deliveryLatitude else (s.deviceLatitude ?: 0.0)
        } else {
            0.0
        }
        val resolvedLng = if (s.orderType == OrderType.DELIVERY) {
            if (deliveryLongitude != 0.0) deliveryLongitude else (s.deviceLongitude ?: 0.0)
        } else {
            0.0
        }
        viewModelScope.launch {
            val res = repository.placeOrder(
                cartItems = s.cartItems,
                orderType = s.orderType,
                deliveryAddress = addressText,
                branchId = s.selectedBranchId,
                customerName = customerName,
                customerPhone = customerPhone,
                guestCheckout = guestCheckout,
                guestSessionId = s.guestSessionId,
                promoCode = s.appliedPromoLabel,
                discountPkr = s.appliedDiscountPkr,
                deliveryLatitude = resolvedLat,
                deliveryLongitude = resolvedLng
            )
            res.onSuccess { orderId ->
                sessionCart.clearCart()
                onSuccess(orderId)
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Order submission failed"
                _actionError.value = msg
                onError(msg)
            }
        }
    }

    fun validateAndApplyPromoCode(
        code: String,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        val currentSession = sessionCart.state.value
        val subtotal = currentSession.subtotalPkr
        val selectedBranchId = currentSession.selectedBranchId
        viewModelScope.launch {
            val res = repository.validatePromoCode(
                rawCode = code,
                subtotalPkr = subtotal,
                selectedBranchId = selectedBranchId
            )
            res.onSuccess { (promo, discount) ->
                sessionCart.applyPromo(promo.code, discount, promo.branchId)
                val desc = if (promo.discountType.equals("PERCENT", ignoreCase = true)) {
                    "${promo.discountValue}% off (-${discount} PKR)"
                } else {
                    "Rs. $discount off"
                }
                onResult(true, "Applied ${promo.code}: $desc")
            }.onFailure { err ->
                onResult(false, err.localizedMessage ?: "Invalid promo code")
            }
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        viewModelScope.launch {
            val res = repository.updateOrderStatus(orderId, newStatus)
            if (res.isFailure) {
                _actionError.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to update order status"
            }
        }
    }

    fun saveMenuItem(
        context: Context,
        itemId: String?,
        name: String,
        description: String,
        category: String,
        pricePkr: Long,
        imageKey: String,
        imageUrl: String,
        pickedImageUri: Uri?,
        badgeText: String,
        available: Boolean,
        sortOrder: Long,
        onError: (String) -> Unit = {},
        onDone: () -> Unit
    ) {
        if (_isSavingMenuItem.value) return
        _isSavingMenuItem.value = true
        viewModelScope.launch {
            try {
                val targetId = itemId?.takeIf { it.isNotBlank() } ?: repository.allocateMenuItemId()
                var resolvedImageUrl = imageUrl.trim()

                if (pickedImageUri != null) {
                    val uploadRes = MenuImageStorageHelper.uploadMenuItemImage(
                        context = context.applicationContext,
                        itemId = targetId,
                        imageUri = pickedImageUri
                    )
                    if (uploadRes.isFailure) {
                        val msg = uploadRes.exceptionOrNull()?.localizedMessage
                            ?: "Failed to upload menu item image"
                        _actionError.value = msg
                        onError(msg)
                        return@launch
                    }
                    resolvedImageUrl = uploadRes.getOrThrow()
                }

                val res = repository.saveMenuItem(
                    itemId = itemId,
                    name = name,
                    description = description,
                    category = category,
                    pricePkr = pricePkr,
                    imageKey = imageKey,
                    imageUrl = resolvedImageUrl,
                    badgeText = badgeText,
                    available = available,
                    sortOrder = sortOrder,
                    preAllocatedId = targetId
                )
                res.onSuccess {
                    onDone()
                }.onFailure { err ->
                    val msg = err.localizedMessage ?: "Failed to save menu item"
                    _actionError.value = msg
                    onError(msg)
                }
            } finally {
                _isSavingMenuItem.value = false
            }
        }
    }

    fun deleteMenuItem(itemId: String) {
        viewModelScope.launch {
            val res = repository.deleteMenuItem(itemId)
            if (res.isFailure) {
                _actionError.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete menu item"
            }
        }
    }

    fun seedMenuNow() {
        viewModelScope.launch {
            val res = repository.seedInitialMenu()
            if (res.isFailure) {
                _actionError.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to seed menu"
            }
        }
    }

    fun saveBranch(
        branchId: String?,
        name: String,
        shortLabel: String,
        shortAddress: String,
        areas: List<String>,
        active: Boolean,
        sortOrder: Long? = null,
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        mapUrl: String = "",
        openHours: String = "12:00 PM - 2:00 AM",
        deliveryRadiusKm: Long = 15L,
        onDone: (String) -> Unit
    ) {
        if (_isSavingBranch.value) return
        _isSavingBranch.value = true
        viewModelScope.launch {
            val res = repository.saveBranch(
                branchId = branchId,
                name = name,
                shortLabel = shortLabel,
                shortAddress = shortAddress,
                areas = areas,
                active = active,
                sortOrder = sortOrder,
                latitude = latitude,
                longitude = longitude,
                mapUrl = mapUrl,
                openHours = openHours,
                deliveryRadiusKm = deliveryRadiusKm
            )
            _isSavingBranch.value = false
            res.onSuccess { savedId -> onDone(savedId) }.onFailure { err ->
                _actionError.value = err.localizedMessage ?: "Failed to save branch"
            }
        }
    }

    fun deleteBranch(branchId: String) {
        viewModelScope.launch {
            val res = repository.deleteBranch(branchId)
            if (res.isFailure) {
                _actionError.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete branch"
            }
        }
    }

    fun seedBranchesNow() {
        viewModelScope.launch {
            val res = repository.seedInitialBranches()
            if (res.isFailure) {
                _actionError.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to seed default branches"
            }
        }
    }

    fun addBranchManager(
        email: String,
        role: String = UserRole.BranchAdmin.name,
        branchId: String,
        onDone: () -> Unit
    ) {
        if (_isSavingStaff.value) return
        _isSavingStaff.value = true
        viewModelScope.launch {
            val res = repository.addBranchAdmin(email = email, branchId = branchId, role = role)
            _isSavingStaff.value = false
            res.onSuccess { onDone() }.onFailure { err ->
                _actionError.value = err.localizedMessage ?: "Failed to assign branch manager"
            }
        }
    }

    fun removeBranchManager(adminId: String) {
        viewModelScope.launch {
            val res = repository.removeBranchAdmin(adminId)
            if (res.isFailure) {
                _actionError.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to remove branch manager"
            }
        }
    }

    fun savePromotion(
        code: String,
        discountType: String,
        discountValue: Long,
        minOrderPkr: Long,
        active: Boolean,
        branchId: String = "",
        branchName: String = "",
        expiresAt: Timestamp? = null,
        promoDocId: String? = null,
        onError: (String) -> Unit = {},
        onDone: () -> Unit
    ) {
        if (_isSavingBranchPromo.value) return
        _isSavingBranchPromo.value = true
        viewModelScope.launch {
            try {
                val res = repository.savePromotion(
                    code = code,
                    discountType = discountType,
                    discountValue = discountValue,
                    minOrderPkr = minOrderPkr,
                    active = active,
                    branchId = branchId,
                    branchName = branchName,
                    expiresAt = expiresAt,
                    promoDocId = promoDocId
                )
                res.onSuccess { onDone() }.onFailure { err ->
                    val msg = err.localizedMessage ?: "Failed to save promo code"
                    _actionError.value = msg
                    onError(msg)
                }
            } finally {
                _isSavingBranchPromo.value = false
            }
        }
    }

    fun deletePromotion(promoId: String) {
        viewModelScope.launch {
            val res = repository.deletePromotion(promoId)
            if (res.isFailure) {
                _actionError.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete promo code"
            }
        }
    }

    fun saveBranchBanner(
        context: Context,
        bannerId: String?,
        branchId: String,
        branchName: String,
        title: String,
        subtitle: String,
        promoCode: String,
        badgeText: String,
        imageUrl: String,
        pickedImageUri: Uri?,
        targetCategory: String,
        active: Boolean,
        expiresAt: Timestamp?,
        onError: (String) -> Unit = {},
        onDone: () -> Unit
    ) {
        if (_isSavingBranchBanner.value) return
        _isSavingBranchBanner.value = true
        viewModelScope.launch {
            try {
                val targetBannerId = bannerId?.takeIf { it.isNotBlank() } ?: repository.allocateBranchBannerId()
                var resolvedImageUrl = imageUrl.trim()

                if (pickedImageUri != null) {
                    val uploadRes = MenuImageStorageHelper.uploadBranchBannerImage(
                        context = context.applicationContext,
                        branchId = branchId,
                        bannerId = targetBannerId,
                        imageUri = pickedImageUri
                    )
                    if (uploadRes.isFailure) {
                        val msg = uploadRes.exceptionOrNull()?.localizedMessage
                            ?: "Failed to upload seasonal banner image"
                        _actionError.value = msg
                        onError(msg)
                        return@launch
                    }
                    resolvedImageUrl = uploadRes.getOrThrow()
                }

                val res = repository.saveBranchBanner(
                    bannerId = bannerId,
                    branchId = branchId,
                    branchName = branchName,
                    title = title,
                    subtitle = subtitle,
                    promoCode = promoCode,
                    badgeText = badgeText,
                    imageUrl = resolvedImageUrl,
                    targetCategory = targetCategory,
                    active = active,
                    expiresAt = expiresAt,
                    preAllocatedId = targetBannerId
                )
                res.onSuccess { onDone() }.onFailure { err ->
                    val msg = err.localizedMessage ?: "Failed to save seasonal banner"
                    _actionError.value = msg
                    onError(msg)
                }
            } finally {
                _isSavingBranchBanner.value = false
            }
        }
    }

    fun deleteBranchBanner(bannerId: String) {
        viewModelScope.launch {
            val res = repository.deleteBranchBanner(bannerId)
            if (res.isFailure) {
                _actionError.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete seasonal banner"
            }
        }
    }

    private fun sanitizeTopicSegment(raw: String): String =
        raw.trim().replace(Regex("[^a-zA-Z0-9\\-_.~%]"), "_").ifBlank { "default" }

    private fun unwrapFirestoreException(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    private companion object {
        const val TAG = "LahoriVM"
    }
}
