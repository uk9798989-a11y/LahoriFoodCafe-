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

    fun nextStatus(): OrderStatus? = when (this) {
        Confirmed -> Preparing
        Preparing -> Dispatched
        Dispatched -> Delivered
        Delivered -> null
    }

    companion object {
        fun fromString(raw: String?): OrderStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: Confirmed
    }
}

data class BranchInfo(
    val id: String,
    val name: String,
    val shortAddress: String,
    val areas: List<String>
)

object BranchDirectory {
    val branch1 = BranchInfo(
        id = "branch_1",
        name = "Lahori Cafe — Gulberg III",
        shortAddress = "MM Alam Road, Gulberg III, Lahore",
        areas = listOf("Gulberg", "Model Town", "Garden Town", "Liberty", "Shadman", "Mall Road", "Muslim Town", "Ichhra")
    )

    val branch2 = BranchInfo(
        id = "branch_2",
        name = "Lahori Cafe — DHA Phase 5",
        shortAddress = "Commercial Sector Z, DHA Phase 5, Lahore",
        areas = listOf("DHA", "Cantt", "Johar Town", "Bahria Town", "Wapda Town", "Cavalry Ground", "Askari", "Bedian")
    )

    val allBranches = listOf(branch1, branch2)

    fun byId(branchId: String): BranchInfo =
        allBranches.firstOrNull { it.id == branchId } ?: branch1

    fun routeAddressToBranch(areaOrAddress: String): BranchInfo {
        val normalized = areaOrAddress.lowercase()
        val branch2Match = branch2.areas.any { normalized.contains(it.lowercase()) }
        if (branch2Match) return branch2
        val branch1Match = branch1.areas.any { normalized.contains(it.lowercase()) }
        if (branch1Match) return branch1
        // Deterministic closest fallback based on hash/length when custom street entered
        return if (normalized.contains("phase") || normalized.contains("south") || normalized.contains("town")) {
            branch2
        } else {
            branch1
        }
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
)

@Keep
@IgnoreExtraProperties
data class Promotion(
    @DocumentId val id: String = "",
    val code: String = "",
    val discountType: String = "FLAT",
    val discountValue: Long = 0L,
    val minOrderPkr: Long = 0L,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun computeDiscount(subtotalPkr: Long): Long {
        if (!active || subtotalPkr < minOrderPkr || subtotalPkr <= 0L) return 0L
        return if (discountType.equals("PERCENT", ignoreCase = true)) {
            ((subtotalPkr * discountValue.coerceIn(0L, 90L)) / 100L).coerceIn(0L, subtotalPkr)
        } else {
            discountValue.coerceIn(0L, subtotalPkr)
        }
    }
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

object SeedMenuData {
    val categories = listOf(
        "ALL",
        "GET PICKLE'D",
        "EVERYDAY VALUE",
        "SIGNATURE BURGERS",
        "EXTREME PIZZAS",
        "HOT WINGS",
        "SANDWICHES"
    )

    val promoBanners = listOf(
        PromoBanner(
            id = "promo_zinger",
            eyebrow = "LAHORI CRUNCH FEST",
            title = "ZINGER BURGER DUO",
            subtitle = "Double crunch thigh fillet, fiery dynamite mayo & toasted sesame bun.",
            priceTag = "Rs. 650",
            targetCategory = "SIGNATURE BURGERS",
            imageKey = "zinger_burger"
        ),
        PromoBanner(
            id = "promo_pizza",
            eyebrow = "WOOD-FIRED HEAT",
            title = "THICK-CRUST EXTREME PIZZA",
            subtitle = "Loaded mozzarella crown, spiced tikka chunks & jalapeno drizzle.",
            priceTag = "Rs. 1,650",
            targetCategory = "EXTREME PIZZAS",
            imageKey = "extreme_pizza"
        ),
        PromoBanner(
            id = "promo_wings",
            eyebrow = "SPICE ALERT",
            title = "HOT WINGS (8 PCS) BUCKET",
            subtitle = "Golden crispy wings tossed in signature Lahori chili glaze.",
            priceTag = "Rs. 580",
            targetCategory = "HOT WINGS",
            imageKey = "hot_wings"
        ),
        PromoBanner(
            id = "promo_pickle",
            eyebrow = "LIMITED DROP",
            title = "GET PICKLE'D CRUNCH",
            subtitle = "Dill pickle crunch slaw meets our spicy crispy fillet.",
            priceTag = "Rs. 720",
            targetCategory = "GET PICKLE'D",
            imageKey = "pickled_crunch"
        )
    )

    val initialItems = listOf(
        MenuItem(
            id = "seed_zinger_burger",
            name = "Zinger Burger",
            description = "Signature whole-muscle crispy chicken fillet with iceberg & fiery Lahori mayo.",
            category = "SIGNATURE BURGERS",
            pricePkr = 650L,
            imageKey = "zinger_burger",
            badgeText = "BESTSELLER",
            available = true,
            sortOrder = 1L
        ),
        MenuItem(
            id = "seed_extreme_pizza",
            name = "Thick-crust Extreme Pizza",
            description = "Deep-pan golden crust overloaded with spicy chicken, cheddar, olives & bell peppers.",
            category = "EXTREME PIZZAS",
            pricePkr = 1650L,
            imageKey = "extreme_pizza",
            badgeText = "LOADED",
            available = true,
            sortOrder = 2L
        ),
        MenuItem(
            id = "seed_hot_wings",
            name = "Hot Wings (8 pcs)",
            description = "Eight fiery marinated crispy wings served with cooling garlic dip.",
            category = "HOT WINGS",
            pricePkr = 580L,
            imageKey = "hot_wings",
            badgeText = "SPICY",
            available = true,
            sortOrder = 3L
        ),
        MenuItem(
            id = "seed_signature_sandwich",
            name = "Signature Sandwich",
            description = "Triple-layer grilled & crispy chicken club on toasted artisan brioche.",
            category = "SANDWICHES",
            pricePkr = 520L,
            imageKey = "signature_sandwich",
            badgeText = "CHEF'S PICK",
            available = true,
            sortOrder = 4L
        ),
        MenuItem(
            id = "seed_pickled_crunch",
            name = "Get Pickle'd Crunch",
            description = "Tangy crunchy dill pickles, crispy chicken & smoky ranch glaze.",
            category = "GET PICKLE'D",
            pricePkr = 720L,
            imageKey = "pickled_crunch",
            badgeText = "NEW",
            available = true,
            sortOrder = 5L
        ),
        MenuItem(
            id = "seed_everyday_value",
            name = "Everyday Value Box",
            description = "Crispy Zinger Burger + 2 Hot Wings + Masala Fries + chilled drink.",
            category = "EVERYDAY VALUE",
            pricePkr = 890L,
            imageKey = "everyday_value",
            badgeText = "VALUE DEAL",
            available = true,
            sortOrder = 6L
        ),
        MenuItem(
            id = "seed_mighty_zinger",
            name = "Mighty Double Zinger",
            description = "Two crunchy fillets stacked with double cheese and Lahori pepper sauce.",
            category = "SIGNATURE BURGERS",
            pricePkr = 850L,
            imageKey = "zinger_burger",
            badgeText = "XL CRUNCH",
            available = true,
            sortOrder = 7L
        ),
        MenuItem(
            id = "seed_smoky_wings",
            name = "Smoky Tikka Wings (8 pcs)",
            description = "Char-kissed spicy Lahori masala wings glazed in tangy tamarind BBQ.",
            category = "HOT WINGS",
            pricePkr = 620L,
            imageKey = "hot_wings",
            badgeText = "HOT",
            available = true,
            sortOrder = 8L
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
