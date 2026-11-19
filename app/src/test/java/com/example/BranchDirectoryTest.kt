package com.example

import com.example.model.BranchDirectory
import com.example.model.CafeBranch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun officialDefaultBranches_matchTmpAndZahirPirDetails() {
        assertEquals("Lahori Food Cafe TMP", BranchDirectory.branch1.displayShortLabel())
        assertEquals("https://share.google/P92yinaSqd7Dz0anJ", BranchDirectory.branch1.mapUrl)
        assertEquals("Lahori Food Cafe Zahir Pir", BranchDirectory.branch2.displayShortLabel())
        assertEquals("https://share.google/XrwRlyd2jYQs8wbzl", BranchDirectory.branch2.mapUrl)
    }

    @Test
    fun routeAddressToBranch_routesZahirPirAndTmpCorrectly() {
        val tmpRouted = BranchDirectory.routeAddressToBranch("Near Main KLP Road TMP")
        assertEquals("branch_1", tmpRouted.id)

        val zahirRouted = BranchDirectory.routeAddressToBranch("Zahir Pir M-5 Interchange Khanpur")
        assertEquals("branch_2", zahirRouted.id)
    }

    @Test
    fun nearestBranch_sortsTmpVsZahirPirByGpsCoordinates() {
        // Near Taranda Muhammad Panah
        val nearTmp = BranchDirectory.nearestBranch(29.105, 70.910)
        assertEquals("branch_1", nearTmp.id)

        // Near Zahir Pir
        val nearZahir = BranchDirectory.nearestBranch(28.815, 70.528)
        assertEquals("branch_2", nearZahir.id)

        val sorted = BranchDirectory.branchesSortedByDistance(28.815, 70.528)
        assertEquals("branch_2", sorted.first().first.id)
        assertTrue((sorted[0].second ?: 999.0) < (sorted[1].second ?: 0.0))
    }

    @Test
    fun routeAddressToBranch_supportsUnlimitedBranchesAndLongestAreaMatch() {
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
