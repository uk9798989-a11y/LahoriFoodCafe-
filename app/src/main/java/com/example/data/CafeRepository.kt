package com.example.data

import android.content.Context
import com.example.BuildConfig
import com.example.R
import com.example.model.BranchAdminEntry
import com.example.model.BranchBanner
import com.example.model.BranchDirectory
import com.example.model.CafeBranch
import com.example.model.CafeFeedback
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
import java.security.MessageDigest

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

    private suspend fun resolveBranch(branchId: String): CafeBranch {
        val cleanId = branchId.trim()
        if (cleanId.isNotBlank()) {
            try {
                val snap = db.collection("branches").document(cleanId).get().await()
                if (snap.exists()) {
                    val parsed = snap.toObject(CafeBranch::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (parsed != null && parsed.name.isNotBlank()) {
                        return parsed
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
        return BranchDirectory.byId(cleanId.ifBlank { BranchDirectory.branch1.id })
    }

    // 0. Dynamic Multi-Branch Directory
    fun observeBranches(): Flow<List<CafeBranch>> {
        val path = "branches"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CafeBranch::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareBy<CafeBranch> { it.sortOrder }.thenBy { it.id })
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

    suspend fun seedInitialBranches(): Result<Unit> {
        val path = "branches"
        return try {
            requireUserId()
            val batch = db.batch()
            var opsCount = 0
            for (branch in BranchDirectory.defaultBranches) {
                val docRef = db.collection(path).document(branch.id)
                val snap = docRef.get().await()
                if (!snap.exists()) {
                    batch.set(
                        docRef,
                        mapOf(
                            "name" to branch.name,
                            "shortLabel" to branch.shortLabel,
                            "shortAddress" to branch.shortAddress,
                            "areas" to branch.areas,
                            "latitude" to branch.latitude,
                            "longitude" to branch.longitude,
                            "mapUrl" to branch.mapUrl,
                            "openHours" to branch.openHours,
                            "deliveryRadiusKm" to branch.deliveryRadiusKm,
                            "active" to branch.active,
                            "sortOrder" to branch.sortOrder,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    opsCount++
                } else {
                    val existingName = snap.getString("name").orEmpty()
                    val existingAddress = snap.getString("shortAddress").orEmpty()
                    val existingMapUrl = snap.getString("mapUrl").orEmpty()
                    val existingLat = snap.getDouble("latitude") ?: 0.0
                    val isLegacyStarter = existingName.contains("Gulberg", ignoreCase = true) ||
                        existingName.contains("DHA", ignoreCase = true) ||
                        existingAddress.contains("MM Alam", ignoreCase = true) ||
                        existingAddress.contains("Phase 5", ignoreCase = true)
                    if (isLegacyStarter || existingMapUrl.isBlank() || existingLat == 0.0) {
                        val updates = mutableMapOf<String, Any>(
                            "latitude" to branch.latitude,
                            "longitude" to branch.longitude,
                            "mapUrl" to branch.mapUrl,
                            "openHours" to branch.openHours,
                            "deliveryRadiusKm" to branch.deliveryRadiusKm,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                        if (isLegacyStarter) {
                            updates["name"] = branch.name
                            updates["shortLabel"] = branch.shortLabel
                            updates["shortAddress"] = branch.shortAddress
                            updates["areas"] = branch.areas
                        }
                        batch.update(docRef, updates)
                        opsCount++
                    }
                }
            }
            if (opsCount > 0) {
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveBranch(
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
        deliveryRadiusKm: Long = 15L
    ): Result<String> {
        var docPath = "branches"
        return try {
            requireUserId()
            val cleanName = name.trim().take(80)
            val cleanShortLabel = shortLabel.trim()
                .ifBlank { cleanName.substringAfter("— ").trim() }
                .take(40)
            val cleanAddress = shortAddress.trim().take(160)
            val cleanAreas = areas
                .map { it.trim().take(50) }
                .filter { it.isNotBlank() }
                .distinct()
                .take(30)
            val cleanLat = latitude.coerceIn(-90.0, 90.0)
            val cleanLng = longitude.coerceIn(-180.0, 180.0)
            val cleanMapUrl = mapUrl.trim().take(300)
            val cleanOpenHours = openHours.trim().ifBlank { "12:00 PM - 2:00 AM" }.take(60)
            val cleanRadius = deliveryRadiusKm.coerceIn(1L, 100L)

            require(cleanName.length in 2..80) { "Branch name must be 2 to 80 characters" }
            require(cleanShortLabel.length in 2..40) { "Short label must be 2 to 40 characters" }
            require(cleanAddress.length in 3..160) { "Address must be 3 to 160 characters" }

            if (branchId.isNullOrBlank()) {
                val existingDocs = db.collection("branches").get().await().documents
                val existingIds = existingDocs.map { it.id }.toSet()
                val maxNum = existingIds
                    .mapNotNull { Regex("^branch_(\\d+)$").find(it)?.groupValues?.get(1)?.toIntOrNull() }
                    .maxOrNull()
                    ?: 2
                var candidateNum = (maxNum + 1).coerceAtLeast(1)
                while (existingIds.contains("branch_$candidateNum")) {
                    candidateNum++
                }
                val newId = "branch_$candidateNum"
                val finalOrder = (sortOrder ?: candidateNum.toLong()).coerceIn(0L, 10000L)
                val docRef = db.collection("branches").document(newId)
                docPath = docRef.path
                docRef.set(
                    mapOf(
                        "name" to cleanName,
                        "shortLabel" to cleanShortLabel,
                        "shortAddress" to cleanAddress,
                        "areas" to cleanAreas,
                        "latitude" to cleanLat,
                        "longitude" to cleanLng,
                        "mapUrl" to cleanMapUrl,
                        "openHours" to cleanOpenHours,
                        "deliveryRadiusKm" to cleanRadius,
                        "active" to active,
                        "sortOrder" to finalOrder,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(newId)
            } else {
                val cleanId = branchId.trim()
                require(Regex("^[a-zA-Z0-9_\\-]{1,64}$").matches(cleanId)) { "Invalid branch ID" }
                val docRef = db.collection("branches").document(cleanId)
                docPath = docRef.path
                val existingSnap = docRef.get().await()
                val currentSortOrder = existingSnap.getLong("sortOrder") ?: 1L
                val finalOrder = (sortOrder ?: currentSortOrder).coerceIn(0L, 10000L)
                if (existingSnap.exists()) {
                    docRef.update(
                        mapOf(
                            "name" to cleanName,
                            "shortLabel" to cleanShortLabel,
                            "shortAddress" to cleanAddress,
                            "areas" to cleanAreas,
                            "latitude" to cleanLat,
                            "longitude" to cleanLng,
                            "mapUrl" to cleanMapUrl,
                            "openHours" to cleanOpenHours,
                            "deliveryRadiusKm" to cleanRadius,
                            "active" to active,
                            "sortOrder" to finalOrder,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else {
                    docRef.set(
                        mapOf(
                            "name" to cleanName,
                            "shortLabel" to cleanShortLabel,
                            "shortAddress" to cleanAddress,
                            "areas" to cleanAreas,
                            "latitude" to cleanLat,
                            "longitude" to cleanLng,
                            "mapUrl" to cleanMapUrl,
                            "openHours" to cleanOpenHours,
                            "deliveryRadiusKm" to cleanRadius,
                            "active" to active,
                            "sortOrder" to finalOrder,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }

                // Keep assigned staff docs synchronized with updated branch display name
                try {
                    val staffDocs = db.collection("branch_admins")
                        .whereEqualTo("branchId", cleanId)
                        .get()
                        .await()
                        .documents
                    for (staffDoc in staffDocs) {
                        staffDoc.reference.update(
                            mapOf(
                                "branchName" to cleanName,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                }

                Result.success(cleanId)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteBranch(branchId: String): Result<Unit> {
        val cleanId = branchId.trim()
        val docPath = "branches/$cleanId"
        return try {
            requireUserId()
            require(cleanId.isNotBlank()) { "Branch ID is required" }
            db.collection("branches").document(cleanId).delete().await()
            try {
                val staffDocs = db.collection("branch_admins")
                    .whereEqualTo("branchId", cleanId)
                    .get()
                    .await()
                    .documents
                staffDocs.forEach { it.reference.delete().await() }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
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
            val existingSnap = db.collection(path).get().await()
            val existingIds = existingSnap.documents.map { it.id }.toSet()
            val hasNewCatalog = existingIds.any { it.startsWith("lfc_") }

            val legacyToDelete = if (!hasNewCatalog) {
                existingIds.intersect(SeedMenuData.legacyStarterIds).toList()
            } else {
                emptyList()
            }
            val missingSeedItems = SeedMenuData.initialItems.filter { item ->
                val docId = item.id.removePrefix("seed_")
                !existingIds.contains(docId)
            }

            if (legacyToDelete.isEmpty() && missingSeedItems.isEmpty()) {
                return Result.success(Unit)
            }

            var batch = db.batch()
            var opsInBatch = 0
            suspend fun flushIfNeeded(force: Boolean = false) {
                if ((opsInBatch >= 400) || (force && opsInBatch > 0)) {
                    batch.commit().await()
                    batch = db.batch()
                    opsInBatch = 0
                }
            }

            for (legacyId in legacyToDelete) {
                batch.delete(db.collection(path).document(legacyId))
                opsInBatch++
                flushIfNeeded()
            }

            for (item in missingSeedItems) {
                val docId = item.id.removePrefix("seed_")
                val docRef = db.collection(path).document(docId)
                val payload = mapOf(
                    "name" to item.name.trim().take(80),
                    "description" to item.description.trim().take(300),
                    "category" to item.category.trim().take(40),
                    "pricePkr" to item.pricePkr,
                    "imageKey" to item.imageKey.trim().take(50),
                    "badgeText" to item.badgeText.trim().take(30),
                    "available" to item.available,
                    "sortOrder" to item.sortOrder.coerceIn(0L, 10000L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(docRef, payload)
                opsInBatch++
                flushIfNeeded()
            }
            flushIfNeeded(force = true)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun allocateMenuItemId(): String = db.collection("menu_items").document().id

    suspend fun saveMenuItem(
        itemId: String?,
        name: String,
        description: String,
        category: String,
        pricePkr: Long,
        imageKey: String,
        imageUrl: String = "",
        badgeText: String,
        available: Boolean,
        sortOrder: Long,
        preAllocatedId: String? = null
    ): Result<Unit> {
        var docPath = "menu_items"
        return try {
            requireUserId()
            val trimmedName = name.trim()
            val trimmedDesc = description.trim()
            val trimmedCategory = category.trim()
            val trimmedImageUrl = imageUrl.trim().take(MenuImageStorageHelper.MAX_STORED_IMAGE_CHARS)
            require(trimmedName.length in 1..80) { "Name must be 1 to 80 characters" }
            require(trimmedDesc.length <= 300) { "Description must be 300 characters or less" }
            require(trimmedCategory.length in 1..40) { "Category must be 1 to 40 characters" }
            require(pricePkr in 1..100000) { "Price must be between 1 and 100,000 PKR" }
            require(MenuImageStorageHelper.isValidStoredImageRef(trimmedImageUrl)) {
                "Invalid photo format. Please upload a photo from your gallery."
            }

            val existingDocId = itemId?.trim()?.takeIf { it.isNotBlank() }
            if (existingDocId == null) {
                val docRef = if (!preAllocatedId.isNullOrBlank()) {
                    db.collection("menu_items").document(preAllocatedId.trim())
                } else {
                    db.collection("menu_items").document()
                }
                docPath = docRef.path
                val payload = mapOf(
                    "name" to trimmedName,
                    "description" to trimmedDesc,
                    "category" to trimmedCategory,
                    "pricePkr" to pricePkr,
                    "imageKey" to imageKey.ifBlank { "zinger_burger" },
                    "imageUrl" to trimmedImageUrl,
                    "badgeText" to badgeText.trim().take(30),
                    "available" to available,
                    "sortOrder" to sortOrder.coerceIn(0L, 10000L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val docRef = db.collection("menu_items").document(existingDocId)
                docPath = docRef.path
                val existingSnap = docRef.get().await()
                if (existingSnap.exists()) {
                    val updates = mapOf(
                        "name" to trimmedName,
                        "description" to trimmedDesc,
                        "category" to trimmedCategory,
                        "pricePkr" to pricePkr,
                        "imageKey" to imageKey.ifBlank { "zinger_burger" },
                        "imageUrl" to trimmedImageUrl,
                        "badgeText" to badgeText.trim().take(30),
                        "available" to available,
                        "sortOrder" to sortOrder.coerceIn(0L, 10000L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.update(updates).await()
                } else {
                    val payload = mapOf(
                        "name" to trimmedName,
                        "description" to trimmedDesc,
                        "category" to trimmedCategory,
                        "pricePkr" to pricePkr,
                        "imageKey" to imageKey.ifBlank { "zinger_burger" },
                        "imageUrl" to trimmedImageUrl,
                        "badgeText" to badgeText.trim().take(30),
                        "available" to available,
                        "sortOrder" to sortOrder.coerceIn(0L, 10000L),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(payload).await()
                }
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
            val normalizedEmail = (user.email ?: "").trim().lowercase().take(120)
            val normalizedName = (user.displayName ?: "Lahori Guest").trim().ifBlank { "Lahori Guest" }.take(80)
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val isSuper = normalizedEmail.equals(SUPER_ADMIN_EMAIL, ignoreCase = true) && user.isEmailVerified
                val role = if (isSuper) UserRole.SuperAdmin.name else UserRole.Customer.name
                val payload = mapOf(
                    "userId" to uid,
                    "email" to normalizedEmail,
                    "displayName" to normalizedName,
                    "role" to role,
                    "assignedBranchId" to "",
                    "favoriteItemIds" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val storedEmail = snap.getString("email").orEmpty()
                val storedName = snap.getString("displayName").orEmpty()
                if ((normalizedEmail.isNotBlank() && storedEmail != normalizedEmail) || storedName.isBlank()) {
                    docRef.update(
                        mapOf(
                            "email" to normalizedEmail,
                            "displayName" to storedName.ifBlank { normalizedName },
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
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

    // 2b. Firebase Cloud Messaging Token Registration
    suspend fun registerFcmToken(token: String): Result<Unit> {
        val cleanToken = token.trim().take(512)
        if (cleanToken.length < 10) {
            return Result.failure(IllegalArgumentException("Invalid FCM registration token"))
        }
        val tokenId = sha256Hex(cleanToken)
        var docPath = "users/unauthenticated/fcm_tokens/$tokenId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("fcm_tokens").document(tokenId)
            docPath = docRef.path
            val appVersion = BuildConfig.VERSION_NAME.ifBlank { "1.0" }.take(30)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "token" to cleanToken,
                        "appVersion" to appVersion,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "token" to cleanToken,
                        "platform" to "android",
                        "appVersion" to appVersion,
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

    suspend fun unregisterFcmToken(token: String): Result<Unit> {
        val cleanToken = token.trim().take(512)
        if (cleanToken.length < 10) return Result.success(Unit)
        val tokenId = sha256Hex(cleanToken)
        var docPath = "users/unauthenticated/fcm_tokens/$tokenId"
        return try {
            val uid = auth.currentUser?.uid ?: return Result.success(Unit)
            val docRef = db.collection("users").document(uid).collection("fcm_tokens").document(tokenId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
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

    suspend fun addSavedAddress(
        userId: String,
        label: String,
        streetAddress: String,
        area: String,
        knownBranches: List<CafeBranch> = emptyList()
    ): Result<SavedAddress> {
        val path = "users/$userId/addresses"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val cleanLabel = label.trim().ifBlank { "Home" }.take(40)
            val cleanStreet = streetAddress.trim().take(200)
            val cleanArea = area.trim().take(60)
            require(cleanStreet.length >= 3) { "Please enter a street address (min 3 characters)" }
            require(cleanArea.isNotBlank()) { "Please enter a neighborhood/area" }

            val activeBranches = knownBranches.ifEmpty {
                try {
                    db.collection("branches").get().await()
                        .toObjects(CafeBranch::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    emptyList()
                }
            }.ifEmpty { BranchDirectory.defaultBranches }

            val routedBranch = BranchDirectory.routeAddressToBranch("$cleanArea $cleanStreet", activeBranches)
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

    suspend fun addBranchAdmin(
        email: String,
        branchId: String,
        role: String = UserRole.BranchAdmin.name
    ): Result<Unit> {
        var docPath = "branch_admins"
        return try {
            requireUserId()
            val cleanEmail = email.trim().lowercase()
            require(cleanEmail.contains("@") && cleanEmail.contains(".") && cleanEmail.length in 5..120) {
                "Enter a valid staff email address"
            }
            require(!cleanEmail.equals(SUPER_ADMIN_EMAIL, ignoreCase = true)) {
                "SuperAdmin email cannot be downgraded to BranchAdmin"
            }
            val normalizedRole = UserRole.BranchAdmin.name
            val branch = resolveBranch(branchId)

            // Document ID matches lowercase email so Firestore rules resolve
            // /branch_admins/{lowercaseEmail} in O(1) with instant revocation.
            val docId = cleanEmail
            val docRef = db.collection("branch_admins").document(docId)
            docPath = docRef.path

            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "email" to cleanEmail,
                        "role" to normalizedRole,
                        "branchId" to branch.id,
                        "branchName" to branch.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "email" to cleanEmail,
                        "role" to normalizedRole,
                        "branchId" to branch.id,
                        "branchName" to branch.name,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Clean up any legacy hashed/random-ID docs for the same email
            try {
                val duplicates = db.collection("branch_admins")
                    .whereEqualTo("email", cleanEmail)
                    .get()
                    .await()
                    .documents
                    .filter { it.id != docId }
                duplicates.forEach { it.reference.delete().await() }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }

            // Sync role onto user's /users/{uid} document if they have already signed in before
            try {
                val matchedUsers = db.collection("users")
                    .whereEqualTo("email", cleanEmail)
                    .limit(1)
                    .get()
                    .await()
                matchedUsers.documents.firstOrNull()?.reference?.update(
                    mapOf(
                        "role" to normalizedRole,
                        "assignedBranchId" to branch.id,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )?.await()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun removeBranchAdmin(adminId: String): Result<Unit> {
        val path = "branch_admins/$adminId"
        return try {
            requireUserId()
            val docRef = db.collection("branch_admins").document(adminId)
            val snap = docRef.get().await()
            val removedEmail = snap.getString("email")?.trim()?.lowercase().orEmpty()
                .ifBlank { if (adminId.contains("@")) adminId.trim().lowercase() else "" }
            docRef.delete().await()

            if (removedEmail.isNotBlank()) {
                try {
                    val matchedUsers = db.collection("users")
                        .whereEqualTo("email", removedEmail)
                        .limit(1)
                        .get()
                        .await()
                    matchedUsers.documents.firstOrNull()?.reference?.update(
                        mapOf(
                            "role" to UserRole.Customer.name,
                            "assignedBranchId" to "",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )?.await()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // 5. Orders (Customer, BranchAdmin, SuperAdmin)
    fun observeOrder(orderId: String): Flow<CafeOrder?> = flow {
        requireUserId()
        require(orderId.isNotBlank()) { "Order ID is required" }
        val path = "orders/$orderId"
        val snapshotFlow = db.collection("orders").document(orderId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(CafeOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, path)
                }
                throw error
            }
        snapshotFlow.collect { emit(it) }
    }

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

    fun observeBranchOrders(branchId: String): Flow<List<CafeOrder>> {
        val path = "orders"
        return db.collection(path)
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
            val branch = resolveBranch(branchId)

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
                "customerEmail" to (user.email ?: "").trim().lowercase().take(120),
                "customerPhone" to cleanPhone,
                "orderType" to orderType.name,
                "deliveryAddress" to cleanAddress,
                "branchId" to branch.id,
                "branchName" to branch.name,
                "branchAdminEmails" to emptyList<String>(),
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

    fun observeBranchPromotions(branchId: String): Flow<List<Promotion>> {
        val path = "promotions"
        return db.collection(path)
            .whereEqualTo("branchId", branchId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Promotion::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
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

    suspend fun savePromotion(
        code: String,
        discountType: String,
        discountValue: Long,
        minOrderPkr: Long,
        active: Boolean,
        branchId: String = "",
        branchName: String = "",
        expiresAt: Timestamp? = null,
        promoDocId: String? = null
    ): Result<Unit> {
        val cleanCode = code.trim().uppercase()
        val cleanBranchId = branchId.trim()
        val cleanBranchName = branchName.trim().take(80)
        val sanitizedCode = cleanCode.lowercase().replace(Regex("[^a-z0-9_\\-]"), "_")
        val docId = promoDocId?.trim()?.takeIf { it.isNotBlank() } ?: if (cleanBranchId.isNotBlank()) {
            "${cleanBranchId}_${sanitizedCode}".take(100)
        } else {
            sanitizedCode
        }
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
                val updates = mutableMapOf<String, Any?>(
                    "code" to cleanCode,
                    "discountType" to discountType,
                    "discountValue" to discountValue,
                    "minOrderPkr" to minOrderPkr.coerceAtLeast(0L),
                    "active" to active,
                    "branchName" to cleanBranchName,
                    "expiresAt" to expiresAt,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                val payload = mutableMapOf<String, Any?>(
                    "code" to cleanCode,
                    "discountType" to discountType,
                    "discountValue" to discountValue,
                    "minOrderPkr" to minOrderPkr.coerceAtLeast(0L),
                    "active" to active,
                    "branchId" to cleanBranchId,
                    "branchName" to cleanBranchName,
                    "expiresAt" to expiresAt,
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

    suspend fun validatePromoCode(
        rawCode: String,
        subtotalPkr: Long,
        selectedBranchId: String = ""
    ): Result<Pair<Promotion, Long>> {
        val cleanCode = rawCode.trim().uppercase()
        val path = "promotions"
        return try {
            requireUserId()
            require(cleanCode.isNotBlank()) { "Please enter a promo code" }

            val querySnap = db.collection(path)
                .whereEqualTo("code", cleanCode)
                .limit(20)
                .get()
                .await()

            val candidates: List<Promotion> = if (!querySnap.isEmpty) {
                querySnap.documents.mapNotNull { it.toObject(Promotion::class.java) }
            } else {
                val anyPromo = db.collection(path).limit(1).get().await()
                if (anyPromo.isEmpty) {
                    SeedMenuData.defaultPromotions.filter { it.code.equals(cleanCode, ignoreCase = true) }
                } else {
                    emptyList()
                }
            }

            if (candidates.isEmpty()) {
                return Result.failure(IllegalArgumentException("Promo code '$cleanCode' was not found."))
            }

            // Prefer exact branch match first, then global promo
            val branchMatch = candidates.firstOrNull {
                it.branchId.isNotBlank() && it.branchId == selectedBranchId
            }
            val globalMatch = candidates.firstOrNull { it.isGlobal() }
            val promo = branchMatch ?: globalMatch

            if (promo == null) {
                val otherBranchName = candidates.firstOrNull()?.branchName?.ifBlank { "another branch" } ?: "another branch"
                return Result.failure(
                    IllegalArgumentException("Promo code '$cleanCode' is only valid for $otherBranchName.")
                )
            }

            if (!promo.active) {
                return Result.failure(IllegalArgumentException("Promo code '$cleanCode' is currently paused."))
            }
            if (promo.isExpired()) {
                return Result.failure(IllegalArgumentException("Promo code '$cleanCode' has expired."))
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

    // 6b. Branch-Exclusive Seasonal Banners (Firestore 'branch_banners' collection)
    fun allocateBranchBannerId(): String = db.collection("branch_banners").document().id

    fun observeAllBranchBanners(): Flow<List<BranchBanner>> {
        val path = "branch_banners"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(BranchBanner::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
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

    fun observeBranchBanners(branchId: String): Flow<List<BranchBanner>> {
        val path = "branch_banners"
        return db.collection(path)
            .whereEqualTo("branchId", branchId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(BranchBanner::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
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

    suspend fun saveBranchBanner(
        bannerId: String?,
        branchId: String,
        branchName: String,
        title: String,
        subtitle: String,
        promoCode: String,
        badgeText: String,
        imageUrl: String,
        targetCategory: String,
        active: Boolean,
        expiresAt: Timestamp? = null,
        preAllocatedId: String? = null
    ): Result<Unit> {
        var docPath = "branch_banners"
        return try {
            requireUserId()
            val cleanBranchId = branchId.trim()
            val cleanBranchName = branchName.trim().take(80)
            val cleanTitle = title.trim().take(80)
            val cleanSubtitle = subtitle.trim().take(200)
            val cleanPromoCode = promoCode.trim().uppercase().take(30)
            val cleanBadge = badgeText.trim().ifBlank { "SEASONAL OFFER" }.take(30)
            val cleanImageUrl = imageUrl.trim().take(MenuImageStorageHelper.MAX_STORED_IMAGE_CHARS)
            val cleanCategory = targetCategory.trim().ifBlank { "DEALS" }.take(40)

            require(cleanBranchId.isNotBlank()) { "Branch ID is required" }
            require(cleanTitle.length in 2..80) { "Banner headline must be 2 to 80 characters" }
            require(cleanImageUrl.length >= 8 && MenuImageStorageHelper.isValidStoredImageRef(cleanImageUrl)) {
                "Please upload a banner photo from your gallery."
            }

            if (bannerId.isNullOrBlank()) {
                val docRef = if (!preAllocatedId.isNullOrBlank()) {
                    db.collection("branch_banners").document(preAllocatedId.trim())
                } else {
                    db.collection("branch_banners").document()
                }
                docPath = docRef.path
                val payload = mutableMapOf<String, Any?>(
                    "branchId" to cleanBranchId,
                    "branchName" to cleanBranchName,
                    "title" to cleanTitle,
                    "subtitle" to cleanSubtitle,
                    "promoCode" to cleanPromoCode,
                    "badgeText" to cleanBadge,
                    "imageUrl" to cleanImageUrl,
                    "targetCategory" to cleanCategory,
                    "active" to active,
                    "expiresAt" to expiresAt,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val docRef = db.collection("branch_banners").document(bannerId)
                docPath = docRef.path
                val updates = mutableMapOf<String, Any?>(
                    "branchName" to cleanBranchName,
                    "title" to cleanTitle,
                    "subtitle" to cleanSubtitle,
                    "promoCode" to cleanPromoCode,
                    "badgeText" to cleanBadge,
                    "imageUrl" to cleanImageUrl,
                    "targetCategory" to cleanCategory,
                    "active" to active,
                    "expiresAt" to expiresAt,
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

    suspend fun deleteBranchBanner(bannerId: String): Result<Unit> {
        val docPath = "branch_banners/$bannerId"
        return try {
            requireUserId()
            require(bannerId.isNotBlank()) { "Banner ID required" }
            db.collection("branch_banners").document(bannerId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
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
            require(OrderStatus.fromString(order.status) == OrderStatus.Delivered) {
                "Only delivered orders can be reviewed"
            }
            require(rating in 1..5) { "Rating must be between 1 and 5 stars" }

            val cleanComment = comment.trim().take(400)
            val cleanCustomerName = order.customerName.trim()
                .ifBlank { user.displayName?.trim().orEmpty() }
                .ifBlank { "Lahori Customer" }
                .take(80)
            val cleanItemsSummary = order.itemsSummary.trim()
                .ifBlank { "Lahori Food Cafe Order" }
                .take(300)
            val branch = resolveBranch(order.branchId)
            val cleanBranchName = order.branchName.trim()
                .ifBlank { branch.name }
                .ifBlank { "Lahori Food Cafe" }
                .take(80)

            val docRef = db.collection("reviews").document(order.id)
            val existing = try {
                docRef.get().await()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            if (existing != null && existing.exists()) {
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
                        "branchName" to cleanBranchName,
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

    suspend fun deleteReview(reviewId: String): Result<Unit> {
        val docPath = "reviews/$reviewId"
        return try {
            requireUserId()
            require(reviewId.isNotBlank()) { "Review ID required" }
            db.collection("reviews").document(reviewId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // 8. General Account Settings Food & Service Feedback (Firestore 'feedback' collection)
    fun observeCustomerFeedback(userId: String): Flow<List<CafeFeedback>> {
        val path = "feedback"
        return db.collection(path)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CafeFeedback::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
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

    fun observeAllFeedback(): Flow<List<CafeFeedback>> {
        val path = "feedback"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CafeFeedback::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
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

    suspend fun submitCafeFeedback(
        branchId: String,
        foodRating: Int,
        serviceRating: Int,
        comment: String
    ): Result<String> {
        var docPath = "feedback"
        return try {
            val uid = requireUserId()
            val user = auth.currentUser!!
            require(foodRating in 1..5) { "Food rating must be between 1 and 5 stars" }
            require(serviceRating in 1..5) { "Service rating must be between 1 and 5 stars" }

            val cleanComment = comment.trim().take(500)
            val cleanName = (user.displayName ?: "Lahori Customer")
                .trim()
                .ifBlank { "Lahori Customer" }
                .take(80)
            val cleanEmail = (user.email ?: "").trim().lowercase().take(120)
            val branch = resolveBranch(branchId)

            val docRef = db.collection("feedback").document()
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "customerName" to cleanName,
                    "customerEmail" to cleanEmail,
                    "branchId" to branch.id,
                    "branchName" to branch.name,
                    "foodRating" to foodRating.toLong(),
                    "serviceRating" to serviceRating.toLong(),
                    "comment" to cleanComment,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteFeedback(feedbackId: String): Result<Unit> {
        val docPath = "feedback/$feedbackId"
        return try {
            requireUserId()
            require(feedbackId.isNotBlank()) { "Feedback ID required" }
            db.collection("feedback").document(feedbackId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
