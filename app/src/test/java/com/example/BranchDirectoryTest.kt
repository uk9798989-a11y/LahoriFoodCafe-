package com.example

import com.example.model.BranchDirectory
import com.example.model.CafeBranch
import org.junit.Assert.assertEquals
import org.junit.Test

class BranchDirectoryTest {

    private val twelveBranches = (1..12).map { index ->
        when (index) {
            1 -> BranchDirectory.branch1
            2 -> BranchDirectory.branch2
            10 -> CafeBranch(
                id = "branch_10",
                name = "Lahori Food Cafe — Johar Town Expo",
                shortLabel = "Branch 10 (Johar Expo)",
                shortAddress = "Expo Center Road, Johar Town, Lahore",
                areas = listOf("Johar Town Expo", "Emporium", "Shaukat Khanum"),
                active = true,
                sortOrder = 10L
            )
            else -> CafeBranch(
                id = "branch_$index",
                name = "Lahori Food Cafe — Zone $index",
                shortLabel = "Branch $index (Zone $index)",
                shortAddress = "Block $index, Lahore",
                areas = listOf("Zone $index Area"),
                active = index != 7, // pause branch_7
                sortOrder = index.toLong()
            )
        }
    }

    @Test
    fun routeAddressToBranch_supportsUnlimitedBranchesAndLongestAreaMatch() {
        // "Johar Town Expo" is more specific than "Johar Town" (on branch_2)
        val routed = BranchDirectory.routeAddressToBranch(
            areaOrAddress = "House 14, Johar Town Expo Block H",
            branches = twelveBranches
        )
        assertEquals("branch_10", routed.id)
    }

    @Test
    fun routeAddressToBranch_ignoresPausedBranchesWhenActiveBranchesExist() {
        val routed = BranchDirectory.routeAddressToBranch(
            areaOrAddress = "Zone 7 Area Street 2",
            branches = twelveBranches
        )
        // branch_7 is paused so it should not be returned; deterministic active fallback picks branch_1
        assertEquals("branch_1", routed.id)
    }

    @Test
    fun byIdAndShortPickerLabel_resolveDynamicBranch10() {
        val branch10 = BranchDirectory.byId("branch_10", twelveBranches)
        assertEquals("Lahori Food Cafe — Johar Town Expo", branch10.name)
        assertEquals(
            "Branch 10 (Johar Expo)",
            BranchDirectory.shortPickerLabel("branch_10", twelveBranches)
        )
    }
}
