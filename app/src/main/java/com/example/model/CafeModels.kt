package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

const val SUPER_ADMIN_EMAIL = "uk9798989@gmail.com"

enum class UserRole {
    Customer,
    BranchAdmin,
    SuperAdmin;

    companion object {
        fun fromString(raw: String?): UserRole =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: Customer
    }
}

enum class OrderType(val label: String) {
    DELIVERY("DELIVERY"),
    PICKUP("PICKUP")
}

enum class OrderStatus(
    val displayTitle: String,
    val notificationTitle: String,
    val notificationMessage: String
) {
    Confirmed(
        displayTitle = "Order Confirmed",
        notificationTitle = "Order Confirmed",
        notificationMessage = "Your Lahori Food Cafe order is confirmed and sent to the kitchen."
    ),
    Preparing(
        displayTitle = "Preparing",
        notificationTitle = "Preparing Your Order",
        notificationMessage = "Our chefs are firing up your crispy Lahori favorites!"
    ),
    Dispatched(
        displayTitle = "Out for Delivery",
        notificationTitle = "Out for Delivery",
        notificationMessage = "Your rider is on the way with your hot order!"
    ),
    Delivered(
        displayTitle = "Delivered",
        notificationTitle = "Delivered",
        notificationMessage = "Order delivered! Enjoy your Lahori Food Cafe feast."
    );

    fun notificationTitleFor(orderType: String): String {
        val isPickup = orderType.equals(OrderType.PICKUP.name, ignoreCase = true)
        return when (this) {
            Dispatched -> if (isPickup) "Ready for Pickup" else notificationTitle
            Delivered -> if (isPickup) "Picked Up" else notificationTitle
            else -> notificationTitle
        }
    }

    fun notificationMessageFor(orderType: String): String {
        val isPickup = orderType.equals(OrderType.PICKUP.name, ignoreCase = true)
        return when (this) {
            Dispatched -> if (isPickup) {
                "Your Lahori Food Cafe order is freshly packed and ready for pickup at the counter!"
            } else {
                notificationMessage
            }
            Delivered -> if (isPickup) {
                "Order picked up! Enjoy your Lahori Food Cafe feast."
            } else {
                notificationMessage
            }
            else -> notificationMessage
        }
    }

    fun nextStatus(): OrderStatus? = when (this) {
        Confirmed -> Preparing
        Preparing -> Dispatched
        Dispatched -> Delivered
        Delivered -> null
    }

    companion object {
        fun fromString(raw: String?): OrderStatus {
            val normalized = raw?.trim().orEmpty()
            if (normalized.equals("Ready for Pickup", ignoreCase = true) ||
                normalized.equals("READY_FOR_PICKUP", ignoreCase = true)
            ) {
                return Dispatched
            }
            if (normalized.equals("Picked Up", ignoreCase = true)) {
                return Delivered
            }
            return entries.firstOrNull { it.name.equals(normalized, ignoreCase = true) } ?: Confirmed
        }
    }
}

@Keep
@IgnoreExtraProperties
data class CafeBranch(
    @DocumentId val id: String = "",
    val name: String = "",
    val shortLabel: String = "",
    val shortAddress: String = "",
    val areas: List<String> = emptyList(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val mapUrl: String = "",
    val openHours: String = "12:00 PM - 2:00 AM",
    val deliveryRadiusKm: Long = 15L,
    val active: Boolean = true,
    val sortOrder: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun displayShortLabel(): String =
        shortLabel.ifBlank { name.substringAfter("— ").trim().ifBlank { id } }

    fun hasCoordinates(): Boolean = latitude != 0.0 && longitude != 0.0
}

typealias BranchInfo = CafeBranch

object BranchDirectory {
    val branch1 = CafeBranch(
        id = "branch_1",
        name = "Lahori Food Cafe — Taranda Muhammad Panah",
        shortLabel = "Lahori Food Cafe TMP",
        shortAddress = "Main KLP Road, Taranda Muhammad Panah (TMP), Rahim Yar Khan",
        areas = listOf(
            "Taranda Muhammad Panah",
            "TMP",
            "Main KLP Road",
            "Basti",
            "Allahabad Road",
            "Feroza",
            "Liaquatpur",
            "Shafiabad"
        ),
        latitude = 29.1003,
        longitude = 70.9080,
        mapUrl = "https://share.google/P92yinaSqd7Dz0anJ",
        openHours = "12:00 PM - 2:00 AM",
        deliveryRadiusKm = 15L,
        active = true,
        sortOrder = 1L
    )

    val branch2 = CafeBranch(
        id = "branch_2",
        name = "Lahori Food Cafe — Zahir Pir",
        shortLabel = "Lahori Food Cafe Zahir Pir",
        shortAddress = "Main KLP Road / M-5 Interchange, Zahir Pir, Khanpur",
        areas = listOf(
            "Zahir Pir",
            "Zahirpir",
            "M-5 Interchange",
            "Khanpur",
            "Chachran Sharif",
            "Nawan Kot",
            "Sardarpur",
            "Jajja Abbasian"
        ),
        latitude = 28.8124,
        longitude = 70.5241,
        mapUrl = "https://share.google/XrwRlyd2jYQs8wbzl",
        openHours = "12:00 PM - 2:00 AM",
        deliveryRadiusKm = 15L,
        active = true,
        sortOrder = 2L
    )

    val defaultBranches = listOf(branch1, branch2)
    val allBranches = defaultBranches

    fun byId(branchId: String, branches: List<CafeBranch> = defaultBranches): CafeBranch =
        branches.firstOrNull { it.id == branchId }
            ?: defaultBranches.firstOrNull { it.id == branchId }
            ?: branches.firstOrNull()
            ?: branch1

    fun shortPickerLabel(branchId: String, branches: List<CafeBranch> = defaultBranches): String {
        val found = branches.firstOrNull { it.id == branchId }
            ?: defaultBranches.firstOrNull { it.id == branchId }
        return found?.displayShortLabel() ?: branchId
    }

    fun haversineDistanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0)
        val c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a))
        return earthRadiusKm * c
    }

    fun distanceToKm(branch: CafeBranch, lat: Double, lng: Double): Double? {
        if (!branch.hasCoordinates()) return null
        return haversineDistanceKm(lat, lng, branch.latitude, branch.longitude)
    }

    fun branchesSortedByDistance(
        lat: Double,
        lng: Double,
        branches: List<CafeBranch> = defaultBranches
    ): List<Pair<CafeBranch, Double?>> {
        val pool = branches.filter { it.active }.ifEmpty {
            branches.ifEmpty { defaultBranches }
        }
        return pool.map { branch ->
            branch to distanceToKm(branch, lat, lng)
        }.sortedWith(
            compareBy<Pair<CafeBranch, Double?>> { it.second ?: Double.MAX_VALUE }
                .thenBy { it.first.sortOrder }
        )
    }

    fun nearestBranch(
        lat: Double,
        lng: Double,
        branches: List<CafeBranch> = defaultBranches
    ): CafeBranch =
        branchesSortedByDistance(lat, lng, branches).firstOrNull()?.first ?: branch1

    fun routeAddressToBranch(
        areaOrAddress: String,
        branches: List<CafeBranch> = defaultBranches
    ): CafeBranch {
        val pool = branches.filter { it.active }.ifEmpty {
            branches.ifEmpty { defaultBranches }
        }.sortedBy { it.sortOrder }

        val normalized = areaOrAddress.lowercase().trim()
        if (normalized.isNotBlank()) {
            // Find the branch whose longest matching area keyword matches the query
            var bestBranch: CafeBranch? = null
            var bestScore = 0
            for (branch in pool) {
                for (area in branch.areas) {
                    val cleanArea = area.trim().lowercase()
                    if (cleanArea.isNotEmpty() && normalized.contains(cleanArea) && cleanArea.length > bestScore) {
                        bestScore = cleanArea.length
                        bestBranch = branch
                    }
                }
            }
            if (bestBranch != null) return bestBranch

            // Secondary match against branch shortLabel, name, or shortAddress
            val metaMatch = pool.firstOrNull { branch ->
                branch.shortLabel.lowercase().let { it.isNotBlank() && normalized.contains(it) } ||
                    branch.name.substringAfter("— ").lowercase().let { it.isNotBlank() && normalized.contains(it) } ||
                    branch.shortAddress.lowercase().let { it.isNotBlank() && normalized.contains(it) }
            }
            if (metaMatch != null) return metaMatch
        }

        // Deterministic fallback for Zahir Pir corridor keywords
        val fallbackBranch2 = pool.firstOrNull { it.id == branch2.id }
        if (fallbackBranch2 != null && (
                normalized.contains("zahir") ||
                    normalized.contains("khanpur") ||
                    normalized.contains("m-5") ||
                    normalized.contains("chachran")
                )
        ) {
            return fallbackBranch2
        }
        return pool.firstOrNull() ?: branch1
    }
}

