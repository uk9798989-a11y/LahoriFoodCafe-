package com.example

import com.example.model.CafeOrder
import com.example.ui.screens.calculateLoyaltyPoints
import org.junit.Assert.assertEquals
import org.junit.Test

class LoyaltyRewardsTest {

    @Test
    fun newCustomer_startsWithZeroLoyaltyPoints() {
        assertEquals(0, calculateLoyaltyPoints(emptyList()))
    }

    @Test
    fun customerEarnsFivePointsPerHundredRupeesSpent() {
        val orders = listOf(
            CafeOrder(id = "o1", totalPkr = 450L),
            CafeOrder(id = "o2", totalPkr = 1550L)
        )
        // Total spent = 2000 PKR -> (2000 / 100) * 5 = 100 pts
        assertEquals(100, calculateLoyaltyPoints(orders))
    }
}
