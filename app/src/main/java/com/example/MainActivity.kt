package com.example

import android.Manifest
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.CafeRepository
import com.example.data.LocationHelper
import com.example.data.ThemePreferences
import com.example.data.attemptAutoSignIn
import com.example.data.authStateFlow
import com.example.data.onGoogleSignInClicked
import com.example.data.signOutUser
import java.util.Locale
import com.example.model.BranchDirectory
import com.example.model.OrderStatus
import com.example.model.OrderType
import com.example.model.SUPER_ADMIN_EMAIL
import com.example.model.SeedMenuData
import com.example.model.UiState
import com.example.model.UserRole
import com.example.notifications.FcmOrderEventBus
import com.example.notifications.OrderNotificationHelper
import com.example.ui.LahoriViewModel
import com.example.ui.PendingGuestOrder
import com.example.ui.SessionCartController
import com.example.ui.components.BucketCheckoutSheet
import com.example.ui.components.CafeTab
import com.example.ui.components.FloatingBucketFab
import com.example.ui.components.LahoriBottomBar
import com.example.ui.components.LahoriSplashOverlay
import com.example.ui.components.LahoriTopHeader
import com.example.ui.components.OrderReviewSurveySheet
import com.example.ui.components.PickupBranchBottomSheet
import com.example.ui.components.SavedAddressesBottomSheet
import com.example.ui.components.SplashSessionState
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.AuthWelcomeScreen
import com.example.ui.screens.BranchAdminDashboardScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.LiveOrderTrackingScreen
import com.example.ui.screens.MenuScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.RewardsScreen
import com.example.ui.screens.SuperAdminDashboardScreen
import com.example.ui.theme.LahoriBlack
import com.example.ui.theme.LahoriCafeTheme
import com.example.ui.theme.LahoriGold
import com.example.ui.theme.LahoriRed
import com.example.ui.theme.LahoriWhite
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var notificationOrderIdState by mutableStateOf<String?>(null)
    private var notificationEventTypeState by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationOrderIdState = intent?.getStringExtra(OrderNotificationHelper.EXTRA_TRACK_ORDER_ID)
        notificationEventTypeState = intent?.getStringExtra(OrderNotificationHelper.EXTRA_NOTIFICATION_EVENT_TYPE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            splashScreen.setOnExitAnimationListener { splashScreenView ->
                splashScreenView.remove()
            }
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        )
        OrderNotificationHelper.ensureChannel(this)
        setContent {
            val isDarkMode by ThemePreferences.observeDarkMode(applicationContext)
                .collectAsStateWithLifecycle(initialValue = false)
            var splashActive by remember { mutableStateOf(!SplashSessionState.hasPlayedThisSession) }
            val themeScope = rememberCoroutineScope()

            LaunchedEffect(isDarkMode, splashActive) {
                val useDarkBars = isDarkMode && !splashActive
                val barStyle = if (useDarkBars) {
                    SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                } else {
                    SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                }
                enableEdgeToEdge(
                    statusBarStyle = barStyle,
                    navigationBarStyle = barStyle
                )
            }

            LahoriCafeTheme(darkTheme = isDarkMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    LahoriAppRoot(
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = { enabled ->
                            themeScope.launch {
                                ThemePreferences.setDarkMode(applicationContext, enabled)
                            }
                        },
                        pendingNotificationOrderId = notificationOrderIdState,
                        pendingNotificationEventType = notificationEventTypeState,
                        onConsumeNotificationOrderId = {
                            notificationOrderIdState = null
                            notificationEventTypeState = null
                        }
                    )
                    LahoriSplashOverlay(
                        onSplashFinished = { splashActive = false }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val orderId = intent.getStringExtra(OrderNotificationHelper.EXTRA_TRACK_ORDER_ID)
        if (!orderId.isNullOrBlank()) {
            notificationOrderIdState = orderId
            notificationEventTypeState = intent.getStringExtra(OrderNotificationHelper.EXTRA_NOTIFICATION_EVENT_TYPE)
        }
    }
}

@Composable
private fun LahoriAppRoot(
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    pendingNotificationOrderId: String?,
    pendingNotificationEventType: String?,
    onConsumeNotificationOrderId: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val sessionCart = remember { SessionCartController() }

    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    var guestBrowseMode by remember { mutableStateOf(false) }
    var authLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { guestBrowseMode = false },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null && !guestBrowseMode) {
        AuthWelcomeScreen(
            isLoading = authLoading,
            authError = authError,
            onGoogleSignInClick = {
                authLoading = true
                authError = null
                onGoogleSignInClicked(
                    context = context,
                    credentialManager = credentialManager,
                    onAuthSuccess = {
                        authLoading = false
                        guestBrowseMode = false
                    },
                    onAuthError = { msg ->
                        authLoading = false
                        authError = msg
                    },
                    scope = scope,
                    onAuthCancelled = {
                        authLoading = false
                    }
                )
            },
            onContinueAsGuestClick = {
                authError = null
                guestBrowseMode = true
            }
        )
    } else if (user == null) {
        // Guest Browse & Cart Mode (zero unauthenticated Firestore calls!)
        GuestCafeShell(
            sessionCart = sessionCart,
            isDarkMode = isDarkMode,
            onToggleDarkMode = onToggleDarkMode,
            onTriggerGoogleSignIn = { onVerified ->
                authLoading = true
                onGoogleSignInClicked(
                    context = context,
                    credentialManager = credentialManager,
                    onAuthSuccess = {
                        authLoading = false
                        guestBrowseMode = false
                        onVerified()
                    },
                    onAuthError = { msg ->
                        authLoading = false
                        authError = msg
                    },
                    scope = scope,
                    onAuthCancelled = { authLoading = false }
                )
            }
        )
    } else {
        // Authenticated Session: keyed by UID so switching accounts resets ViewModels & listeners cleanly
        key(user.uid) {
            AuthenticatedCafeShell(
                userId = user.uid,
                userEmail = user.email ?: "",
                userDisplayName = user.displayName ?: "Lahori Customer",
                isEmailVerified = user.isEmailVerified,
                sessionCart = sessionCart,
                isDarkMode = isDarkMode,
                onToggleDarkMode = onToggleDarkMode,
                pendingNotificationOrderId = pendingNotificationOrderId,
                pendingNotificationEventType = pendingNotificationEventType,
                onConsumeNotificationOrderId = onConsumeNotificationOrderId,
                onSignOut = {
                    signOutUser(credentialManager, scope) {
                        guestBrowseMode = false
                    }
                }
            )
        }
    }
}