@Keep
@IgnoreExtraProperties
data class MenuItem(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "EVERYDAY VALUE",
    val pricePkr: Long = 0L,
    val imageKey: String = "zinger_burger",
    val imageUrl: String = "",
    val badgeText: String = "",
    val available: Boolean = true,
    val sortOrder: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = UserRole.Customer.name,
    val assignedBranchId: String = "",
    val favoriteItemIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SavedAddress(
    @DocumentId val id: String = "",
    val label: String = "",
    val streetAddress: String = "",
    val area: String = "",
    val branchId: String = BranchDirectory.branch1.id,
    val branchName: String = BranchDirectory.branch1.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BranchAdminEntry(
    @DocumentId val id: String = "",
    val email: String = "",
    val role: String = UserRole.BranchAdmin.name,
    val branchId: String = BranchDirectory.branch1.id,
    val branchName: String = BranchDirectory.branch1.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class OrderLineItem(
    val itemId: String = "",
    val name: String = "",
    val quantity: Long = 1L,
    val unitPricePkr: Long = 0L,
    val lineTotalPkr: Long = 0L
) {
    fun toMap(): Map<String, Any> = mapOf(
        "itemId" to itemId,
        "name" to name,
        "quantity" to quantity,
        "unitPricePkr" to unitPricePkr,
        "lineTotalPkr" to lineTotalPkr
    )
}

@Keep
@IgnoreExtraProperties
data class CafeOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val guestSessionId: String = "",
    val guestCheckout: Boolean = false,
    val customerName: String = "",
    val customerEmail: String = "",
    val customerPhone: String = "",
    val orderType: String = OrderType.DELIVERY.name,
    val deliveryAddress: String = "",
    val deliveryLatitude: Double = 0.0,
    val deliveryLongitude: Double = 0.0,
    val branchId: String = BranchDirectory.branch1.id,
    val branchName: String = BranchDirectory.branch1.name,
    val branchAdminEmails: List<String> = emptyList(),
    val items: List<OrderLineItem> = emptyList(),
    val itemsSummary: String = "",
    val promoCode: String = "",
    val discountPkr: Long = 0L,
    val subtotalPkr: Long = 0L,
    val deliveryFeePkr: Long = 0L,
    val totalPkr: Long = 0L,
    val status: String = OrderStatus.Confirmed.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun hasDeliveryLocation(): Boolean =
        (deliveryLatitude != 0.0 || deliveryLongitude != 0.0) &&
            deliveryLatitude in -90.0..90.0 &&
            deliveryLongitude in -180.0..180.0

    fun formattedCoordinates(): String =
        if (hasDeliveryLocation()) {
            String.format(java.util.Locale.US, "%.5f, %.5f", deliveryLatitude, deliveryLongitude)
        } else {
            ""
        }

    fun deliveryGoogleMapsUrl(): String =
        if (hasDeliveryLocation()) {
            "https://www.google.com/maps/search/?api=1&query=$deliveryLatitude,$deliveryLongitude"
        } else if (deliveryAddress.isNotBlank()) {
            "https://www.google.com/maps/search/?api=1&query=${android.net.Uri.encode(deliveryAddress)}"
        } else {
            ""
        }
}

@Keep
@IgnoreExtraProperties
data class Promotion(
    @DocumentId val id: String = "",
    val code: String = "",
    val discountType: String = "FLAT",
    val discountValue: Long = 0L,
    val minOrderPkr: Long = 0L,
    val active: Boolean = true,
    val branchId: String = "",
    val branchName: String = "",
    val expiresAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun isGlobal(): Boolean = branchId.isBlank()

    fun appliesToBranch(selectedBranchId: String): Boolean =
        branchId.isBlank() || branchId == selectedBranchId

    fun isExpired(nowMillis: Long = System.currentTimeMillis()): Boolean =
        expiresAt != null && expiresAt.toDate().time <= nowMillis

    fun isCurrentlyValidForBranch(
        selectedBranchId: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean = active && appliesToBranch(selectedBranchId) && !isExpired(nowMillis)

    fun computeDiscount(
        subtotalPkr: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        if (!active || isExpired(nowMillis) || subtotalPkr < minOrderPkr || subtotalPkr <= 0L) return 0L
        return if (discountType.equals("PERCENT", ignoreCase = true)) {
            ((subtotalPkr * discountValue.coerceIn(0L, 90L)) / 100L).coerceIn(0L, subtotalPkr)
        } else {
            discountValue.coerceIn(0L, subtotalPkr)
        }
    }
}

@Keep
@IgnoreExtraProperties
data class BranchBanner(
    @DocumentId val id: String = "",
    val branchId: String = "",
    val branchName: String = "",
    val title: String = "",
    val subtitle: String = "",
    val promoCode: String = "",
    val badgeText: String = "SEASONAL OFFER",
    val imageUrl: String = "",
    val targetCategory: String = "DEALS",
    val active: Boolean = true,
    val expiresAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun isExpired(nowMillis: Long = System.currentTimeMillis()): Boolean =
        expiresAt != null && expiresAt.toDate().time <= nowMillis

    fun isVisibleForBranch(
        selectedBranchId: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean = active && branchId.isNotBlank() && branchId == selectedBranchId && !isExpired(nowMillis)
}

@Keep
@IgnoreExtraProperties
data class OrderReview(
    @DocumentId val id: String = "",
    val orderId: String = "",
    val userId: String = "",
    val customerName: String = "",
    val branchId: String = BranchDirectory.branch1.id,
    val branchName: String = BranchDirectory.branch1.name,
    val rating: Long = 5L,
    val comment: String = "",
    val itemsSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CafeFeedback(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerEmail: String = "",
    val branchId: String = BranchDirectory.branch1.id,
    val branchName: String = BranchDirectory.branch1.name,
    val foodRating: Long = 5L,
    val serviceRating: Long = 5L,
    val comment: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class CartItem(
    val menuItem: MenuItem,
    val quantity: Int
) {
    val lineTotalPkr: Long get() = menuItem.pricePkr * quantity
}

data class PromoBanner(
    val id: String,
    val eyebrow: String,
    val title: String,
    val subtitle: String,
    val priceTag: String,
    val targetCategory: String,
    val imageKey: String
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class MenuSizeOption(
    val label: String,
    val pricePkr: Long
)

object SeedMenuData {
    val legacyStarterIds = setOf(
        "zinger_burger",
        "extreme_pizza",
        "hot_wings",
        "signature_sandwich",
        "pickled_crunch",
        "everyday_value",
        "mighty_zinger",
        "smoky_wings"
    )

    val categories = listOf(
        "ALL",
        "PASTA'S",
        "DEALS",
        "BURGERS",
        "EXTREME PIZZAS",
        "TRADITIONAL PIZZAS",
        "PREMIUM PIZZAS",
        "WRAPS & ROLLS",
        "SANDWICHES",
        "FRIES",
        "FRIED CHICKEN",
        "DIPS & TOPPINGS",
        "SOFT DRINKS"
    )

    val promoBanners = listOf(
        PromoBanner(
            id = "promo_one_meter",
            eyebrow = "1 METER PIZZA FUN",
            title = "ONE METER PIZZA — 3 FLAVORS, 24 SLICES",
            subtitle = "Served with 3 signature dips for 6 people — fresh from the Lahori oven.",
            priceTag = "Rs. 3,990",
            targetCategory = "EXTREME PIZZAS",
            imageKey = "menu_one_meter_pizza"
        ),
        PromoBanner(
            id = "promo_deals",
            eyebrow = "LAHORI VALUE DEALS",
            title = "COMBO DEALS 1 TO 19 & BIRTHDAY DEAL",
            subtitle = "Zinger combos, double pizza feasts, wings, broast & chilled drinks.",
            priceTag = "From Rs. 400",
            targetCategory = "DEALS",
            imageKey = "menu_hero_platter"
        ),
        PromoBanner(
            id = "promo_pasta",
            eyebrow = "CHEF'S PASTA SPECIAL",
            title = "LAHORI SPECIAL & CRUNCHY PASTA",
            subtitle = "Oven-baked cheesy pasta in Half & Full portions, loaded with signature sauces.",
            priceTag = "From Rs. 450",
            targetCategory = "PASTA'S",
            imageKey = "menu_pasta_lahori_sp"
        ),
        PromoBanner(
            id = "promo_burgers",
            eyebrow = "LAHORI CRUNCH FEST",
            title = "LAHORI SP, ZINGER & DOUBLE DECKER BURGERS",
            subtitle = "Crispy whole-muscle Zinger, Beef, Tower & Double Decker burgers.",
            priceTag = "From Rs. 200",
            targetCategory = "BURGERS",
            imageKey = "menu_burger_zinger"
        )
    )

    private fun pizzaTraditionalSizes(s: Long, m: Long, l: Long, xl: Long): List<MenuSizeOption> = listOf(
        MenuSizeOption("S", s),
        MenuSizeOption("M", m),
        MenuSizeOption("L", l),
        MenuSizeOption("XL", xl)
    )

    private fun pizzaSML(s: Long, m: Long, l: Long): List<MenuSizeOption> = listOf(
        MenuSizeOption("S", s),
        MenuSizeOption("M", m),
        MenuSizeOption("L", l)
    )

    private fun pizzaMLXL(m: Long, l: Long, xl: Long): List<MenuSizeOption> = listOf(
        MenuSizeOption("M", m),
        MenuSizeOption("L", l),
        MenuSizeOption("XL", xl)
    )

    private fun pizzaML(m: Long, l: Long): List<MenuSizeOption> = listOf(
        MenuSizeOption("M", m),
        MenuSizeOption("L", l)
    )

    private fun halfFull(half: Long, full: Long): List<MenuSizeOption> = listOf(
        MenuSizeOption("Half", half),
        MenuSizeOption("Full", full)
    )

    private fun wingsPieces(p5: Long, p10: Long): List<MenuSizeOption> = listOf(
        MenuSizeOption("5P", p5),
        MenuSizeOption("10P", p10)
    )

    private fun toppingSizes(s: Long, m: Long, l: Long): List<MenuSizeOption> = listOf(
        MenuSizeOption("S", s),
        MenuSizeOption("M", m),
        MenuSizeOption("L", l)
    )

    val itemSizeOptions: Map<String, List<MenuSizeOption>> = mapOf(
        // PASTA'S (Half / Full — PDF Page 8)
        "lfc_pasta_lahori_special" to halfFull(500L, 800L),
        "lfc_pasta_flaming" to halfFull(450L, 750L),
        "lfc_pasta_crunchy" to halfFull(500L, 800L),
        "lfc_pasta_creamy" to halfFull(450L, 700L),

        // EXTREME & PAN PIZZAS (M / L or S / M / L — PDF Pages 2-3)
        "lfc_ext_lazania" to pizzaML(1450L, 1950L),
        "lfc_ext_premium_pan" to pizzaML(1450L, 1950L),
        "lfc_ext_peri_peri" to pizzaML(1450L, 1950L),
        "lfc_ext_malai_boti" to pizzaML(1450L, 1950L),
        "lfc_ext_kababish" to pizzaML(1450L, 1950L),
        "lfc_ext_crispy" to pizzaML(1450L, 1950L),
        "lfc_ext_pizza_stick" to pizzaSML(550L, 950L, 1350L),

        // TRADITIONAL PIZZAS (S / M / L / XL — PDF Pages 2-3)
        "lfc_trad_lahori_special" to pizzaTraditionalSizes(590L, 1050L, 1590L, 2500L),
        "lfc_trad_fajita" to pizzaTraditionalSizes(550L, 950L, 1450L, 2400L),
        "lfc_trad_tikka" to pizzaTraditionalSizes(550L, 950L, 1450L, 2400L),
        "lfc_trad_chicken_supreme" to pizzaTraditionalSizes(590L, 1000L, 1490L, 2400L),
        "lfc_trad_hot_spicy" to pizzaTraditionalSizes(590L, 1000L, 1490L, 2400L),
        "lfc_trad_peri_tikka" to pizzaTraditionalSizes(590L, 1000L, 1490L, 2400L),
        "lfc_trad_bonfire" to pizzaTraditionalSizes(590L, 1000L, 1490L, 2400L),
        "lfc_trad_shahi" to pizzaTraditionalSizes(590L, 1000L, 1490L, 2400L),
        "lfc_trad_malai_boti" to pizzaTraditionalSizes(600L, 1050L, 1550L, 2500L),
        "lfc_trad_crispy" to pizzaTraditionalSizes(600L, 1290L, 1590L, 2500L),
        "lfc_trad_nawabi" to pizzaTraditionalSizes(550L, 950L, 1450L, 2400L),
        "lfc_trad_creamy_mughlai" to pizzaTraditionalSizes(550L, 950L, 1450L, 2400L),

        // PREMIUM / CRUSTY PIZZAS (PDF Page 4)
        "lfc_prem_bihari_kabab" to pizzaTraditionalSizes(690L, 1190L, 1690L, 2800L),
        "lfc_prem_makhni_kabab" to pizzaMLXL(1190L, 1690L, 2800L),
        "lfc_prem_crown_crust" to pizzaMLXL(1190L, 1690L, 2800L),
        "lfc_prem_royal_crust" to pizzaMLXL(1190L, 1690L, 2800L),
        "lfc_prem_cheese_crust" to pizzaMLXL(1190L, 1690L, 2800L),
        "lfc_prem_double_seekh" to pizzaML(1400L, 1850L),
        "lfc_prem_kabab_crust" to pizzaMLXL(1190L, 1690L, 2800L),
        "lfc_prem_gola_kabab" to pizzaMLXL(1190L, 1690L, 2800L),

        // LOADED FRIES (Half / Full — PDF Page 7)
        "lfc_fries_pizza_loaded" to halfFull(450L, 750L),
        "lfc_fries_crispy_loaded" to halfFull(500L, 800L),

        // FRIED CHICKEN (5P / 10P and Half / Full — PDF Page 7)
        "lfc_fc_crispy_wings" to wingsPieces(300L, 600L),
        "lfc_fc_nuggets" to wingsPieces(300L, 600L),
        "lfc_fc_grilled_wings" to halfFull(300L, 550L),
        "lfc_fc_smoky_wings" to halfFull(350L, 650L),
        "lfc_fc_creamy_wings" to halfFull(350L, 650L),
        "lfc_fc_peri_peri_wings" to halfFull(350L, 650L),
        "lfc_fc_hot_shots" to wingsPieces(250L, 450L),

        // EXTRA TOPPINGS (S / M / L — PDF Page 8)
        "lfc_top_chicken" to toppingSizes(100L, 150L, 200L),
        "lfc_top_cheese" to toppingSizes(100L, 150L, 200L)
    )

    fun sizeOptionsFor(item: MenuItem): List<MenuSizeOption> {
        val normalizedId = item.id.removePrefix("seed_")
        return itemSizeOptions[normalizedId].orEmpty()
    }

    val initialItems = listOf(
        // 1. PASTA'S (PDF Page 8)
        MenuItem(
            id = "seed_lfc_pasta_lahori_special",
            name = "Lahori Sp Pasta",
            description = "Signature oven-baked cheesy pasta loaded with spiced chicken, olives & Lahori sauce (Half / Full).",
            category = "PASTA'S",
            pricePkr = 500L,
            imageKey = "menu_pasta_lahori_sp",
            badgeText = "SIGNATURE",
            available = true,
            sortOrder = 1L
        ),
        MenuItem(
            id = "seed_lfc_pasta_flaming",
            name = "Flaming Pasta",
            description = "Fiery chili-garlic sauce baked golden with mozzarella & tender chicken (Half / Full).",
            category = "PASTA'S",
            pricePkr = 450L,
            imageKey = "menu_pasta_flaming",
            badgeText = "SPICY",
            available = true,
            sortOrder = 2L
        ),
        MenuItem(
            id = "seed_lfc_pasta_crunchy",
            name = "Crunchy Pasta",
            description = "Golden crispy chicken strips over rich cheese sauce, olives & penne pasta (Half / Full).",
            category = "PASTA'S",
            pricePkr = 500L,
            imageKey = "menu_pasta_crunchy",
            badgeText = "CRUNCHY",
            available = true,
            sortOrder = 3L
        ),
        MenuItem(
            id = "seed_lfc_pasta_creamy",
            name = "Creamy Pasta",
            description = "Velvety Alfredo cream sauce tossed with juicy chicken & melted mozzarella (Half / Full).",
            category = "PASTA'S",
            pricePkr = 450L,
            imageKey = "menu_pasta_creamy",
            badgeText = "CREAMY",
            available = true,
            sortOrder = 4L
        ),

        // 2. DEALS (PDF Pages 9 & 10 — Deals 1 to 19 + Birthday Deal)
        MenuItem(
            id = "seed_lfc_deal_1",
            name = "Deal 1",
            description = "1 Zinger Burger + 1 Regular Drink.",
            category = "DEALS",
            pricePkr = 400L,
            imageKey = "menu_deal_1",
            badgeText = "DEAL 1",
            available = true,
            sortOrder = 10L
        ),
        MenuItem(
            id = "seed_lfc_deal_2",
            name = "Deal 2",
            description = "3 Zinger Burgers + 1 Liter Drink.",
            category = "DEALS",
            pricePkr = 1190L,
            imageKey = "menu_deal_2",
            badgeText = "TRIO",
            available = true,
            sortOrder = 11L
        ),
        MenuItem(
            id = "seed_lfc_deal_3",
            name = "Deal 3",
            description = "1 Medium Pizza + 1 Zinger Burger + 1 Liter Drink.",
            category = "DEALS",
            pricePkr = 1440L,
            imageKey = "menu_deal_3",
            badgeText = "POPULAR",
            available = true,
            sortOrder = 12L
        ),
        MenuItem(
            id = "seed_lfc_deal_4",
            name = "Deal 4",
            description = "5 Zinger Burgers + 1.5 Liter Drink.",
            category = "DEALS",
            pricePkr = 1930L,
            imageKey = "menu_deal_4",
            badgeText = "SQUAD",
            available = true,
            sortOrder = 13L
        ),
        MenuItem(
            id = "seed_lfc_deal_5",
            name = "Deal 5",
            description = "1 Large Pizza + 8 Pc Hot Wings + 1 Liter Drink.",
            category = "DEALS",
            pricePkr = 2070L,
            imageKey = "menu_deal_5",
            badgeText = "FEAST",
            available = true,
            sortOrder = 14L
        ),
        MenuItem(
            id = "seed_lfc_deal_6",
            name = "Deal 6",
            description = "1 Large Pizza + 1 Liter Drink.",
            category = "DEALS",
            pricePkr = 1580L,
            imageKey = "menu_deal_6",
            badgeText = "CLASSIC",
            available = true,
            sortOrder = 15L
        ),
        MenuItem(
            id = "seed_lfc_deal_7",
            name = "Deal 7",
            description = "1 Large Pizza + 1 Medium Pizza + 1.5 Liter Drink.",
            category = "DEALS",
            pricePkr = 2560L,
            imageKey = "menu_deal_7",
            badgeText = "DUO PIZZA",
            available = true,
            sortOrder = 16L
        ),
        MenuItem(
            id = "seed_lfc_deal_8",
            name = "Deal 8",
            description = "2 Large Pizzas + 1.5 Liter Drink.",
            category = "DEALS",
            pricePkr = 3050L,
            imageKey = "menu_deal_8",
            badgeText = "2X LARGE",
            available = true,
            sortOrder = 17L
        ),
        MenuItem(
            id = "seed_lfc_deal_9",
            name = "Deal 9",
            description = "2 Medium Pizzas + 1.5 Liter Drink.",
            category = "DEALS",
            pricePkr = 2070L,
            imageKey = "menu_deal_9",
            badgeText = "2X MEDIUM",
            available = true,
            sortOrder = 18L
        ),
        MenuItem(
            id = "seed_lfc_deal_10",
            name = "Deal 10",
            description = "1 Large Pizza + 10 Hot Wings + 1 Liter Drink.",
            category = "DEALS",
            pricePkr = 2190L,
            imageKey = "menu_deal_10",
            badgeText = "MEGA",
            available = true,
            sortOrder = 19L
        ),
        MenuItem(
            id = "seed_lfc_deal_11",
            name = "Deal 11",
            description = "1 Zinger Burger + 1 Paratha Roll + 2 Reg Drinks.",
            category = "DEALS",
            pricePkr = 720L,
            imageKey = "menu_deal_11",
            badgeText = "COMBO",
            available = true,
            sortOrder = 20L
        ),
        MenuItem(
            id = "seed_lfc_deal_12",
            name = "Deal 12",
            description = "1 Small Pizza + 1 Zinger Burger + 5 Hot Wings + 1 Liter Drink.",
            category = "DEALS",
            pricePkr = 1430L,
            imageKey = "menu_deal_12",
            badgeText = "ALL-IN-1",
            available = true,
            sortOrder = 21L
        ),
        MenuItem(
            id = "seed_lfc_deal_13",
            name = "Deal 13",
            description = "1 Small Pizza + 4 Pc Wings + 1 Reg Drink.",
            category = "DEALS",
            pricePkr = 1240L,
            imageKey = "menu_deal_13",
            badgeText = "DEAL 13",
            available = true,
            sortOrder = 22L
        ),
        MenuItem(
            id = "seed_lfc_deal_14",
            name = "Deal 14",
            description = "2 Small Pizzas + 1 Liter Drink.",
            category = "DEALS",
            pricePkr = 1240L,
            imageKey = "menu_deal_14",
            badgeText = "2X SMALL",
            available = true,
            sortOrder = 23L
        ),
        MenuItem(
            id = "seed_lfc_deal_15",
            name = "Deal 15",
            description = "1 Small Pizza + 10 Hot Wings + 1 Liter Drink.",
            category = "DEALS",
            pricePkr = 1340L,
            imageKey = "menu_deal_15",
            badgeText = "WINGS BOX",
            available = true,
            sortOrder = 24L
        ),
        MenuItem(
            id = "seed_lfc_deal_16",
            name = "Deal 16",
            description = "1 Medium Pizza + 6 Hot Wings + 1 Liter Drink.",
            category = "DEALS",
            pricePkr = 1450L,
            imageKey = "menu_deal_16",
            badgeText = "VALUE",
            available = true,
            sortOrder = 25L
        ),
        MenuItem(
            id = "seed_lfc_deal_17",
            name = "Deal 17",
            description = "1 Leg Piece + 1 Chest Piece + 1 Fries + 2 Reg Drinks.",
            category = "DEALS",
            pricePkr = 1060L,
            imageKey = "menu_deal_17",
            badgeText = "BROAST",
            available = true,
            sortOrder = 26L
        ),
        MenuItem(
            id = "seed_lfc_deal_18",
            name = "Deal 18",
            description = "2 Zinger Burgers + 5 Hot Wings + 2 Reg Drinks.",
            category = "DEALS",
            pricePkr = 1160L,
            imageKey = "menu_deal_18",
            badgeText = "DUO",
            available = true,
            sortOrder = 27L
        ),
        MenuItem(
            id = "seed_lfc_deal_19",
            name = "Deal 19",
            description = "Half Pasta + Tikka Sandwich + Fries + 2 Reg Drinks.",
            category = "DEALS",
            pricePkr = 1260L,
            imageKey = "menu_deal_19",
            badgeText = "PLATTER",
            available = true,
            sortOrder = 28L
        ),
        MenuItem(
            id = "seed_lfc_deal_birthday",
            name = "Birthday Deal",
            description = "2 Large Pizzas, 4 Zinger Burgers, 10 Hot Wings, 10 Nuggets, 2 Fries, 1 Pound Cake & 2x 1.5L Drinks.",
            category = "DEALS",
            pricePkr = 7070L,
            imageKey = "menu_deal_birthday",
            badgeText = "PARTY",
            available = true,
            sortOrder = 29L
        ),

        // 3. BURGERS (PDF Page 5)
        MenuItem(
            id = "seed_lfc_burger_zinger",
            name = "Zinger Burger",
            description = "Whole-muscle crunchy fried chicken thigh with iceberg lettuce & signature Lahori mayo.",
            category = "BURGERS",
            pricePkr = 350L,
            imageKey = "menu_burger_zinger",
            badgeText = "BESTSELLER",
            available = true,
            sortOrder = 30L
        ),
        MenuItem(
            id = "seed_lfc_burger_signature",
            name = "Lahori Sp Burger",
            description = "House-special loaded burger with melted cheese, fresh veggies & smoky Lahori sauce.",
            category = "BURGERS",
            pricePkr = 450L,
            imageKey = "menu_burger_lahori_sp",
            badgeText = "SIGNATURE",
            available = true,
            sortOrder = 31L
        ),
        MenuItem(
            id = "seed_lfc_burger_patty",
            name = "Patty Burger",
            description = "Crispy golden chicken patty on a toasted sesame bun with fresh onion & classic mayo.",
            category = "BURGERS",
            pricePkr = 300L,
            imageKey = "menu_burger_patty",
            badgeText = "",
            available = true,
            sortOrder = 32L
        ),
        MenuItem(
            id = "seed_lfc_burger_double_decker",
            name = "Double Decker",
            description = "Two stacked crispy chicken fillets, double cheddar cheese & signature dressing.",
            category = "BURGERS",
            pricePkr = 600L,
            imageKey = "menu_burger_double_decker",
            badgeText = "DOUBLE",
            available = true,
            sortOrder = 33L
        ),
        MenuItem(
            id = "seed_lfc_burger_jalapeno",
            name = "Jalapeno Burger",
            description = "Crispy zinger fillet layered with zesty sliced jalapenos & creamy pepper sauce.",
            category = "BURGERS",
            pricePkr = 400L,
            imageKey = "menu_burger_jalapeno",
            badgeText = "SPICY",
            available = true,
            sortOrder = 34L
        ),
        MenuItem(
            id = "seed_lfc_burger_beef",
            name = "Beef Burger",
            description = "Char-grilled juicy beef patty with melted cheddar, crisp lettuce, tomato & smoky sauce.",
            category = "BURGERS",
            pricePkr = 650L,
            imageKey = "menu_burger_beef",
            badgeText = "GRILLED",
            available = true,
            sortOrder = 35L
        ),
        MenuItem(
            id = "seed_lfc_burger_tower",
            name = "Tower Burger",
            description = "Crunchy chicken fillet, golden hash brown, melted cheese slice & tangy salsa.",
            category = "BURGERS",
            pricePkr = 550L,
            imageKey = "menu_burger_tower",
            badgeText = "LOADED",
            available = true,
            sortOrder = 36L
        ),
        MenuItem(
            id = "seed_lfc_burger_chicken",
            name = "Chicken Burger",
            description = "Everyday favorite tender chicken sub-style burger with cheese & creamy mayo.",
            category = "BURGERS",
            pricePkr = 200L,
            imageKey = "menu_burger_chicken",
            badgeText = "VALUE",
            available = true,
            sortOrder = 37L
        ),
        MenuItem(
            id = "seed_lfc_burger_extra_cheese",
            name = "Extra Cheese Slice",
            description = "Add an extra rich cheddar cheese slice to any burger or sandwich.",
            category = "BURGERS",
            pricePkr = 70L,
            imageKey = "menu_extra_cheese_slice",
            badgeText = "ADD-ON",
            available = true,
            sortOrder = 38L
        ),

        // 4. EXTREME & SPECIALTY PAN PIZZAS (PDF Pages 2 & 3)
        MenuItem(
            id = "seed_lfc_one_meter_pizza",
            name = "One Meter Pizza",
            description = "1 Meter Pizza Fun — 3 Flavors, 3 Dips, 24 Slices, 6 Serves. Ultimate party centerpiece!",
            category = "EXTREME PIZZAS",
            pricePkr = 3990L,
            imageKey = "menu_one_meter_pizza",
            badgeText = "1 METER",
            available = true,
            sortOrder = 39L
        ),
        MenuItem(
            id = "seed_lfc_ext_lazania",
            name = "Lazania Pizza",
            description = "Special Sauce, Special Chicken, Double Cheese, Capsicum, Tomato, Black Olive, Spicy (M / L).",
            category = "EXTREME PIZZAS",
            pricePkr = 1450L,
            imageKey = "menu_pizza_lazania",
            badgeText = "LAZANIA",
            available = true,
            sortOrder = 40L
        ),
        MenuItem(
            id = "seed_lfc_ext_premium_pan",
            name = "Premium Pan Pizza",
            description = "Flavors: Peri Peri, Kababish, Malai Boti — Premium Sauce, Double Layer Dough, Cheese, Jalapeno & Peppers (M / L).",
            category = "EXTREME PIZZAS",
            pricePkr = 1450L,
            imageKey = "menu_pizza_premium_pan",
            badgeText = "PAN PIZZA",
            available = true,
            sortOrder = 41L
        ),
        MenuItem(
            id = "seed_lfc_ext_pizza_stick",
            name = "Doner Pizza Stick",
            description = "Special Sauce, Chicken, Cheese, Corn, Capsicum, Black Olive, Tomato, Onion, Spicy (S / M / L).",
            category = "EXTREME PIZZAS",
            pricePkr = 550L,
            imageKey = "menu_pizza_doner_stick",
            badgeText = "DONER STICK",
            available = true,
            sortOrder = 42L
        ),
        MenuItem(
            id = "seed_lfc_ext_peri_peri",
            name = "Extreme Peri Peri",
            description = "Double-loaded extreme crust with peri peri chicken, jalapenos & mozzarella (M / L).",
            category = "EXTREME PIZZAS",
            pricePkr = 1450L,
            imageKey = "menu_pizza_peri_peri",
            badgeText = "EXTREME",
            available = true,
            sortOrder = 43L
        ),
        MenuItem(
            id = "seed_lfc_ext_malai_boti",
            name = "Extreme Malai Boti",
            description = "Ultra-cheesy extreme pizza topped with creamy smoky malai boti chunks (M / L).",
            category = "EXTREME PIZZAS",
            pricePkr = 1450L,
            imageKey = "menu_pizza_malai_boti",
            badgeText = "CREAMY",
            available = true,
            sortOrder = 44L
        ),
        MenuItem(
            id = "seed_lfc_ext_kababish",
            name = "Extreme Kababish",
            description = "Stuffed kabab-packed extreme crust with spiced chicken & special sauce (M / L).",
            category = "EXTREME PIZZAS",
            pricePkr = 1450L,
            imageKey = "menu_pizza_bihari_kabab",
            badgeText = "LOADED",
            available = true,
            sortOrder = 45L
        ),
        MenuItem(
            id = "seed_lfc_ext_crispy",
            name = "Extreme Crispy",
            description = "Crunchy fried chicken pops over melted mozzarella & tangy drizzle (M / L).",
            category = "EXTREME PIZZAS",
            pricePkr = 1450L,
            imageKey = "menu_pizza_crunchy_cheese",
            badgeText = "CRISPY",
            available = true,
            sortOrder = 46L
        ),

        // 5. TRADITIONAL PIZZAS (S / M / L / XL — PDF Pages 2 & 3)
        MenuItem(
            id = "seed_lfc_trad_lahori_special",
            name = "Lahori Special Pizza",
            description = "Special Sauce, Onion, Special Chicken, Cheese, Kabab Slice, Capsicum, Jalapeno, Mushrooms, Tomato, Black Olive, Spicy.",
            category = "TRADITIONAL PIZZAS",
            pricePkr = 590L,
            imageKey = "menu_pizza_lahori_special",
            badgeText = "HOUSE PICK",
            available = true,
            sortOrder = 50L
        ),
        MenuItem(
            id = "seed_lfc_trad_fajita",
            name = "Chicken Fajita Pizza",
            description = "Red Sauce, Chicken, Cheese, Capsicum, Black Olive, Mushroom, Spicy.",
            category = "TRADITIONAL PIZZAS",
            pricePkr = 550L,
            imageKey = "menu_pizza_fajita",
            badgeText = "CLASSIC",
            available = true,
            sortOrder = 51L
        ),
        MenuItem(
            id = "seed_lfc_trad_tikka",
            name = "Chicken Tikka Pizza",
            description = "Red Sauce, Tikka Chicken, Cheese, Tomato, Black Olive, Spicy.",
            category = "TRADITIONAL PIZZAS",
            pricePkr = 550L,
            imageKey = "menu_pizza_tikka",
            badgeText = "DESI",
            available = true,
            sortOrder = 52L
        ),
        MenuItem(
            id = "seed_lfc_trad_chicken_supreme",
            name = "Chicken Supreme Pizza",
            description = "Bonfire Sauce, Supreme Chicken, Cheese, Mushroom, Capsicum, Black Olive, Tomato, Onion, Spicy.",
            category = "TRADITIONAL PIZZAS",
            pricePkr = 590L,
            imageKey = "menu_pizza_supreme",
            badgeText = "LOADED",
            available = true,
            sortOrder = 53L
        ),
        MenuItem(
            id = "seed_lfc_trad_hot_spicy",
            name = "Hot n Spicy Pizza",
            description = "Hot n Spicy Sauce, Spicy Chicken, Cheese, Capsicum, Jalapeno, Spicy.",
            category = "TRADITIONAL PIZZAS",
            pricePkr = 590L,
            imageKey = "menu_pizza_hot_spicy",
            badgeText = "HOT",
            available = true,
            sortOrder = 54L
        ),
        MenuItem(
            id = "seed_lfc_trad_peri_tikka",
            name = "Peri Peri Pizza",
            description = "Peri Peri Sauce, Peri Peri Chicken, Cheese, Sauce Topping, All Crushed Vegetables, Spicy.",
            category = "TRADITIONAL PIZZAS",
            pricePkr = 590L,
            imageKey = "menu_pizza_peri_peri",
            badgeText = "PERI PERI",
            available = true,
            sortOrder = 55L
        ),
        MenuItem(
            id = "seed_lfc_trad_bonfire",
            name = "BoneFire Pizza",
            description = "Bonefire Sauce, BoneFire Chicken, Cheese, Jalapeno, Tomato, Onion, Black Olive, Spicy.",
            category = "TRADITIONAL PIZZAS",
            pricePkr = 590L,
            imageKey = "menu_pizza_bonfire",
            badgeText = "SMOKY",
            available = true,
            sortOrder = 56L
        ),
        MenuItem(
            id = "seed_lfc_trad_shahi",
            name = "Shahi Pizza",
            description = "Special Sauce, Shahi Chicken, Cheese, Jalapeno, Onion, Spicy.",
            category = "TRADITIONAL PIZZAS",
            pricePkr = 590L,
            imageKey = "menu_pizza_shahi",
            badgeText = "ROYAL",
            available = true,
            sortOrder = 57L
        ),
        MenuItem(
            id = "seed_lfc_trad_malai_boti",
            name = "Malai Boti Pizza",
            description = "Creamy Sauce, Malai Chicken, Cheese, Cream Topping, Black Olive, Non Spicy.",
            category = "TRADITIONAL PIZZAS",
            pricePkr = 600L,
            imageKey = "menu_pizza_malai_boti",
            badgeText = "CREAMY",
            available = true,
            sortOrder = 58L
        ),
        MenuItem(
            id = "seed_lfc_trad_crispy",
            name = "Crunchy Cheese Pizza",
            description = "Special Sauce, Crunchy Chicken, Cheese, Tomato, Capsicum, Olive, Spicy.",
            category = "TRADITIONAL PIZZAS",
            pricePkr = 600L,
            imageKey = "menu_pizza_crunchy_cheese",
            badgeText = "CRUNCHY",
            available = true,
            sortOrder = 59L
        ),
        MenuItem(
            id = "seed_lfc_trad_nawabi",
            name = "Nawabi Pizza",
            description = "Aromatic Mughlai-spiced chicken, sweet corn, mushrooms & mozzarella.",
            category = "TRADITIONAL PIZZAS",
            pricePkr = 550L,
            imageKey = "menu_pizza_shahi",
            badgeText = "",
            available = true,
            sortOrder = 60L
        ),
        MenuItem(
            id = "seed_lfc_trad_creamy_mughlai",
            name = "Creamy Mughlai Pizza",
            description = "Rich garlic cream base topped with roasted chicken & mozzarella.",
            category = "TRADITIONAL PIZZAS",
            pricePkr = 550L,
            imageKey = "menu_pizza_malai_boti",
            badgeText = "RICH",
            available = true,
            sortOrder = 61L
        ),

        // 6. PREMIUM / CRUSTY PIZZAS (PDF Page 4)
        MenuItem(
            id = "seed_lfc_prem_bihari_kabab",
            name = "Bihari Kabab Pizza",
            description = "Bihari Sauce, Bihari Chicken, Cheese, Seekh Kabab on Top, Jalapeno, Spicy.",
            category = "PREMIUM PIZZAS",
            pricePkr = 690L,
            imageKey = "menu_pizza_bihari_kabab",
            badgeText = "CRUSTY",
            available = true,
            sortOrder = 70L
        ),
        MenuItem(
            id = "seed_lfc_prem_makhni_kabab",
            name = "Makhni Kabab Pizza",
            description = "Special Sauce, Red Chicken, Extra Cheese, Seekh Kabab on Top, Onion, Tomato, Spicy (M / L / XL).",
            category = "PREMIUM PIZZAS",
            pricePkr = 1190L,
            imageKey = "menu_pizza_makhni_kabab",
            badgeText = "PREMIUM",
            available = true,
            sortOrder = 71L
        ),
        MenuItem(
            id = "seed_lfc_prem_crown_crust",
            name = "Crown Crust Pizza",
            description = "Crown Edge of Dough filled with Kabab, Special Sauce, Special Chicken, Extra Cheese, Jalapeno, Capsicum, Black Olives, Spicy.",
            category = "PREMIUM PIZZAS",
            pricePkr = 1190L,
            imageKey = "menu_pizza_crown_crust",
            badgeText = "CROWN",
            available = true,
            sortOrder = 72L
        ),
        MenuItem(
            id = "seed_lfc_prem_royal_crust",
            name = "Royal Crust Pizza",
            description = "Crown Edge of Dough filled with Full Kabab, Special Sauce, Special Chicken, Extra Cheese, Tomato, Capsicum, Black Olives, Spicy.",
            category = "PREMIUM PIZZAS",
            pricePkr = 1190L,
            imageKey = "menu_pizza_royal_crust",
            badgeText = "ROYAL",
            available = true,
            sortOrder = 73L
        ),
        MenuItem(
            id = "seed_lfc_prem_cheese_crust",
            name = "Cheese Crust Pizza",
            description = "Edge of Dough filled with Cheese, Creamy Sauce, Special Chicken, Extra Cheese, Jalapeno, Capsicum, Black Olives, Spicy.",
            category = "PREMIUM PIZZAS",
            pricePkr = 1190L,
            imageKey = "menu_pizza_cheese_crust",
            badgeText = "CHEESY",
            available = true,
            sortOrder = 74L
        ),
        MenuItem(
            id = "seed_lfc_prem_double_seekh",
            name = "Double Seekh Kabab Pizza",
            description = "Edge of Dough filled with Kabab, Bihari Sauce, Bihari Chicken, Extra Cheese, Seekh Kabab Topping, Jalapeno & Olives (M / L).",
            category = "PREMIUM PIZZAS",
            pricePkr = 1400L,
            imageKey = "menu_pizza_double_seekh",
            badgeText = "SIGNATURE",
            available = true,
            sortOrder = 75L
        ),
        MenuItem(
            id = "seed_lfc_prem_kabab_crust",
            name = "Kabab Crust Pizza",
            description = "Seekh kabab stuffed edge crust topped with spiced chicken & olives (M / L / XL).",
            category = "PREMIUM PIZZAS",
            pricePkr = 1190L,
            imageKey = "menu_pizza_royal_crust",
            badgeText = "CRUST",
            available = true,
            sortOrder = 76L
        ),
        MenuItem(
            id = "seed_lfc_prem_gola_kabab",
            name = "Gola Kabab Pizza",
            description = "Savory gola kabab medallions over rich tomato sauce & stretchy mozzarella (M / L / XL).",
            category = "PREMIUM PIZZAS",
            pricePkr = 1190L,
            imageKey = "menu_pizza_makhni_kabab",
            badgeText = "PREMIUM",
            available = true,
            sortOrder = 77L
        ),

        // 7. WRAPS & ROLLS (PDF Page 6)
        MenuItem(
            id = "seed_lfc_wrap_lahori_special",
            name = "Lahori Sp Roll",
            description = "House special crispy chicken, fresh veggies, melted cheese & signature Lahori sauce roll.",
            category = "WRAPS & ROLLS",
            pricePkr = 400L,
            imageKey = "menu_roll_lahori_sp",
            badgeText = "SPECIAL",
            available = true,
            sortOrder = 80L
        ),
        MenuItem(
            id = "seed_lfc_wrap_bihari_kabab",
            name = "Bihari Kabab Roll",
            description = "Charcoal-spiced Bihari kabab rolled with crisp lettuce, onion rings & mint chutney.",
            category = "WRAPS & ROLLS",
            pricePkr = 350L,
            imageKey = "menu_roll_bihari_kabab",
            badgeText = "SMOKY",
            available = true,
            sortOrder = 81L
        ),
        MenuItem(
            id = "seed_lfc_wrap_tikka_paratha",
            name = "Tikka Paratha Roll",
            description = "Flaky golden paratha filled with smoky chicken tikka, fresh lettuce & chutney.",
            category = "WRAPS & ROLLS",
            pricePkr = 250L,
            imageKey = "menu_roll_tikka_paratha",
            badgeText = "PARATHA",
            available = true,
            sortOrder = 82L
        ),
        MenuItem(
            id = "seed_lfc_wrap_zinger_roll",
            name = "Zinger Roll",
            description = "Crunchy zinger strips wrapped in warm flatbread with garlic mayo & crisp lettuce.",
            category = "WRAPS & ROLLS",
            pricePkr = 350L,
            imageKey = "menu_roll_zinger",
            badgeText = "CRUNCHY",
            available = true,
            sortOrder = 83L
        ),
        MenuItem(
            id = "seed_lfc_wrap_malai_boti",
            name = "Malai Boti Roll",
            description = "Soft creamy malai chicken boti wrapped with garlic sauce & onions in golden paratha.",
            category = "WRAPS & ROLLS",
            pricePkr = 350L,
            imageKey = "menu_roll_malai_boti",
            badgeText = "CREAMY",
            available = true,
            sortOrder = 84L
        ),
        MenuItem(
            id = "seed_lfc_wrap_grill",
            name = "Lahori Sp Grilled Wrap",
            description = "Flame-grilled tender chicken breast, jalapenos & smoky sauce in toasted tortilla wrap.",
            category = "WRAPS & ROLLS",
            pricePkr = 490L,
            imageKey = "menu_wrap_lahori_grilled",
            badgeText = "GRILLED",
            available = true,
            sortOrder = 85L
        ),
        MenuItem(
            id = "seed_lfc_wrap_chicken_shawarma",
            name = "Chicken Shawarma",
            description = "Classic Arabian-style grilled chicken shawarma with garlic dip & pickled veggies.",
            category = "WRAPS & ROLLS",
            pricePkr = 180L,
            imageKey = "menu_shawarma_chicken",
            badgeText = "VALUE",
            available = true,
            sortOrder = 86L
        ),
        MenuItem(
            id = "seed_lfc_wrap_zinger_shawarma",
            name = "Zinger Shawarma",
            description = "Soft pita bread stuffed with crispy zinger strips, lettuce & creamy garlic mayo.",
            category = "WRAPS & ROLLS",
            pricePkr = 300L,
            imageKey = "menu_shawarma_zinger",
            badgeText = "POPULAR",
            available = true,
            sortOrder = 87L
        ),

        // 8. SANDWICHES (PDF Page 7)
        MenuItem(
            id = "seed_lfc_sand_lahori_special",
            name = "Lahori Sp Sandwich",
            description = "Loaded house sandwich with grilled & crispy chicken, egg, melted cheese & fries.",
            category = "SANDWICHES",
            pricePkr = 500L,
            imageKey = "menu_sandwich_club",
            badgeText = "CHEF'S PICK",
            available = true,
            sortOrder = 90L
        ),
        MenuItem(
            id = "seed_lfc_sand_chicken",
            name = "Chicken Sandwich",
            description = "Savory shredded seasoned chicken, melted cheese & creamy mayo on toasted slices.",
            category = "SANDWICHES",
            pricePkr = 450L,
            imageKey = "menu_sandwich_club",
            badgeText = "CLASSIC",
            available = true,
            sortOrder = 91L
        ),
        MenuItem(
            id = "seed_lfc_sand_club",
            name = "Club Sandwich",
            description = "Triple-decker toasted club sandwich with chicken, omelette, cheese & crisp greens.",
            category = "SANDWICHES",
            pricePkr = 450L,
            imageKey = "menu_sandwich_club",
            badgeText = "TRIPLE",
            available = true,
            sortOrder = 92L
        ),
        MenuItem(
            id = "seed_lfc_sand_malai_panini",
            name = "Malai Panini Sandwich",
            description = "Pressed artisan panini stuffed with tender malai boti & melted cheese.",
            category = "SANDWICHES",
            pricePkr = 450L,
            imageKey = "menu_sandwich_club",
            badgeText = "GRILLED",
            available = true,
            sortOrder = 93L
        ),

        // 9. FRIES & LOADED FRIES (PDF Page 7)
        MenuItem(
            id = "seed_lfc_fries_plain",
            name = "Plain Fries",
            description = "Generous bucket of crunchy golden straight-cut potato fries.",
            category = "FRIES",
            pricePkr = 250L,
            imageKey = "menu_fries_regular",
            badgeText = "CRISPY",
            available = true,
            sortOrder = 100L
        ),
        MenuItem(
            id = "seed_lfc_fries_masala",
            name = "Masala Fries",
            description = "Hot crispy fries dusted in tangy Lahori chatpata spice mix.",
            category = "FRIES",
            pricePkr = 350L,
            imageKey = "menu_fries_regular",
            badgeText = "SPICY",
            available = true,
            sortOrder = 101L
        ),
        MenuItem(
            id = "seed_lfc_fries_pizza_loaded",
            name = "Pizza Loaded Fries",
            description = "Oven-baked fries smothered in pizza sauce, chicken chunks, olives, jalapenos & cheese (Half / Full).",
            category = "FRIES",
            pricePkr = 450L,
            imageKey = "menu_fries_loaded",
            badgeText = "LOADED",
            available = true,
            sortOrder = 102L
        ),
        MenuItem(
            id = "seed_lfc_fries_crispy_loaded",
            name = "Crispy Loaded Fries",
            description = "Crispy fries loaded with crunchy zinger bites, melted cheese & dynamite sauce (Half / Full).",
            category = "FRIES",
            pricePkr = 500L,
            imageKey = "menu_fries_loaded",
            badgeText = "CRUNCHY",
            available = true,
            sortOrder = 103L
        ),
        MenuItem(
            id = "seed_lfc_fries_single",
            name = "Single Serving Fries",
            description = "Crisp golden salted potato fries — solo snack portion.",
            category = "FRIES",
            pricePkr = 150L,
            imageKey = "menu_fries_regular",
            badgeText = "",
            available = true,
            sortOrder = 104L
        ),

        // 10. FRIED CHICKEN (PDF Page 7)
        MenuItem(
            id = "seed_lfc_fc_crispy_wings",
            name = "Crispy Wings",
            description = "Golden crunchy marinated chicken wings bucket with garlic dip (5 Pcs / 10 Pcs).",
            category = "FRIED CHICKEN",
            pricePkr = 300L,
            imageKey = "menu_fc_crispy_wings",
            badgeText = "CRISPY",
            available = true,
            sortOrder = 110L
        ),
        MenuItem(
            id = "seed_lfc_fc_nuggets",
            name = "Chicken Nuggets",
            description = "Tender golden breaded chicken nuggets with dipping sauce (5 Pcs / 10 Pcs).",
            category = "FRIED CHICKEN",
            pricePkr = 300L,
            imageKey = "menu_fc_nuggets",
            badgeText = "GOLDEN",
            available = true,
            sortOrder = 111L
        ),
        MenuItem(
            id = "seed_lfc_fc_grilled_wings",
            name = "Grilled Wings",
            description = "Flame-grilled spiced chicken wings glazed with herbs (Half / Full).",
            category = "FRIED CHICKEN",
            pricePkr = 300L,
            imageKey = "menu_fc_glazed_wings",
            badgeText = "GRILLED",
            available = true,
            sortOrder = 112L
        ),
        MenuItem(
            id = "seed_lfc_fc_smoky_wings",
            name = "Smoky BBQ Wings",
            description = "Tossed in rich smoky BBQ glaze (Half / Full).",
            category = "FRIED CHICKEN",
            pricePkr = 350L,
            imageKey = "menu_fc_glazed_wings",
            badgeText = "SMOKY BBQ",
            available = true,
            sortOrder = 113L
        ),
        MenuItem(
            id = "seed_lfc_fc_creamy_wings",
            name = "Creamy Wings",
            description = "Tender wings coated in smooth garlic parmesan cream sauce (Half / Full).",
            category = "FRIED CHICKEN",
            pricePkr = 350L,
            imageKey = "menu_fc_glazed_wings",
            badgeText = "CREAMY",
            available = true,
            sortOrder = 114L
        ),
        MenuItem(
            id = "seed_lfc_fc_peri_peri_wings",
            name = "Peri Peri Wings",
            description = "Zesty fiery peri peri spiced glazed wings (Half / Full).",
            category = "FRIED CHICKEN",
            pricePkr = 350L,
            imageKey = "menu_fc_glazed_wings",
            badgeText = "HOT",
            available = true,
            sortOrder = 115L
        ),
        MenuItem(
            id = "seed_lfc_fc_hot_shots",
            name = "Hot Shot",
            description = "Bite-sized spicy crispy chicken pops fried to perfection (5 Pcs / 10 Pcs).",
            category = "FRIED CHICKEN",
            pricePkr = 250L,
            imageKey = "menu_fc_hot_shot",
            badgeText = "BITES",
            available = true,
            sortOrder = 116L
        ),
        MenuItem(
            id = "seed_lfc_fc_leg_broast",
            name = "Crispy Chicken Broast (Leg)",
            description = "Juicy quarter leg & thigh broast pressure-fried crisp in Lahori spices.",
            category = "FRIED CHICKEN",
            pricePkr = 350L,
            imageKey = "menu_fc_broast",
            badgeText = "BROAST",
            available = true,
            sortOrder = 117L
        ),
        MenuItem(
            id = "seed_lfc_fc_chest_broast",
            name = "Crispy Chicken Broast (Chest)",
            description = "Hearty crispy breast quarter broast seasoned in Lahori spices.",
            category = "FRIED CHICKEN",
            pricePkr = 400L,
            imageKey = "menu_fc_broast",
            badgeText = "BROAST",
            available = true,
            sortOrder = 118L
        ),

        // 11. DIPS & EXTRA TOPPINGS (PDF Page 8)
        MenuItem(
            id = "seed_lfc_dip_sauce",
            name = "Dip Sauce",
            description = "Classic house dipping sauce cup.",
            category = "DIPS & TOPPINGS",
            pricePkr = 50L,
            imageKey = "menu_dips_assorted",
            badgeText = "DIP",
            available = true,
            sortOrder = 120L
        ),
        MenuItem(
            id = "seed_lfc_dip_special",
            name = "Special Sauce",
            description = "Lahori Food Cafe signature zesty sauce dip.",
            category = "DIPS & TOPPINGS",
            pricePkr = 50L,
            imageKey = "menu_dips_assorted",
            badgeText = "SPECIAL",
            available = true,
            sortOrder = 121L
        ),
        MenuItem(
            id = "seed_lfc_dip_bonfire",
            name = "BoneFire Sauce",
            description = "Smoky fiery BoneFire signature dip cup.",
            category = "DIPS & TOPPINGS",
            pricePkr = 50L,
            imageKey = "menu_dips_assorted",
            badgeText = "SMOKY",
            available = true,
            sortOrder = 122L
        ),
        MenuItem(
            id = "seed_lfc_dip_peri",
            name = "Peri Peri Sauce",
            description = "Fiery chili-herb peri peri dip cup.",
            category = "DIPS & TOPPINGS",
            pricePkr = 50L,
            imageKey = "menu_dips_assorted",
            badgeText = "SPICY",
            available = true,
            sortOrder = 123L
        ),
        MenuItem(
            id = "seed_lfc_dip_garlic",
            name = "Garlic Mayo",
            description = "Rich creamy roasted garlic mayo dip cup.",
            category = "DIPS & TOPPINGS",
            pricePkr = 50L,
            imageKey = "menu_dips_assorted",
            badgeText = "CREAMY",
            available = true,
            sortOrder = 124L
        ),
        MenuItem(
            id = "seed_lfc_top_chicken",
            name = "Extra Chicken Topping",
            description = "Double up grilled spiced chicken topping on your pizza (S / M / L).",
            category = "DIPS & TOPPINGS",
            pricePkr = 100L,
            imageKey = "menu_pizza_lahori_special",
            badgeText = "TOPPING",
            available = true,
            sortOrder = 125L
        ),
        MenuItem(
            id = "seed_lfc_top_cheese",
            name = "Extra Cheese Topping",
            description = "Extra layer of stretchy mozzarella cheese for your pizza (S / M / L).",
            category = "DIPS & TOPPINGS",
            pricePkr = 100L,
            imageKey = "menu_extra_cheese_slice",
            badgeText = "TOPPING",
            available = true,
            sortOrder = 126L
        ),

        // 12. SOFT DRINKS (PDF Page 8)
        MenuItem(
            id = "seed_lfc_drink_regular",
            name = "Buddy Drink",
            description = "Chilled carbonated Buddy soft drink bottle.",
            category = "SOFT DRINKS",
            pricePkr = 100L,
            imageKey = "menu_drinks_assorted",
            badgeText = "BUDDY",
            available = true,
            sortOrder = 130L
        ),
        MenuItem(
            id = "seed_lfc_drink_500ml",
            name = "Cold Drink 500ml",
            description = "Chilled 500 ml carbonated cold drink bottle.",
            category = "SOFT DRINKS",
            pricePkr = 140L,
            imageKey = "menu_drinks_assorted",
            badgeText = "500 ML",
            available = true,
            sortOrder = 131L
        ),
        MenuItem(
            id = "seed_lfc_drink_1l",
            name = "1 Liter Drink",
            description = "Chilled 1 Liter sharing cold drink bottle.",
            category = "SOFT DRINKS",
            pricePkr = 200L,
            imageKey = "menu_drinks_assorted",
            badgeText = "1 LITER",
            available = true,
            sortOrder = 132L
        ),
        MenuItem(
            id = "seed_lfc_drink_15l",
            name = "1.5 Liter Drink",
            description = "Chilled 1.5 Liter family cold drink bottle.",
            category = "SOFT DRINKS",
            pricePkr = 260L,
            imageKey = "menu_drinks_assorted",
            badgeText = "1.5 L",
            available = true,
            sortOrder = 133L
        ),
        MenuItem(
            id = "seed_lfc_drink_water_small",
            name = "Small Water",
            description = "Chilled mineral water bottle (Small).",
            category = "SOFT DRINKS",
            pricePkr = 60L,
            imageKey = "menu_drinks_assorted",
            badgeText = "WATER",
            available = true,
            sortOrder = 134L
        ),
        MenuItem(
            id = "seed_lfc_drink_water_large",
            name = "Large Water",
            description = "Chilled mineral water bottle (Large).",
            category = "SOFT DRINKS",
            pricePkr = 110L,
            imageKey = "menu_drinks_assorted",
            badgeText = "WATER",
            available = true,
            sortOrder = 135L
        )
    )

    val defaultPromotions = listOf(
        Promotion(
            id = "lahori20",
            code = "LAHORI20",
            discountType = "PERCENT",
            discountValue = 20L,
            minOrderPkr = 500L,
            active = true
        ),
        Promotion(
            id = "zinger150",
            code = "ZINGER150",
            discountType = "FLAT",
            discountValue = 150L,
            minOrderPkr = 600L,
            active = true
        ),
        Promotion(
            id = "welcome100",
            code = "WELCOME100",
            discountType = "FLAT",
            discountValue = 100L,
            minOrderPkr = 300L,
            active = true
        )
    )
}
