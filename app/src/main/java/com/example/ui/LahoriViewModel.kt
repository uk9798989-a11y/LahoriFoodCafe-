package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CafeRepository
import com.example.model.BranchAdminEntry
import com.example.model.BranchDirectory
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
import com.google.firebase.firestore.FirebaseFirestoreException
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class PendingGuestOrder(
    val cartItems: List<CartItem>,
    val orderType: OrderType,
    val deliveryAddress: String,
    val branchId: String,
    val customerName: String,
    val customerPhone: String,
    val guestSessionId: String,
    val promoCode: String,
    val discountPkr: Long
)

data class SharedSessionState(
    val cartItems: List<CartItem> = emptyList(),
    val orderType: OrderType = OrderType.PICKUP,
    val selectedBranchId: String = BranchDirectory.branch1.id,
    val selectedAddressLabel: String = "",
    val selectedAddressStreet: String = "",
    val guestAddresses: List<SavedAddress> = emptyList(),
    val guestFavorites: Set<String> = emptySet(),
    val guestSessionId: String = "guest_${UUID.randomUUID().toString().take(8)}",
    val appliedDiscountPkr: Long = 0L,
    val appliedPromoLabel: String = "",
    val pendingGuestOrder: PendingGuestOrder? = null
) {
    val totalItemCount: Int get() = cartItems.sumOf { it.quantity }
    val subtotalPkr: Long get() = cartItems.sumOf { it.lineTotalPkr }
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
            appliedPromoLabel = ""
        )
    }

    fun setOrderType(type: OrderType) {
        _state.value = _state.value.copy(orderType = type)
    }

    fun selectDeliveryAddress(address: SavedAddress) {
        _state.value = _state.value.copy(
            orderType = OrderType.DELIVERY,
            selectedBranchId = address.branchId,
            selectedAddressLabel = address.label,
            selectedAddressStreet = "${address.streetAddress}, ${address.area}"
        )
    }

    fun selectPickupBranch(branchId: String) {
        _state.value = _state.value.copy(
            orderType = OrderType.PICKUP,
            selectedBranchId = branchId
        )
    }

    fun addGuestAddress(label: String, streetAddress: String, area: String): SavedAddress {
        val branch = BranchDirectory.routeAddressToBranch("$area $streetAddress")
        val addr = SavedAddress(
            id = "guest_addr_${UUID.randomUUID().toString().take(6)}",
            label = label.ifBlank { "Home" },
            streetAddress = streetAddress,
            area = area,
            branchId = branch.id,
            branchName = branch.name
        )
        _state.value = _state.value.copy(
            guestAddresses = listOf(addr) + _state.value.guestAddresses,
            orderType = OrderType.DELIVERY,
            selectedBranchId = addr.branchId,
            selectedAddressLabel = addr.label,
            selectedAddressStreet = "${addr.streetAddress}, ${addr.area}"
        )
        return addr
    }

    fun toggleGuestFavorite(itemId: String) {
        val favs = _state.value.guestFavorites
        _state.value = _state.value.copy(
            guestFavorites = if (favs.contains(itemId)) favs - itemId else favs + itemId
        )
    }

    fun applyPromo(label: String, discountPkr: Long) {
        _state.value = _state.value.copy(
            appliedPromoLabel = label,
            appliedDiscountPkr = discountPkr
        )
    }

    fun clearPromo() {
        _state.value = _state.value.copy(
            appliedPromoLabel = "",
            appliedDiscountPkr = 0L
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

    private val lastSeenOrderStatuses = mutableMapOf<String, OrderStatus>()
    private val handledSurveyOrderIds = mutableSetOf<String>()
    private var initialOrderLoadComplete = false

    val menuState: StateFlow<UiState<List<MenuItem>>> = repository.observeMenuItems()
        .map<List<MenuItem>, UiState<List<MenuItem>>> { items ->
            UiState.Success(items.ifEmpty { SeedMenuData.initialItems })
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
                .catch { error ->
                    val firestoreError = unwrapFirestoreException(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(TAG, "Branch admin check error", firestoreError ?: error)
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

    val customerReviewsState: StateFlow<UiState<List<OrderReview>>> = repository.observeCustomerReviews(currentUserId)
        .map<List<OrderReview>, UiState<List<OrderReview>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreException(error)
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Customer reviews error", firestoreError ?: error)
            emit(UiState.Error((firestoreError ?: error).message ?: "Failed to load reviews"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    @Suppress("OPT_IN_USAGE")
    val branchOrdersState: StateFlow<UiState<List<CafeOrder>>> = branchAdminAssignment
        .flatMapLatest { assignment ->
            if (assignment == null || currentUserEmail.isBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeBranchOrders(currentUserEmail, assignment.branchId)
                    .map<List<CafeOrder>, UiState<List<CafeOrder>>> { UiState.Success(it) }
                    .catch { error ->
                        val firestoreError = unwrapFirestoreException(error)
                        if (firestoreError == null && error is CancellationException) throw error
                        emit(UiState.Error((firestoreError ?: error).message ?: "Failed to load branch orders"))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    @Suppress("OPT_IN_USAGE")
    val superAdminOrdersState: StateFlow<UiState<List<CafeOrder>>> = effectiveRole
        .flatMapLatest { role ->
            if (role != UserRole.SuperAdmin) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeAllOrders()
                    .map<List<CafeOrder>, UiState<List<CafeOrder>>> { UiState.Success(it) }
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

    @Suppress("OPT_IN_USAGE")
    val promotionsList: StateFlow<List<Promotion>> = effectiveRole
        .flatMapLatest { role ->
            if (role != UserRole.SuperAdmin) {
                flowOf(emptyList())
            } else {
                repository.observePromotions()
                    .catch { error ->
                        val firestoreError = unwrapFirestoreException(error)
                        if (firestoreError == null && error is CancellationException) throw error
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

    init {
        viewModelScope.launch {
            repository.ensureUserProfile()
            if (isSuperAdminByEmail) {
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
                    discountPkr = pending.discountPkr
                )
                if (res.isSuccess) {
                    sessionCart.clearCart()
                } else {
                    _actionError.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to record guest order"
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
                        order.status == OrderStatus.Delivered.name &&
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

    fun openOrderSurvey(order: CafeOrder) {
        if (order.status == OrderStatus.Delivered.name) {
            _pendingSurveyOrder.value = order
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

    fun dismissStatusBanner() {
        _statusBanner.value = null
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

    fun saveAddressAndRoute(label: String, streetAddress: String, area: String, onSaved: (SavedAddress) -> Unit) {
        viewModelScope.launch {
            val res = repository.addSavedAddress(currentUserId, label, streetAddress, area)
            res.onSuccess { addr ->
                sessionCart.selectDeliveryAddress(addr)
                onSaved(addr)
            }.onFailure { err ->
                _actionError.value = err.localizedMessage ?: "Could not save address"
            }
        }
    }

    fun submitOrder(
        customerName: String,
        customerPhone: String,
        guestCheckout: Boolean,
        onSuccess: (String) -> Unit
    ) {
        val s = sessionCart.state.value
        val addressText = if (s.orderType == OrderType.DELIVERY) {
            s.selectedAddressStreet.ifBlank { "Lahore Delivery" }
        } else {
            "Self Pickup — ${BranchDirectory.byId(s.selectedBranchId).shortAddress}"
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
                discountPkr = s.appliedDiscountPkr
            )
            res.onSuccess { orderId ->
                sessionCart.clearCart()
                onSuccess(orderId)
            }.onFailure { err ->
                _actionError.value = err.localizedMessage ?: "Order submission failed"
            }
        }
    }

    fun validateAndApplyPromoCode(
        code: String,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        val subtotal = sessionCart.state.value.subtotalPkr
        viewModelScope.launch {
            val res = repository.validatePromoCode(code, subtotal)
            res.onSuccess { (promo, discount) ->
                sessionCart.applyPromo(promo.code, discount)
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
        itemId: String?,
        name: String,
        description: String,
        category: String,
        pricePkr: Long,
        imageKey: String,
        badgeText: String,
        available: Boolean,
        sortOrder: Long,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.saveMenuItem(
                itemId = itemId,
                name = name,
                description = description,
                category = category,
                pricePkr = pricePkr,
                imageKey = imageKey,
                badgeText = badgeText,
                available = available,
                sortOrder = sortOrder
            )
            res.onSuccess { onDone() }.onFailure { err ->
                _actionError.value = err.localizedMessage ?: "Failed to save menu item"
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

    fun addBranchManager(email: String, branchId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val res = repository.addBranchAdmin(email, branchId)
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
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.savePromotion(code, discountType, discountValue, minOrderPkr, active)
            res.onSuccess { onDone() }.onFailure { err ->
                _actionError.value = err.localizedMessage ?: "Failed to save promo code"
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

    private fun unwrapFirestoreException(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    private companion object {
        const val TAG = "LahoriVM"
    }
}
