package com.example

import com.example.model.SeedMenuData
import com.example.ui.components.resolveMenuDrawableRes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfMenuCatalogTest {

    @Test
    fun allSeedItemsHaveValidFirestoreFieldsAndRealDrawablePhotos() {
        val items = SeedMenuData.initialItems
        assertTrue("Expected full Lahori Food Cafe PDF menu (>= 80 items)", items.size >= 80)

        val seenIds = mutableSetOf<String>()
        for (item in items) {
            assertTrue("Duplicate item id: ${item.id}", seenIds.add(item.id))
            assertTrue("Name out of bounds: ${item.name}", item.name.length in 1..80)
            assertTrue("Description too long: ${item.name}", item.description.length <= 300)
            assertTrue("Category out of bounds: ${item.category}", item.category.length in 1..40)
            assertTrue("Price out of bounds: ${item.name}", item.pricePkr in 1..100000)
            assertTrue("imageKey out of bounds: ${item.imageKey}", item.imageKey.length in 1..50)
            assertTrue("badgeText too long: ${item.badgeText}", item.badgeText.length <= 30)

            val resId = resolveMenuDrawableRes(item.imageKey, item.id)
            assertNotNull("Missing real PDF photo drawable for ${item.name} (${item.imageKey})", resId)
        }
    }

    @Test
    fun allPromoBannersHaveRealDrawablePhotos() {
        for (banner in SeedMenuData.promoBanners) {
            val resId = resolveMenuDrawableRes(banner.imageKey)
            assertNotNull("Missing drawable for promo banner ${banner.id}", resId)
        }
    }

    @Test
    fun newAndUpdatedPdfItemsMatchOfficialPricesAndSizes() {
        val byId = SeedMenuData.initialItems.associateBy { it.id }

        assertEquals(3990L, byId["seed_lfc_one_meter_pizza"]?.pricePkr)
        assertEquals(1450L, byId["seed_lfc_ext_lazania"]?.pricePkr)
        assertEquals(1450L, byId["seed_lfc_ext_premium_pan"]?.pricePkr)
        assertEquals(550L, byId["seed_lfc_ext_pizza_stick"]?.pricePkr)
        assertEquals(650L, byId["seed_lfc_burger_beef"]?.pricePkr)
        assertEquals(200L, byId["seed_lfc_burger_chicken"]?.pricePkr)
        assertEquals(70L, byId["seed_lfc_burger_extra_cheese"]?.pricePkr)
        assertEquals(490L, byId["seed_lfc_wrap_grill"]?.pricePkr)
        assertEquals(350L, byId["seed_lfc_wrap_malai_boti"]?.pricePkr)
        assertEquals(300L, byId["seed_lfc_wrap_zinger_shawarma"]?.pricePkr)
        assertEquals(450L, byId["seed_lfc_fries_pizza_loaded"]?.pricePkr)
        assertEquals(500L, byId["seed_lfc_fries_crispy_loaded"]?.pricePkr)
        assertEquals(50L, byId["seed_lfc_dip_bonfire"]?.pricePkr)

        // Check updated deals from PDF Pages 9-10
        assertEquals(400L, byId["seed_lfc_deal_1"]?.pricePkr)
        assertEquals(1440L, byId["seed_lfc_deal_3"]?.pricePkr)
        assertEquals(1930L, byId["seed_lfc_deal_4"]?.pricePkr)
        assertEquals(2560L, byId["seed_lfc_deal_7"]?.pricePkr)
        assertEquals(2190L, byId["seed_lfc_deal_10"]?.pricePkr)
        assertEquals(720L, byId["seed_lfc_deal_11"]?.pricePkr)
        assertEquals(1060L, byId["seed_lfc_deal_17"]?.pricePkr)
        assertEquals(1260L, byId["seed_lfc_deal_19"]?.pricePkr)
        assertEquals(7070L, byId["seed_lfc_deal_birthday"]?.pricePkr)

        // Check updated size tiers
        val lahoriPizzaSizes = SeedMenuData.sizeOptionsFor(byId.getValue("seed_lfc_trad_lahori_special"))
        assertEquals(listOf(590L, 1050L, 1590L, 2500L), lahoriPizzaSizes.map { it.pricePkr })

        val crunchyCheeseSizes = SeedMenuData.sizeOptionsFor(byId.getValue("seed_lfc_trad_crispy"))
        assertEquals(listOf(600L, 1290L, 1590L, 2500L), crunchyCheeseSizes.map { it.pricePkr })

        val flamingPastaSizes = SeedMenuData.sizeOptionsFor(byId.getValue("seed_lfc_pasta_flaming"))
        assertEquals(listOf(450L, 750L), flamingPastaSizes.map { it.pricePkr })

        val pizzaLoadedFriesSizes = SeedMenuData.sizeOptionsFor(byId.getValue("seed_lfc_fries_pizza_loaded"))
        assertEquals(listOf(450L, 750L), pizzaLoadedFriesSizes.map { it.pricePkr })

        val crispyLoadedFriesSizes = SeedMenuData.sizeOptionsFor(byId.getValue("seed_lfc_fries_crispy_loaded"))
        assertEquals(listOf(500L, 800L), crispyLoadedFriesSizes.map { it.pricePkr })
    }
}
