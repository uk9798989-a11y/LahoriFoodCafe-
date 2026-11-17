package com.example.data

import android.content.Context
import com.example.R
import com.example.model.BranchAdminEntry
import com.example.model.BranchDirectory
import com.example.model.CafeOrder
import com.example.model.CartItem
import com.example.model.MenuItem
import com.example.model.OrderLineItem
import com.example.model.OrderReview
import com.example.model.OrderStatus
import com.example.model.OrderType
import com.example.model.Promotion
import com.example.model.SUPER_ADMIN_EMAIL
import com.example.model.SavedAddress
import com.example.model.SeedMenuData
import com.example.model.UserProfile
import com.example.model.UserRole
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CafeRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // 1. Real-time Menu Items
    fun observeMenuItems(): Flow<List<MenuItem>> {
        val path = "menu_items"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(MenuItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.sortOrder }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun seedInitialMenu(): Result<Unit> {
        val path = "menu_items"
        return try {
            requireUserId()
            val existing = db.collection(path).limit(1).get().await()
            if (!existing.isEmpty) return Result.success(Unit)
            val batch = db.batch()
            SeedMenuData.initialItems.forEach { item ->
                val docId = item.id.removePrefix("seed_")
                val docRef = db.collection(path).document(docId)
                val payload = mapOf(
                    "name" to item.name,
                    "description" to item.description,
                    "category" to item.category,
                    "pricePkr" to item.pricePkr,
                    "imageKey" to item.imageKey,
                    "badgeText" to item.badgeText,
                    "available" to item.available,
                    "sortOrder" to item.sortOrder,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(docRef, payload)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveMenuItem(
        itemId: String?,
        name: String,
        description: String,
        category: String,
        pricePkr: Long,
        imageKey: String,
        badgeText: String,
        available: Boolean,
        sortOrder: Long
    ): Result<Unit> {
        var docPath = "menu_items"
        return try {
            requireUserId()
            val trimmedName = name.trim()
            val trimmedDesc = description.trim()
            val trimmedCategory = category.trim()
            require(trimmedName.length in 1..80) { "Name must be 1 to 80 characters" }
            require(trimmedDesc.length <= 300) { "Description must be 300 characters or less" }
            require(trimmedCategory.length in 1..40) { "Category must be 1 to 40 characters" }
            require(pricePkr in 1..100000) { "Price must be between 1 and 100,000 PKR" }

            if (itemId.isNullOrBlank()) {
                val docRef = db.collection("menu_items").document()
                docPath = docRef.path
                val payload = mapOf(
                    "name" to trimmedName,
                    "description" to trimmedDesc,
                    "category" to trimmedCategory,
                    "pricePkr" to pricePkr,
                    "imageKey" to imageKey.ifBlank { "zinger_burger" },
                    "badgeText" to badgeText.trim().take(30),
                    "available" to available,
                    "sortOrder" to sortOrder.coerceIn(0L, 10000L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val docRef = db.collection("menu_items").document(itemId)
                docPath = docRef.path
                val updates = mapOf(
                    "name" to trimmedName,
                    "description" to trimmedDesc,
                    "category" to trimmedCategory,
                    "pricePkr" to pricePkr,
                    "imageKey" to imageKey.ifBlank { "zinger_burger" },
                    "badgeText" to badgeText.trim().take(30),
                    "available" to available,
                    "sortOrder" to sortOrder.coerceIn(0L, 10000L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteMenuItem(itemId: String): Result<Unit> {
        val docPath = "menu_items/$itemId"
        return try {
            requireUserId()
            require(itemId.isNotBlank()) { "Menu item ID required" }
            db.collection("menu_items").document(itemId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // 2. User Profile & Favorites
    suspend fun ensureUserProfile(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val docPath = "users/$uid"
        return try {
            val user = auth.currentUser!!
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val isSuper = user.email.equals(SUPER_ADMIN_EMAIL, ignoreCase = true) && user.isEmailVerified
                val role = if (isSuper) UserRole.SuperAdmin.name else UserRole.Customer.name
                val payload = mapOf(
                    "userId" to uid,
                    "email" to (user.email ?: "").take(120),
                    "displayName" to (user.displayName ?: "Lahori Guest").take(80),
                    "role" to role,
                    "assignedBranchId" to "",
                    "favoriteItemIds" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        val snapshotFlow = db.collection("users").document(userId).snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, path)
                }
                throw error
            }
        snapshotFlow.collect { emit(it) }
    }

    suspend fun toggleFavorite(userId: String, itemId: String, currentFavorites: List<String>): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed" }
            val updated = if (currentFavorites.contains(itemId)) {
                currentFavorites - itemId
            } else {
                (currentFavorites + itemId).take(50)
            }
            db.collection("users").document(uid).update(
                mapOf(
                    "favoriteItemIds" to updated,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // 3. Saved Addresses & Branch Routing
    fun observeSavedAddresses(userId: String): Flow<List<SavedAddress>> {
        val path = "users/$userId/addresses"
        return db.collection("users").document(userId).collection("addresses")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SavedAddress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun addSavedAddress(userId: String, label: String, streetAddress: String, area: String): Result<SavedAddress> {
        val path = "users/$userId/addresses"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val cleanLabel = label.trim().ifBlank { "Home" }.take(40)
            val cleanStreet = streetAddress.trim().take(200)
            val cleanArea = area.trim().take(60)
            require(cleanStreet.length >= 3) { "Please enter a street address (min 3 characters)" }
            require(cleanArea.isNotBlank()) { "Please enter a neighborhood/area" }

            val routedBranch = BranchDirectory.routeAddressToBranch("$cleanArea $cleanStreet")
            val docRef = db.collection("users").document(uid).collection("addresses").document()
            val payload = mapOf(
                "label" to cleanLabel,
                "streetAddress" to cleanStreet,
                "area" to cleanArea,
                "branchId" to routedBranch.id,
                "branchName" to routedBranch.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(
                SavedAddress(
                    id = docRef.id,
                    label = cleanLabel,
                    streetAddress = cleanStreet,
                    area = cleanArea,
                    branchId = routedBranch.id,
                    branchName = routedBranch.name
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // 4. Branch Admin Assignments
    fun observeMyBranchAdminAssignment(email: String): Flow<BranchAdminEntry?> {
        val path = "branch_admins"
        val cleanEmail = email.trim().lowercase()
        return db.collection(path)
            .whereEqualTo("email", cleanEmail)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(BranchAdminEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .firstOrNull()
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeAllBranchAdmins(): Flow<List<BranchAdminEntry>> {
        val path = "branch_admins"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(BranchAdminEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.branchId }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun addBranchAdmin(email: String, branchId: String): Result<Unit> {
        val path = "branch_admins"
        return try {
            requireUserId()
            val cleanEmail = email.trim().lowercase()
            require(cleanEmail.contains("@") && cleanEmail.length in 3..120) { "Enter a valid Gmail address" }
            val branch = BranchDirectory.byId(branchId)
            val docRef = db.collection(path).document()
            docRef.set(
                mapOf(
                    "email" to cleanEmail,
                    "branchId" to branch.id,
                    "branchName" to branch.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun removeBranchAdmin(adminId: String): Result<Unit> {
        val path = "branch_admins/$adminId"
        return try {
            requireUserId()
            db.collection("branch_admins").document(adminId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // 5. Orders (Customer, BranchAdmin, SuperAdmin)
    fun observeCustomerOrders(userId: String): Flow<List<CafeOrder>> {
        val path = "orders"
        return db.collection(path)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CafeOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeBranchOrders(adminEmail: String, branchId: String): Flow<List<CafeOrder>> {
        val path = "orders"
        val cleanEmail = adminEmail.trim().lowercase()
        return db.collection(path)
            .whereArrayContains("branchAdminEmails", cleanEmail)
            .whereEqualTo("branchId", branchId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CafeOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeAllOrders(): Flow<List<CafeOrder>> {
        val path = "orders"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CafeOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun placeOrder(
        cartItems: List<CartItem>,
        orderType: OrderType,
        deliveryAddress: String,
        branchId: String,
        customerName: String,
        customerPhone: String,
        guestCheckout: Boolean,
        guestSessionId: String,
        promoCode: String = "",
        discountPkr: Long = 0L
    ): Result<String> {
        var docPath = "orders"
        return try {
            val uid = requireUserId()
            val user = auth.currentUser!!
            require(cartItems.isNotEmpty()) { "Your bucket is empty" }
            val cleanName = customerName.trim().ifBlank { user.displayName ?: "Lahori Guest" }.take(80)
            val cleanPhone = customerPhone.trim().take(30)
            val cleanAddress = deliveryAddress.trim().take(250)
            val cleanPromo = promoCode.trim().uppercase().take(30)
            val branch = BranchDirectory.byId(branchId)

            val branchManagers = try {
                db.collection("branch_admins")
                    .whereEqualTo("branchId", branch.id)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.getString("email")?.lowercase() }
                    .distinct()
                    .take(10)
            } catch (_: Exception) {
                emptyList()
            }

            val lineItems = cartItems.take(30).map { cart ->
                OrderLineItem(
                    itemId = cart.menuItem.id,
                    name = cart.menuItem.name,
                    quantity = cart.quantity.toLong(),
                    unitPricePkr = cart.menuItem.pricePkr,
                    lineTotalPkr = cart.lineTotalPkr
                )
            }
            val subtotal = lineItems.sumOf { it.lineTotalPkr }.coerceAtLeast(1L)
            val deliveryFee = if (orderType == OrderType.DELIVERY) 150L else 0L
            val appliedDiscount = discountPkr.coerceIn(0L, subtotal)
            val total = (subtotal + deliveryFee - appliedDiscount).coerceAtLeast(1L)
            val summary = lineItems.joinToString(", ") { "${it.quantity}x ${it.name}" }.take(300)

            val docRef = db.collection("orders").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "guestSessionId" to if (guestCheckout) guestSessionId.take(40) else "",
                "guestCheckout" to guestCheckout,
                "customerName" to cleanName,
                "customerEmail" to (user.email ?: "").take(120),
                "customerPhone" to cleanPhone,
                "orderType" to orderType.name,
                "deliveryAddress" to cleanAddress,
                "branchId" to branch.id,
                "branchName" to branch.name,
                "branchAdminEmails" to branchManagers,
                "items" to lineItems.map { it.toMap() },
                "itemsSummary" to summary,
                "promoCode" to cleanPromo,
                "discountPkr" to appliedDiscount,
                "subtotalPkr" to subtotal,
                "deliveryFeePkr" to deliveryFee,
                "totalPkr" to total,
                "status" to OrderStatus.Confirmed.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: OrderStatus): Result<Unit> {
        val docPath = "orders/$orderId"
        return try {
            requireUserId()
            require(orderId.isNotBlank()) { "Order ID required" }
            db.collection("orders").document(orderId).update(
                mapOf(
                    "status" to newStatus.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // 6. Promotions (Firestore 'promotions' collection)
    fun observePromotions(): Flow<List<Promotion>> {
        val path = "promotions"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Promotion::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.code }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun seedInitialPromotions(): Result<Unit> {
        val path = "promotions"
        return try {
            requireUserId()
            val existing = db.collection(path).limit(1).get().await()
            if (!existing.isEmpty) return Result.success(Unit)
            val batch = db.batch()
            SeedMenuData.defaultPromotions.forEach { promo ->
                val docRef = db.collection(path).document(promo.id)
                batch.set(
                    docRef,
                    mapOf(
                        "code" to promo.code,
                        "discountType" to promo.discountType,
                        "discountValue" to promo.discountValue,
                        "minOrderPkr" to promo.minOrderPkr,
                        "active" to promo.active,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun savePromotion(
        code: String,
        discountType: String,
        discountValue: Long,
        minOrderPkr: Long,
        active: Boolean
    ): Result<Unit> {
        val cleanCode = code.trim().uppercase()
        val docId = cleanCode.lowercase().replace(Regex("[^a-z0-9_\\-]"), "_")
        val docPath = "promotions/$docId"
        return try {
            requireUserId()
            require(cleanCode.length in 2..30) { "Promo code must be 2 to 30 characters" }
            require(discountType in listOf("PERCENT", "FLAT")) { "Invalid discount type" }
            if (discountType == "PERCENT") {
                require(discountValue in 1..90) { "Percent discount must be 1% to 90%" }
            } else {
                require(discountValue in 1..100000) { "Flat discount must be 1 to 100,000 PKR" }
            }
            val docRef = db.collection("promotions").document(docId)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "code" to cleanCode,
                        "discountType" to discountType,
                        "discountValue" to discountValue,
                        "minOrderPkr" to minOrderPkr.coerceAtLeast(0L),
                        "active" to active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "code" to cleanCode,
                        "discountType" to discountType,
                        "discountValue" to discountValue,
                        "minOrderPkr" to minOrderPkr.coerceAtLeast(0L),
                        "active" to active,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deletePromotion(promoId: String): Result<Unit> {
        val docPath = "promotions/$promoId"
        return try {
            requireUserId()
            db.collection("promotions").document(promoId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun validatePromoCode(rawCode: String, subtotalPkr: Long): Result<Pair<Promotion, Long>> {
        val cleanCode = rawCode.trim().uppercase()
        val path = "promotions"
        return try {
            requireUserId()
            require(cleanCode.isNotBlank()) { "Please enter a promo code" }

            val querySnap = db.collection(path)
                .whereEqualTo("code", cleanCode)
                .limit(1)
                .get()
                .await()

            val promo: Promotion? = if (!querySnap.isEmpty) {
                querySnap.documents.first().toObject(Promotion::class.java)
            } else {
                // If SuperAdmin has not yet seeded /promotions in Firestore, allow built-in starter codes
                val anyPromo = db.collection(path).limit(1).get().await()
                if (anyPromo.isEmpty) {
                    SeedMenuData.defaultPromotions.firstOrNull { it.code.equals(cleanCode, ignoreCase = true) }
                } else {
                    null
                }
            }

            if (promo == null || !promo.active) {
                return Result.failure(IllegalArgumentException("Promo code '$cleanCode' is invalid or expired."))
            }
            if (subtotalPkr < promo.minOrderPkr) {
                return Result.failure(
                    IllegalArgumentException("Minimum order of Rs. ${promo.minOrderPkr} required for ${promo.code}.")
                )
            }
            val discount = promo.computeDiscount(subtotalPkr)
            if (discount <= 0L) {
                return Result.failure(IllegalArgumentException("Promo code '$cleanCode' does not apply to this order."))
            }
            Result.success(promo to discount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e !is IllegalArgumentException) {
                handleFirestoreError(e, OperationType.LIST, path)
            }
            Result.failure(e)
        }
    }

    // 7. Customer Order Reviews (Firestore 'reviews' collection)
    fun observeCustomerReviews(userId: String): Flow<List<OrderReview>> {
        val path = "reviews"
        return db.collection(path)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(OrderReview::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeAllReviews(): Flow<List<OrderReview>> {
        val path = "reviews"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(OrderReview::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun submitOrderReview(
        order: CafeOrder,
        rating: Int,
        comment: String
    ): Result<Unit> {
        val docPath = "reviews/${order.id}"
        return try {
            val uid = requireUserId()
            val user = auth.currentUser!!
            require(order.id.isNotBlank()) { "Order ID is required" }
            require(order.status == OrderStatus.Delivered.name) { "Only delivered orders can be reviewed" }
            require(rating in 1..5) { "Rating must be between 1 and 5 stars" }

            val cleanComment = comment.trim().take(400)
            val cleanCustomerName = order.customerName
                .ifBlank { user.displayName ?: "Lahori Customer" }
                .trim()
                .take(80)
            val cleanItemsSummary = order.itemsSummary
                .ifBlank { "Lahori Food Cafe Order" }
                .trim()
                .take(300)
            val branch = BranchDirectory.byId(order.branchId)

            val docRef = db.collection("reviews").document(order.id)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "rating" to rating.toLong(),
                        "comment" to cleanComment,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "orderId" to order.id,
                        "userId" to uid,
                        "customerName" to cleanCustomerName,
                        "branchId" to branch.id,
                        "branchName" to order.branchName.ifBlank { branch.name }.take(80),
                        "rating" to rating.toLong(),
                        "comment" to cleanComment,
                        "itemsSummary" to cleanItemsSummary,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }
}
