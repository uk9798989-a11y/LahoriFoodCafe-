package com.example

import com.example.model.BranchBanner
import com.example.model.Promotion
import com.example.model.SeedMenuData
import com.example.ui.SessionCartController
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class BranchPromoIsolationTest {

    private val branch1Id = "branch_1"
    private val branch2Id = "branch_2"

    @Test
    fun branchExclusivePromotionOnlyAppliesToTargetBranchAndRespectsExpiry() {
        val now = 1_750_000_000_000L
        val futureTs = Timestamp(Date(now + 24L * 60L * 60L * 1000L))
        val pastTs = Timestamp(Date(now - 60L * 1000L))

        val tmpPromo = Promotion(
            id = "branch_1_TMP20",
            code = "TMP20",
            discountType = "PERCENT",
            discountValue = 20L,
            minOrderPkr = 500L,
            active = true,
            branchId = branch1Id,
            branchName = "Lahori Food Cafe — Taranda Muhammad Panah (TMP)",
            expiresAt = futureTs
        )

        assertFalse(tmpPromo.isGlobal())
        assertTrue(tmpPromo.appliesToBranch(branch1Id))
        assertFalse(tmpPromo.appliesToBranch(branch2Id))
        assertTrue(tmpPromo.isCurrentlyValidForBranch(branch1Id, now))
        assertFalse(tmpPromo.isCurrentlyValidForBranch(branch2Id, now))
        assertEquals(200L, tmpPromo.computeDiscount(1000L, now))

        val expiredPromo = tmpPromo.copy(expiresAt = pastTs)
        assertTrue(expiredPromo.isExpired(now))
        assertFalse(expiredPromo.isCurrentlyValidForBranch(branch1Id, now))
        assertEquals(0L, expiredPromo.computeDiscount(1000L, now))

        val globalPromo = Promotion(
            id = "LAHORI20",
            code = "LAHORI20",
            discountType = "PERCENT",
            discountValue = 20L,
            minOrderPkr = 500L,
            active = true,
            branchId = ""
        )
        assertTrue(globalPromo.isGlobal())
        assertTrue(globalPromo.isCurrentlyValidForBranch(branch1Id, now))
        assertTrue(globalPromo.isCurrentlyValidForBranch(branch2Id, now))
    }

    @Test
    fun branchSeasonalBannerVisibilityIsStrictlyIsolatedByBranchAndExpiry() {
        val now = 1_750_000_000_000L
        val futureTs = Timestamp(Date(now + 3L * 24L * 60L * 60L * 1000L))
        val pastTs = Timestamp(Date(now - 10_000L))

        val zpBanner = BranchBanner(
            id = "banner_zp_eid",
            branchId = branch2Id,
            branchName = "Lahori Food Cafe — Zahir Pir",
            title = "Zahir Pir Eid Feast",
            subtitle = "Special 25% off family platters",
            promoCode = "ZPEID25",
            badgeText = "EID SPECIAL",
            imageUrl = "data:image/jpeg;base64,AAAA",
            targetCategory = "PIZZA",
            active = true,
            expiresAt = futureTs
        )

        assertTrue(zpBanner.isVisibleForBranch(branch2Id, now))
        assertFalse(zpBanner.isVisibleForBranch(branch1Id, now))
        assertFalse(zpBanner.copy(active = false).isVisibleForBranch(branch2Id, now))
        assertFalse(zpBanner.copy(expiresAt = pastTs).isVisibleForBranch(branch2Id, now))
    }

    @Test
    fun switchingBranchClearsBranchExclusivePromoButPreservesGlobalPromo() {
        val cart = SessionCartController()
        cart.selectPickupBranch(branch1Id)
        cart.addToCart(SeedMenuData.initialItems.first())

        // Apply Branch 1 exclusive promo
        cart.applyPromo(label = "TMP20", discountPkr = 200L, branchId = branch1Id)
        assertEquals("TMP20", cart.state.value.appliedPromoLabel)
        assertEquals(200L, cart.state.value.appliedDiscountPkr)
        assertEquals(branch1Id, cart.state.value.appliedPromoBranchId)

        // Switching to Branch 2 must immediately clear the Branch 1 promo
        cart.selectPickupBranch(branch2Id)
        assertEquals("", cart.state.value.appliedPromoLabel)
        assertEquals(0L, cart.state.value.appliedDiscountPkr)
        assertEquals("", cart.state.value.appliedPromoBranchId)

        // Apply global promo (branchId = "")
        cart.applyPromo(label = "LAHORI20", discountPkr = 150L, branchId = "")
        cart.selectPickupBranch(branch1Id)
        assertEquals("LAHORI20", cart.state.value.appliedPromoLabel)
        assertEquals(150L, cart.state.value.appliedDiscountPkr)
    }
}
