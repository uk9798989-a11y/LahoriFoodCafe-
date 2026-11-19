package com.example

import com.example.model.BranchBanner
import com.example.model.BranchDirectory
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
            branchId = BranchDirectory.BRANCH_1_ID,
            branchName = "Lahori Food Cafe — Taranda Muhammad Panah (TMP)",
            expiresAt = futureTs
        )

        assertFalse(tmpPromo.isGlobal())
        assertTrue(tmpPromo.appliesToBranch(BranchDirectory.BRANCH_1_ID))
        assertFalse(tmpPromo.appliesToBranch(BranchDirectory.BRANCH_2_ID))
        assertTrue(tmpPromo.isCurrentlyValidForBranch(BranchDirectory.BRANCH_1_ID, now))
        assertFalse(tmpPromo.isCurrentlyValidForBranch(BranchDirectory.BRANCH_2_ID, now))
        assertEquals(200L, tmpPromo.computeDiscount(1000L, now))

        val expiredPromo = tmpPromo.copy(expiresAt = pastTs)
        assertTrue(expiredPromo.isExpired(now))
        assertFalse(expiredPromo.isCurrentlyValidForBranch(BranchDirectory.BRANCH_1_ID, now))
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
        assertTrue(globalPromo.isCurrentlyValidForBranch(BranchDirectory.BRANCH_1_ID, now))
        assertTrue(globalPromo.isCurrentlyValidForBranch(BranchDirectory.BRANCH_2_ID, now))
    }

    @Test
    fun branchSeasonalBannerVisibilityIsStrictlyIsolatedByBranchAndExpiry() {
        val now = 1_750_000_000_000L
        val futureTs = Timestamp(Date(now + 3L * 24L * 60L * 60L * 1000L))
        val pastTs = Timestamp(Date(now - 10_000L))

        val zpBanner = BranchBanner(
            id = "banner_zp_eid",
            branchId = BranchDirectory.BRANCH_2_ID,
            branchName = "Lahori Food Cafe — Zahir Pir",
            title = "Zahir Pir Eid Feast",
            subtitle = "Special 25% off family platters",
            promoCode = "ZPEID25",
            badgeText = "EID SPECIAL",
            imageUrl = "https://example.com/eid.jpg",
            targetCategory = "PIZZA",
            active = true,
            expiresAt = futureTs
        )

        assertTrue(zpBanner.isVisibleForBranch(BranchDirectory.BRANCH_2_ID, now))
        assertFalse(zpBanner.isVisibleForBranch(BranchDirectory.BRANCH_1_ID, now))
        assertFalse(zpBanner.copy(active = false).isVisibleForBranch(BranchDirectory.BRANCH_2_ID, now))
        assertFalse(zpBanner.copy(expiresAt = pastTs).isVisibleForBranch(BranchDirectory.BRANCH_2_ID, now))
    }

    @Test
    fun switchingBranchClearsBranchExclusivePromoButPreservesGlobalPromo() {
        val cart = SessionCartController()
        cart.selectPickupBranch(BranchDirectory.BRANCH_1_ID)
        cart.addToCart(SeedMenuData.initialItems.first())

        // Apply Branch 1 exclusive promo
        cart.applyPromo(code = "TMP20", discountPkr = 200L, branchId = BranchDirectory.BRANCH_1_ID)
        assertEquals("TMP20", cart.state.value.appliedPromoLabel)
        assertEquals(200L, cart.state.value.appliedDiscountPkr)
        assertEquals(BranchDirectory.BRANCH_1_ID, cart.state.value.appliedPromoBranchId)

        // Switching to Branch 2 must immediately clear the Branch 1 promo
        cart.selectPickupBranch(BranchDirectory.BRANCH_2_ID)
        assertEquals("", cart.state.value.appliedPromoLabel)
        assertEquals(0L, cart.state.value.appliedDiscountPkr)
        assertEquals("", cart.state.value.appliedPromoBranchId)

        // Apply global promo (branchId = "")
        cart.applyPromo(code = "LAHORI20", discountPkr = 150L, branchId = "")
        cart.selectPickupBranch(BranchDirectory.BRANCH_1_ID)
        assertEquals("LAHORI20", cart.state.value.appliedPromoLabel)
        assertEquals(150L, cart.state.value.appliedDiscountPkr)
    }
}