@Composable
private fun GuestCafeShell(
    sessionCart: SessionCartController,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onTriggerGoogleSignIn: (onVerified: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val sessionState by sessionCart.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var currentTab by remember { mutableStateOf(CafeTab.Explore) }
    var searchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showSavedAddressesSheet by remember { mutableStateOf(false) }
    var showPickupBranchSheet by remember { mutableStateOf(false) }
    var showBucketSheet by remember { mutableStateOf(false) }
    var isLocatingGps by remember { mutableStateOf(false) }

    val triggerGpsDetection: (Boolean) -> Unit = { keepOrderType ->
        if (!isLocatingGps) {
            isLocatingGps = true
            scope.launch {
                val gpsRes = LocationHelper.fetchCurrentLocationWithAddress(context)
                isLocatingGps = false
                gpsRes.onSuccess { loc ->
                    val nearest = BranchDirectory.nearestBranch(
                        lat = loc.latitude,
                        lng = loc.longitude,
                        branches = BranchDirectory.defaultBranches
                    )
                    sessionCart.applyDetectedGpsLocation(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        formattedAddress = loc.formattedAddress,
                        areaLabel = loc.areaLabel,
                        nearestBranchId = nearest.id,
                        keepOrderType = keepOrderType
                    )
                    val dist = BranchDirectory.distanceToKm(nearest, loc.latitude, loc.longitude)
                    val distText = if (dist != null) " (~${String.format(Locale.US, "%.1f", dist)} km)" else ""
                    snackbarHostState.showSnackbar("Nearest branch: ${nearest.displayShortLabel()}$distText")
                }.onFailure { err ->
                    snackbarHostState.showSnackbar(err.localizedMessage ?: "Could not detect GPS location")
                }
            }
        }
    }

    val cartQuantities = remember(sessionState.cartItems) {
        sessionState.cartItems.associate { it.menuItem.id to it.quantity }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LahoriTopHeader(
                orderType = sessionState.orderType,
                selectedBranchId = sessionState.selectedBranchId,
                selectedAddressStreet = sessionState.selectedAddressStreet,
                searchVisible = searchVisible,
                searchQuery = searchQuery,
                onToggleSearch = {
                    searchVisible = !searchVisible
                    if (!searchVisible) searchQuery = ""
                },
                onSearchQueryChange = { searchQuery = it },
                onDeliveryClick = {
                    sessionCart.setOrderType(OrderType.DELIVERY)
                    showSavedAddressesSheet = true
                },
                onPickupClick = {
                    sessionCart.setOrderType(OrderType.PICKUP)
                    showPickupBranchSheet = true
                },
                branches = BranchDirectory.defaultBranches,
                deviceLatitude = sessionState.deviceLatitude,
                deviceLongitude = sessionState.deviceLongitude,
                onLocationBannerClick = {
                    if (sessionState.orderType == OrderType.DELIVERY) {
                        showSavedAddressesSheet = true
                    } else {
                        showPickupBranchSheet = true
                    }
                }
            )
        },
        bottomBar = {
            LahoriBottomBar(
                currentTab = currentTab,
                activeOrderCount = 0,
                onTabSelected = { currentTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                CafeTab.Explore -> ExploreScreen(
                    menuItems = SeedMenuData.initialItems,
                    favoriteIds = sessionState.guestFavorites,
                    cartQuantities = cartQuantities,
                    searchQuery = searchQuery,
                    onToggleFavorite = { sessionCart.toggleGuestFavorite(it) },
                    onAddToBucket = { item ->
                        sessionCart.addToCart(item)
                        scope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar("Added ${item.name} to Bucket")
                        }
                    }
                )
                CafeTab.Menu -> MenuScreen(
                    menuItems = SeedMenuData.initialItems,
                    favoriteIds = sessionState.guestFavorites,
                    cartQuantities = cartQuantities,
                    searchQuery = searchQuery,
                    onToggleFavorite = { sessionCart.toggleGuestFavorite(it) },
                    onAddToBucket = { sessionCart.addToCart(it) }
                )
                CafeTab.Rewards -> RewardsScreen(
                    orders = emptyList(),
                    appliedPromoLabel = sessionState.appliedPromoLabel,
                    onApplyVoucher = { code, discount ->
                        sessionCart.applyPromo(code, discount)
                        scope.launch { snackbarHostState.showSnackbar("Applied $code (-Rs. $discount)") }
                    }
                )
                CafeTab.Orders -> OrdersScreen(
                    ordersState = UiState.Success(emptyList()),
                    isSignedIn = false,
                    onSignInRequested = { onTriggerGoogleSignIn {} }
                )
                CafeTab.Account -> AccountScreen(
                    isSignedIn = false,
                    displayName = "Guest User",
                    email = "",
                    effectiveRole = UserRole.Customer,
                    branchAdminAssignment = null,
                    ordersState = UiState.Success(emptyList()),
                    notificationsEnabled = false,
                    onRequestNotificationPermission = {},
                    onSignInClick = { onTriggerGoogleSignIn {} },
                    onSignOutClick = {},
                    onOpenSuperAdminDashboard = {},
                    onOpenBranchAdminDashboard = {},
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = onToggleDarkMode,
                    defaultFeedbackBranchId = sessionState.selectedBranchId
                )
            }

            FloatingBucketFab(
                itemCount = sessionState.totalItemCount,
                totalPkr = sessionState.subtotalPkr,
                onClick = { showBucketSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }
    }

    if (showPickupBranchSheet) {
        PickupBranchBottomSheet(
            branches = BranchDirectory.defaultBranches,
            selectedBranchId = sessionState.selectedBranchId,
            onSelectBranch = { branch ->
                sessionCart.selectPickupBranch(branch.id)
                showPickupBranchSheet = false
            },
            onDismiss = { showPickupBranchSheet = false },
            deviceLatitude = sessionState.deviceLatitude,
            deviceLongitude = sessionState.deviceLongitude,
            detectedGpsAddress = sessionState.detectedGpsAddress,
            isLocatingGps = isLocatingGps,
            onUseCurrentLocation = { triggerGpsDetection(true) },
            onSwitchToDelivery = {
                sessionCart.setOrderType(OrderType.DELIVERY)
                showPickupBranchSheet = false
                showSavedAddressesSheet = true
            }
        )
    }

    if (showSavedAddressesSheet) {
        SavedAddressesBottomSheet(
            savedAddresses = sessionState.guestAddresses,
            selectedAddressStreet = sessionState.selectedAddressStreet,
            onSelectAddress = { addr -> sessionCart.selectDeliveryAddress(addr) },
            onAddNewAddress = { label, street, area ->
                val saved = sessionCart.addGuestAddress(label, street, area)
                scope.launch {
                    snackbarHostState.showSnackbar("Routed to ${saved.branchName} (${saved.branchId})")
                }
            },
            onStartOrder = { showSavedAddressesSheet = false },
            onDismiss = { showSavedAddressesSheet = false },
            branches = BranchDirectory.defaultBranches,
            orderType = sessionState.orderType,
            selectedBranchId = sessionState.selectedBranchId,
            deviceLatitude = sessionState.deviceLatitude,
            deviceLongitude = sessionState.deviceLongitude,
            detectedGpsAddress = sessionState.detectedGpsAddress,
            isLocatingGps = isLocatingGps,
            onOrderTypeChange = { sessionCart.setOrderType(it) },
            onUseCurrentLocation = { triggerGpsDetection(true) },
            onSelectPickupBranch = { branch ->
                if (sessionState.orderType == OrderType.PICKUP) {
                    sessionCart.selectPickupBranch(branch.id)
                } else {
                    sessionCart.selectQuickDeliveryArea(branch.areas.firstOrNull() ?: branch.displayShortLabel(), branch.id)
                }
                scope.launch {
                    snackbarHostState.showSnackbar("Selected ${branch.displayShortLabel()}")
                }
            },
            onSelectQuickArea = { area, branchId ->
                sessionCart.selectQuickDeliveryArea(area, branchId)
                val matched = BranchDirectory.byId(branchId)
                scope.launch {
                    snackbarHostState.showSnackbar("$area routed to ${matched.displayShortLabel()}")
                }
            }
        )
    }

    if (showBucketSheet) {
        BucketCheckoutSheet(
            sessionState = sessionState,
            isSignedIn = false,
            defaultCustomerName = "Lahori Guest",
            isSubmitting = false,
            onAddItem = { sessionCart.addToCart(it) },
            onRemoveItem = { sessionCart.decrementCartItem(it) },
            onApplyPromoCode = { code, onResult ->
                val clean = code.trim().uppercase()
                val promo = SeedMenuData.defaultPromotions.firstOrNull {
                    it.code.equals(clean, ignoreCase = true) && it.active
                }
                val subtotal = sessionState.subtotalPkr
                when {
                    clean.isBlank() -> onResult(false, "Please enter a promo code")
                    promo == null -> onResult(false, "Promo code '$clean' is invalid or expired.")
                    subtotal < promo.minOrderPkr -> onResult(
                        false,
                        "Minimum order of Rs. ${promo.minOrderPkr} required for ${promo.code}."
                    )
                    else -> {
                        val discount = promo.computeDiscount(subtotal)
                        if (discount <= 0L) {
                            onResult(false, "Promo code '$clean' does not apply to this order.")
                        } else {
                            sessionCart.applyPromo(promo.code, discount)
                            val desc = if (promo.discountType.equals("PERCENT", ignoreCase = true)) {
                                "${promo.discountValue}% off (-$discount PKR)"
                            } else {
                                "Rs. $discount off"
                            }
                            onResult(true, "Applied ${promo.code}: $desc")
                        }
                    }
                }
            },
            onClearPromoCode = { sessionCart.clearPromo() },
            onPlaceOrder = { name, phone, _ ->
                val chosenBranch = BranchDirectory.byId(sessionState.selectedBranchId)
                val address = if (sessionState.orderType == OrderType.DELIVERY) {
                    sessionState.selectedAddressStreet.ifBlank { "${chosenBranch.displayShortLabel()} Delivery" }
                } else {
                    "Self Pickup — ${chosenBranch.shortAddress}"
                }
                sessionCart.setPendingGuestOrder(
                    PendingGuestOrder(
                        cartItems = sessionState.cartItems,
                        orderType = sessionState.orderType,
                        deliveryAddress = address,
                        branchId = sessionState.selectedBranchId,
                        customerName = name,
                        customerPhone = phone,
                        guestSessionId = sessionState.guestSessionId,
                        promoCode = sessionState.appliedPromoLabel,
                        discountPkr = sessionState.appliedDiscountPkr
                    )
                )
                showBucketSheet = false
                onTriggerGoogleSignIn {}
            },
            onDismiss = { showBucketSheet = false }
        )
    }
}

@Composable
private fun AuthenticatedCafeShell(
    userId: String,
    userEmail: String,
    userDisplayName: String,
    isEmailVerified: Boolean,
    sessionCart: SessionCartController,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    pendingNotificationOrderId: String?,
    pendingNotificationEventType: String?,
    onConsumeNotificationOrderId: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val viewModel: LahoriViewModel = viewModel(
        key = userId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                LahoriViewModel(
                    repository = CafeRepository(db),
                    currentUserId = userId,
                    currentUserEmail = userEmail,
                    isEmailVerified = isEmailVerified,
                    sessionCart = sessionCart
                )
            }
        }
    )

    val sessionState by sessionCart.state.collectAsStateWithLifecycle()
    val branchesList by viewModel.branchesList.collectAsStateWithLifecycle()
    val menuState by viewModel.menuState.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val savedAddresses by viewModel.savedAddresses.collectAsStateWithLifecycle()
    val effectiveRole by viewModel.effectiveRole.collectAsStateWithLifecycle()
    val branchAdminAssignment by viewModel.branchAdminAssignment.collectAsStateWithLifecycle()
    val customerOrdersState by viewModel.customerOrdersState.collectAsStateWithLifecycle()
    val customerReviewsState by viewModel.customerReviewsState.collectAsStateWithLifecycle()
    val customerFeedbackState by viewModel.customerFeedbackState.collectAsStateWithLifecycle()
    val branchOrdersState by viewModel.branchOrdersState.collectAsStateWithLifecycle()
    val superAdminOrdersState by viewModel.superAdminOrdersState.collectAsStateWithLifecycle()
    val allBranchAdmins by viewModel.allBranchAdmins.collectAsStateWithLifecycle()
    val promotionsList by viewModel.promotionsList.collectAsStateWithLifecycle()
    val allBranchBanners by viewModel.allBranchBanners.collectAsStateWithLifecycle()
    val branchAdminPromotions by viewModel.branchAdminPromotions.collectAsStateWithLifecycle()
    val branchAdminBanners by viewModel.branchAdminBanners.collectAsStateWithLifecycle()
    val allReviews by viewModel.allReviews.collectAsStateWithLifecycle()
    val allFeedback by viewModel.allFeedback.collectAsStateWithLifecycle()
    val pendingSurveyOrder by viewModel.pendingSurveyOrder.collectAsStateWithLifecycle()
    val isSubmittingReview by viewModel.isSubmittingReview.collectAsStateWithLifecycle()
    val isSubmittingFeedback by viewModel.isSubmittingFeedback.collectAsStateWithLifecycle()
    val isSavingStaff by viewModel.isSavingStaff.collectAsStateWithLifecycle()
    val isSavingBranch by viewModel.isSavingBranch.collectAsStateWithLifecycle()
    val isSavingMenuItem by viewModel.isSavingMenuItem.collectAsStateWithLifecycle()
    val isSavingBranchPromo by viewModel.isSavingBranchPromo.collectAsStateWithLifecycle()
    val isSavingBranchBanner by viewModel.isSavingBranchBanner.collectAsStateWithLifecycle()
    val branchAssignmentLoaded by viewModel.branchAssignmentLoaded.collectAsStateWithLifecycle()
    val trackedOrderState by viewModel.trackedOrderState.collectAsStateWithLifecycle()
    val fcmTokenRegistered by viewModel.fcmTokenRegistered.collectAsStateWithLifecycle()
    val newBranchOrderAlert by viewModel.newBranchOrderAlert.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    var isLocatingGps by remember { mutableStateOf(false) }

    val triggerGpsDetection: (Boolean) -> Unit = { keepOrderType ->
        if (!isLocatingGps) {
            isLocatingGps = true
            scope.launch {
                val gpsRes = LocationHelper.fetchCurrentLocationWithAddress(context)
                isLocatingGps = false
                gpsRes.onSuccess { loc ->
                    val nearest = BranchDirectory.nearestBranch(
                        lat = loc.latitude,
                        lng = loc.longitude,
                        branches = branchesList
                    )
                    sessionCart.applyDetectedGpsLocation(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        formattedAddress = loc.formattedAddress,
                        areaLabel = loc.areaLabel,
                        nearestBranchId = nearest.id,
                        keepOrderType = keepOrderType
                    )
                    val dist = BranchDirectory.distanceToKm(nearest, loc.latitude, loc.longitude)
                    val distText = if (dist != null) " (~${String.format(Locale.US, "%.1f", dist)} km)" else ""
                    snackbarHostState.showSnackbar("Nearest branch: ${nearest.displayShortLabel()}$distText")
                }.onFailure { err ->
                    snackbarHostState.showSnackbar(err.localizedMessage ?: "Could not detect GPS location")
                }
            }
        }
    }

    var notificationsGranted by remember {
        mutableStateOf(OrderNotificationHelper.hasNotificationPermission(context))
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsGranted = granted
        if (granted) {
            viewModel.syncFcmRegistrationToken()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsGranted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(pendingNotificationOrderId, pendingNotificationEventType, effectiveRole) {
        val targetOrderId = pendingNotificationOrderId
        if (!targetOrderId.isNullOrBlank()) {
            if (pendingNotificationEventType == OrderNotificationHelper.EVENT_NEW_ORDER) {
                snackbarHostState.showSnackbar("New branch order #${targetOrderId.takeLast(6).uppercase()} is in your queue!")
            } else {
                viewModel.startTrackingOrder(targetOrderId)
            }
            onConsumeNotificationOrderId()
        }
    }

    LaunchedEffect(Unit) {
        FcmOrderEventBus.events.collect { pushEvent ->
            if (pushEvent.eventType == OrderNotificationHelper.EVENT_NEW_ORDER) {
                snackbarHostState.showSnackbar(
                    message = "${pushEvent.title}: ${pushEvent.body}",
                    withDismissAction = true
                )
            } else {
                val result = snackbarHostState.showSnackbar(
                    message = "${pushEvent.title}: ${pushEvent.body}",
                    actionLabel = "TRACK",
                    withDismissAction = true
                )
                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                    viewModel.startTrackingOrder(pushEvent.orderId)
                }
            }
        }
    }

    LaunchedEffect(newBranchOrderAlert) {
        val incoming = newBranchOrderAlert ?: return@LaunchedEffect
        OrderNotificationHelper.notifyNewBranchOrder(
            context = context,
            orderId = incoming.id,
            branchId = incoming.branchId,
            branchName = incoming.branchName,
            customerName = incoming.customerName,
            totalPkr = incoming.totalPkr,
            itemsSummary = incoming.itemsSummary,
            orderType = incoming.orderType
        )
    }

    // Fire real Android system notification whenever order status transitions in Firestore
    LaunchedEffect(statusBanner) {
        val banner = statusBanner ?: return@LaunchedEffect
        val (orderId, newStatus) = banner
        val order = (customerOrdersState as? UiState.Success)?.data?.firstOrNull { it.id == orderId }
        OrderNotificationHelper.notifyOrderStatus(
            context = context,
            orderId = orderId,
            status = newStatus,
            branchName = order?.branchName ?: "Lahori Food Cafe",
            itemsSummary = order?.itemsSummary ?: "Your order",
            orderType = order?.orderType ?: OrderType.DELIVERY.name
        )
        delay(4500L)
        viewModel.dismissStatusBanner()
    }

    LaunchedEffect(actionError) {
        val err = actionError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(err)
        viewModel.clearError()
    }

    var currentTab by remember { mutableStateOf(CafeTab.Explore) }
    var searchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showSavedAddressesSheet by remember { mutableStateOf(false) }
    var showPickupBranchSheet by remember { mutableStateOf(false) }
    var showBucketSheet by remember { mutableStateOf(false) }
    var isSubmittingOrder by remember { mutableStateOf(false) }
    var showSuperAdminDashboard by remember { mutableStateOf(false) }
    var showBranchAdminDashboard by remember { mutableStateOf(false) }

    val menuItems = (menuState as? UiState.Success)?.data ?: SeedMenuData.initialItems
    val favoriteIds = remember(userProfile) { userProfile?.favoriteItemIds?.toSet() ?: emptySet() }
    val cartQuantities = remember(sessionState.cartItems) {
        sessionState.cartItems.associate { it.menuItem.id to it.quantity }
    }
    val ordersList = (customerOrdersState as? UiState.Success)?.data ?: emptyList()
    val reviewsList = (customerReviewsState as? UiState.Success)?.data ?: emptyList()
    val feedbackList = (customerFeedbackState as? UiState.Success)?.data ?: emptyList()
    val reviewsByOrderId = remember(reviewsList) { reviewsList.associateBy { it.orderId } }
    val activeOrdersList = remember(ordersList) {
        ordersList.filter { OrderStatus.fromString(it.status) != OrderStatus.Delivered }
    }
    val activeOrderCount = activeOrdersList.size

    // Strict branch-specific filtering for customer-facing seasonal banners & temporary promo codes
    val selectedBranchLabel = remember(sessionState.selectedBranchId, branchesList) {
        BranchDirectory.shortPickerLabel(sessionState.selectedBranchId, branchesList)
    }
    val selectedBranchBanners = remember(allBranchBanners, sessionState.selectedBranchId) {
        val now = System.currentTimeMillis()
        allBranchBanners.filter { it.isVisibleForBranch(sessionState.selectedBranchId, now) }
    }
    val selectedBranchPromos = remember(promotionsList, sessionState.selectedBranchId) {
        val now = System.currentTimeMillis()
        promotionsList.filter {
            it.branchId == sessionState.selectedBranchId &&
                it.isCurrentlyValidForBranch(sessionState.selectedBranchId, now)
        }
    }

    val isSuperAdminUser =
        (userEmail.equals(SUPER_ADMIN_EMAIL, ignoreCase = true) && isEmailVerified) ||
            effectiveRole == UserRole.SuperAdmin

    if (showSuperAdminDashboard && userEmail.equals(SUPER_ADMIN_EMAIL, ignoreCase = true)) {
        SuperAdminDashboardScreen(
            allOrdersState = superAdminOrdersState,
            menuItems = menuItems,
            branchAdmins = allBranchAdmins,
            promotions = promotionsList,
            reviews = allReviews,
            feedbackList = allFeedback,
            onDeleteReview = { viewModel.deleteReview(it) },
            onDeleteFeedback = { viewModel.deleteFeedback(it) },
            isSavingStaff = isSavingStaff,
            isSavingMenuItem = isSavingMenuItem,
            branches = branchesList,
            isSavingBranch = isSavingBranch,
            onSaveBranch = { branchId, name, shortLabel, shortAddress, areas, active, sortOrder, lat, lng, mapUrl, openHours, done ->
                viewModel.saveBranch(
                    branchId = branchId,
                    name = name,
                    shortLabel = shortLabel,
                    shortAddress = shortAddress,
                    areas = areas,
                    active = active,
                    sortOrder = sortOrder,
                    latitude = lat,
                    longitude = lng,
                    mapUrl = mapUrl,
                    openHours = openHours,
                    onDone = done
                )
            },
            onDeleteBranch = { viewModel.deleteBranch(it) },
            onSeedBranches = { viewModel.seedBranchesNow() },
            onUpdateOrderStatus = { orderId, status -> viewModel.updateOrderStatus(orderId, status) },
            onSaveMenuItem = { id, name, desc, cat, price, imgKey, imgUrl, pickedUri, badge, avail, sort, onError, done ->
                viewModel.saveMenuItem(
                    context = context,
                    itemId = id,
                    name = name,
                    description = desc,
                    category = cat,
                    pricePkr = price,
                    imageKey = imgKey,
                    imageUrl = imgUrl,
                    pickedImageUri = pickedUri,
                    badgeText = badge,
                    available = avail,
                    sortOrder = sort,
                    onError = onError,
                    onDone = done
                )
            },
            onDeleteMenuItem = { viewModel.deleteMenuItem(it) },
            onSeedMenu = { viewModel.seedMenuNow() },
            onAddBranchAdmin = { email, role, branchId, done ->
                viewModel.addBranchManager(
                    email = email,
                    role = role,
                    branchId = branchId,
                    onDone = done
                )
            },
            onRemoveBranchAdmin = { viewModel.removeBranchManager(it) },
            onSavePromotion = { code, discountType, discountValue, minOrderPkr, active, done ->
                viewModel.savePromotion(
                    code = code,
                    discountType = discountType,
                    discountValue = discountValue,
                    minOrderPkr = minOrderPkr,
                    active = active,
                    onDone = done
                )
            },
            onDeletePromotion = { viewModel.deletePromotion(it) },
            onBack = { showSuperAdminDashboard = false }
        )
        return
    }

    if (!isSuperAdminUser && !branchAssignmentLoaded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(color = LahoriRed)
                Text(
                    text = "Verifying staff & branch access...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val assignedBranchEntry = branchAdminAssignment
    if (!isSuperAdminUser && assignedBranchEntry != null) {
        Box(modifier = Modifier.fillMaxSize()) {
            BranchAdminDashboardScreen(
                branchId = assignedBranchEntry.branchId,
                branchName = assignedBranchEntry.branchName,
                ordersState = branchOrdersState,
                onUpdateOrderStatus = { orderId, status -> viewModel.updateOrderStatus(orderId, status) },
                managerEmail = userEmail,
                onSignOut = {
                    scope.launch {
                        withContext(NonCancellable) {
                            viewModel.unregisterDeviceFcmToken()
                        }
                        onSignOut()
                    }
                },
                branches = branchesList,
                newOrderAlert = newBranchOrderAlert,
                onDismissNewOrderAlert = { viewModel.dismissNewBranchOrderAlert() },
                branchPromotions = branchAdminPromotions,
                branchBanners = branchAdminBanners,
                isSavingBranchPromo = isSavingBranchPromo,
                isSavingBranchBanner = isSavingBranchBanner,
                onSaveBranchPromo = { promoDocId, code, discountType, discountValue, minOrderPkr, active, expiresAt, onError, onDone ->
                    viewModel.savePromotion(
                        code = code,
                        discountType = discountType,
                        discountValue = discountValue,
                        minOrderPkr = minOrderPkr,
                        active = active,
                        branchId = assignedBranchEntry.branchId,
                        branchName = assignedBranchEntry.branchName,
                        expiresAt = expiresAt,
                        promoDocId = promoDocId,
                        onError = onError,
                        onDone = {
                            onDone()
                            scope.launch {
                                snackbarHostState.showSnackbar("Saved branch promo $code")
                            }
                        }
                    )
                },
                onDeleteBranchPromo = { promoId ->
                    viewModel.deletePromotion(promoId)
                    scope.launch { snackbarHostState.showSnackbar("Promo code deleted") }
                },
                onSaveBranchBanner = { bannerId, title, subtitle, promoCode, badgeText, imageUrl, pickedUri, targetCategory, active, expiresAt, onError, onDone ->
                    viewModel.saveBranchBanner(
                        context = context,
                        bannerId = bannerId,
                        branchId = assignedBranchEntry.branchId,
                        branchName = assignedBranchEntry.branchName,
                        title = title,
                        subtitle = subtitle,
                        promoCode = promoCode,
                        badgeText = badgeText,
                        imageUrl = imageUrl,
                        pickedImageUri = pickedUri,
                        targetCategory = targetCategory,
                        active = active,
                        expiresAt = expiresAt,
                        onError = onError,
                        onDone = {
                            onDone()
                            scope.launch {
                                snackbarHostState.showSnackbar("Seasonal banner published for ${assignedBranchEntry.branchName}")
                            }
                        }
                    )
                },
                onDeleteBranchBanner = { bannerId ->
                    viewModel.deleteBranchBanner(bannerId)
                    scope.launch { snackbarHostState.showSnackbar("Seasonal banner removed") }
                }
            )
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
        }
        return
    }

    if (showBranchAdminDashboard && assignedBranchEntry != null) {
        BranchAdminDashboardScreen(
            branchId = assignedBranchEntry.branchId,
            branchName = assignedBranchEntry.branchName,
            ordersState = branchOrdersState,
            onUpdateOrderStatus = { orderId, status -> viewModel.updateOrderStatus(orderId, status) },
            onBack = { showBranchAdminDashboard = false },
            managerEmail = userEmail,
            branches = branchesList,
            newOrderAlert = newBranchOrderAlert,
            onDismissNewOrderAlert = { viewModel.dismissNewBranchOrderAlert() },
            branchPromotions = branchAdminPromotions,
            branchBanners = branchAdminBanners,
            isSavingBranchPromo = isSavingBranchPromo,
            isSavingBranchBanner = isSavingBranchBanner,
            onSaveBranchPromo = { promoDocId, code, discountType, discountValue, minOrderPkr, active, expiresAt, onError, onDone ->
                viewModel.savePromotion(
                    code = code,
                    discountType = discountType,
                    discountValue = discountValue,
                    minOrderPkr = minOrderPkr,
                    active = active,
                    branchId = assignedBranchEntry.branchId,
                    branchName = assignedBranchEntry.branchName,
                    expiresAt = expiresAt,
                    promoDocId = promoDocId,
                    onError = onError,
                    onDone = onDone
                )
            },
            onDeleteBranchPromo = { viewModel.deletePromotion(it) },
            onSaveBranchBanner = { bannerId, title, subtitle, promoCode, badgeText, imageUrl, pickedUri, targetCategory, active, expiresAt, onError, onDone ->
                viewModel.saveBranchBanner(
                    context = context,
                    bannerId = bannerId,
                    branchId = assignedBranchEntry.branchId,
                    branchName = assignedBranchEntry.branchName,
                    title = title,
                    subtitle = subtitle,
                    promoCode = promoCode,
                    badgeText = badgeText,
                    imageUrl = imageUrl,
                    pickedImageUri = pickedUri,
                    targetCategory = targetCategory,
                    active = active,
                    expiresAt = expiresAt,
                    onError = onError,
                    onDone = onDone
                )
            },
            onDeleteBranchBanner = { viewModel.deleteBranchBanner(it) }
        )
        return
    }

    trackedOrderState?.let { activeTrackedState ->
        LiveOrderTrackingScreen(
            trackedOrderState = activeTrackedState,
            allActiveOrders = activeOrdersList,
            reviewsByOrderId = reviewsByOrderId,
            onSelectTrackedOrder = { orderId -> viewModel.startTrackingOrder(orderId) },
            onRateOrder = { order -> viewModel.openOrderSurvey(order) },
            onBack = { viewModel.stopTrackingOrder() },
            snackbarHostState = snackbarHostState
        )
        pendingSurveyOrder?.let { surveyOrder ->
            OrderReviewSurveySheet(
                order = surveyOrder,
                existingReview = reviewsByOrderId[surveyOrder.id],
                isSubmitting = isSubmittingReview,
                onSubmit = { rating, comment ->
                    viewModel.submitOrderReview(surveyOrder, rating, comment) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Thank you for rating your order ($rating/5 stars)!")
                        }
                    }
                },
                onDismiss = { viewModel.dismissOrderSurvey(surveyOrder.id) }
            )
        }
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LahoriTopHeader(
                orderType = sessionState.orderType,
                selectedBranchId = sessionState.selectedBranchId,
                selectedAddressStreet = sessionState.selectedAddressStreet,
                searchVisible = searchVisible,
                searchQuery = searchQuery,
                onToggleSearch = {
                    searchVisible = !searchVisible
                    if (!searchVisible) searchQuery = ""
                },
                onSearchQueryChange = { searchQuery = it },
                onDeliveryClick = {
                    sessionCart.setOrderType(OrderType.DELIVERY)
                    showSavedAddressesSheet = true
                },
                onPickupClick = {
                    sessionCart.setOrderType(OrderType.PICKUP)
                    showPickupBranchSheet = true
                },
                branches = branchesList,
                deviceLatitude = sessionState.deviceLatitude,
                deviceLongitude = sessionState.deviceLongitude,
                onLocationBannerClick = {
                    if (sessionState.orderType == OrderType.DELIVERY) {
                        showSavedAddressesSheet = true
                    } else {
                        showPickupBranchSheet = true
                    }
                }
            )
        },
        bottomBar = {
            LahoriBottomBar(
                currentTab = currentTab,
                activeOrderCount = activeOrderCount,
                onTabSelected = { currentTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                CafeTab.Explore -> ExploreScreen(
                    menuItems = menuItems,
                    favoriteIds = favoriteIds,
                    cartQuantities = cartQuantities,
                    searchQuery = searchQuery,
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onAddToBucket = { item ->
                        sessionCart.addToCart(item)
                        scope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar("Added ${item.name} to Bucket")
                        }
                    },
                    selectedBranchLabel = selectedBranchLabel,
                    branchBanners = selectedBranchBanners,
                    branchPromos = selectedBranchPromos,
                    appliedPromoCode = sessionState.appliedPromoLabel,
                    onApplyBranchPromo = { code ->
                        if (sessionState.cartItems.isEmpty()) {
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar(
                                    "Add items to your bucket first, then tap $code at checkout!"
                                )
                            }
                        } else {
                            viewModel.validateAndApplyPromoCode(code) { _, msg ->
                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(msg)
                                }
                            }
                        }
                    }
                )
                CafeTab.Menu -> MenuScreen(
                    menuItems = menuItems,
                    favoriteIds = favoriteIds,
                    cartQuantities = cartQuantities,
                    searchQuery = searchQuery,
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onAddToBucket = { sessionCart.addToCart(it) }
                )
                CafeTab.Rewards -> RewardsScreen(
                    orders = ordersList,
                    appliedPromoLabel = sessionState.appliedPromoLabel,
                    onApplyVoucher = { code, discount ->
                        sessionCart.applyPromo(code, discount)
                        scope.launch { snackbarHostState.showSnackbar("Applied $code (-Rs. $discount)") }
                    }
                )
                CafeTab.Orders -> OrdersScreen(
                    ordersState = customerOrdersState,
                    isSignedIn = true,
                    onSignInRequested = {},
                    reviewsByOrderId = reviewsByOrderId,
                    onRateOrder = { order -> viewModel.openOrderSurvey(order) },
                    onTrackOrder = { order -> viewModel.startTrackingOrder(order.id) }
                )
                CafeTab.Account -> AccountScreen(
                    isSignedIn = true,
                    displayName = userDisplayName,
                    email = userEmail,
                    effectiveRole = effectiveRole,
                    branchAdminAssignment = branchAdminAssignment,
                    ordersState = customerOrdersState,
                    notificationsEnabled = notificationsGranted,
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onSignInClick = {},
                    onSignOutClick = {
                        scope.launch {
                            withContext(NonCancellable) {
                                viewModel.unregisterDeviceFcmToken()
                            }
                            onSignOut()
                        }
                    },
                    onOpenSuperAdminDashboard = { showSuperAdminDashboard = true },
                    onOpenBranchAdminDashboard = { showBranchAdminDashboard = true },
                    reviewsByOrderId = reviewsByOrderId,
                    onRateOrder = { order -> viewModel.openOrderSurvey(order) },
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = onToggleDarkMode,
                    fcmTokenRegistered = fcmTokenRegistered,
                    defaultFeedbackBranchId = sessionState.selectedBranchId,
                    recentFeedback = feedbackList,
                    isSubmittingFeedback = isSubmittingFeedback,
                    onSubmitCafeFeedback = { branchId, foodRating, serviceRating, comment, onComplete ->
                        viewModel.submitCafeFeedback(
                            branchId = branchId,
                            foodRating = foodRating,
                            serviceRating = serviceRating,
                            comment = comment,
                            onSuccess = {
                                onComplete()
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Feedback saved! Food $foodRating/5 • Service $serviceRating/5"
                                    )
                                }
                            }
                        )
                    },
                    branches = branchesList
                )
            }

            // Live In-App Order Status Push Notification Banner
            AnimatedVisibility(
                visible = statusBanner != null,
                enter = slideInVertically { -it },
                exit = slideOutVertically { -it },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                statusBanner?.let { (orderId, st) ->
                    val matchingOrder = ordersList.firstOrNull { it.id == orderId }
                    val orderTypeStr = matchingOrder?.orderType ?: OrderType.DELIVERY.name
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = LahoriBlack,
                        border = BorderStroke(1.5.dp, LahoriGold),
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = LahoriGold)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${st.notificationTitleFor(orderTypeStr)} (#${orderId.takeLast(6).uppercase()})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = LahoriWhite
                                )
                                Text(
                                    text = st.notificationMessageFor(orderTypeStr),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LahoriWhite.copy(alpha = 0.8f)
                                )
                            }
                            IconButton(onClick = { viewModel.dismissStatusBanner() }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Dismiss alert", tint = LahoriWhite)
                            }
                        }
                    }
                }
            }

            FloatingBucketFab(
                itemCount = sessionState.totalItemCount,
                totalPkr = sessionState.subtotalPkr,
                onClick = { showBucketSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }
    }

    if (showPickupBranchSheet) {
        PickupBranchBottomSheet(
            branches = branchesList,
            selectedBranchId = sessionState.selectedBranchId,
            onSelectBranch = { branch ->
                sessionCart.selectPickupBranch(branch.id)
                showPickupBranchSheet = false
            },
            onDismiss = { showPickupBranchSheet = false },
            deviceLatitude = sessionState.deviceLatitude,
            deviceLongitude = sessionState.deviceLongitude,
            detectedGpsAddress = sessionState.detectedGpsAddress,
            isLocatingGps = isLocatingGps,
            onUseCurrentLocation = { triggerGpsDetection(true) },
            onSwitchToDelivery = {
                sessionCart.setOrderType(OrderType.DELIVERY)
                showPickupBranchSheet = false
                showSavedAddressesSheet = true
            }
        )
    }

    if (showSavedAddressesSheet) {
        SavedAddressesBottomSheet(
            savedAddresses = savedAddresses,
            selectedAddressStreet = sessionState.selectedAddressStreet,
            onSelectAddress = { addr -> sessionCart.selectDeliveryAddress(addr) },
            onAddNewAddress = { label, street, area ->
                viewModel.saveAddressAndRoute(label, street, area) { saved ->
                    scope.launch {
                        snackbarHostState.showSnackbar("Routed to ${saved.branchName} (${saved.branchId})")
                    }
                }
            },
            onStartOrder = { showSavedAddressesSheet = false },
            onDismiss = { showSavedAddressesSheet = false },
            branches = branchesList,
            orderType = sessionState.orderType,
            selectedBranchId = sessionState.selectedBranchId,
            deviceLatitude = sessionState.deviceLatitude,
            deviceLongitude = sessionState.deviceLongitude,
            detectedGpsAddress = sessionState.detectedGpsAddress,
            isLocatingGps = isLocatingGps,
            onOrderTypeChange = { sessionCart.setOrderType(it) },
            onUseCurrentLocation = { triggerGpsDetection(true) },
            onSelectPickupBranch = { branch ->
                if (sessionState.orderType == OrderType.PICKUP) {
                    sessionCart.selectPickupBranch(branch.id)
                } else {
                    sessionCart.selectQuickDeliveryArea(branch.areas.firstOrNull() ?: branch.displayShortLabel(), branch.id)
                }
                scope.launch {
                    snackbarHostState.showSnackbar("Selected ${branch.displayShortLabel()}")
                }
            },
            onSelectQuickArea = { area, branchId ->
                sessionCart.selectQuickDeliveryArea(area, branchId)
                val matched = BranchDirectory.byId(branchId, branchesList)
                scope.launch {
                    snackbarHostState.showSnackbar("$area routed to ${matched.displayShortLabel()}")
                }
            }
        )
    }

    if (showBucketSheet) {
        BucketCheckoutSheet(
            sessionState = sessionState,
            isSignedIn = true,
            defaultCustomerName = userDisplayName,
            isSubmitting = isSubmittingOrder,
            onAddItem = { sessionCart.addToCart(it) },
            onRemoveItem = { sessionCart.decrementCartItem(it) },
            onApplyPromoCode = { code, onResult -> viewModel.validateAndApplyPromoCode(code, onResult) },
            onClearPromoCode = { sessionCart.clearPromo() },
            onPlaceOrder = { name, phone, guestCheckout ->
                isSubmittingOrder = true
                viewModel.submitOrder(name, phone, guestCheckout) { orderId ->
                    isSubmittingOrder = false
                    showBucketSheet = false
                    currentTab = CafeTab.Orders
                    viewModel.startTrackingOrder(orderId)
                    val branch = BranchDirectory.byId(sessionState.selectedBranchId, branchesList)
                    OrderNotificationHelper.notifyOrderStatus(
                        context = context,
                        orderId = orderId,
                        status = OrderStatus.Confirmed,
                        branchName = branch.name,
                        itemsSummary = "Order confirmed!",
                        orderType = sessionState.orderType.name
                    )
                }
            },
            onDismiss = { showBucketSheet = false },
            branches = branchesList,
            branchPromos = selectedBranchPromos
        )
    }

    pendingSurveyOrder?.let { surveyOrder ->
        OrderReviewSurveySheet(
            order = surveyOrder,
            existingReview = reviewsByOrderId[surveyOrder.id],
            isSubmitting = isSubmittingReview,
            onSubmit = { rating, comment ->
                viewModel.submitOrderReview(surveyOrder, rating, comment) {
                    scope.launch {
                        snackbarHostState.showSnackbar("Thank you for rating your order ($rating/5 stars)!")
                    }
                }
            },
            onDismiss = { viewModel.dismissOrderSurvey(surveyOrder.id) }
        )
    }
}
