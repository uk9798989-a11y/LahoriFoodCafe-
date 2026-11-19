package com.example

import com.example.data.MenuImageStorageHelper
import com.example.model.MenuItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuImageValidationTest {

    @Test
    fun menuItem_defaultsToEmptyImageUrlForArtworkFallback() {
        val item = MenuItem(
            id = "lfc_pasta_lahori_special",
            name = "Lahori Special Pasta",
            category = "PASTA'S",
            pricePkr = 500L,
            imageKey = "pasta_bowl"
        )
        assertEquals("", item.imageUrl)
        assertEquals("pasta_bowl", item.imageKey)
    }

    @Test
    fun isValidRemoteImageUrl_acceptsEmptyAndHttpOrHttpsUrls() {
        assertTrue(MenuImageStorageHelper.isValidRemoteImageUrl(""))
        assertTrue(MenuImageStorageHelper.isValidRemoteImageUrl("   "))
        assertTrue(
            MenuImageStorageHelper.isValidRemoteImageUrl(
                "https://firebasestorage.googleapis.com/v0/b/gen-lang-client-0251605363.firebasestorage.app/o/menu-items%2Fzinger%2Fphoto.jpg?alt=media"
            )
        )
        assertTrue(MenuImageStorageHelper.isValidRemoteImageUrl("http://cdn.example.com/zinger.jpg"))
    }

    @Test
    fun isValidRemoteImageUrl_rejectsInvalidSchemesAndOversizedUrls() {
        assertFalse(MenuImageStorageHelper.isValidRemoteImageUrl("not-a-url"))
        assertFalse(MenuImageStorageHelper.isValidRemoteImageUrl("ftp://example.com/burger.jpg"))
        assertFalse(MenuImageStorageHelper.isValidRemoteImageUrl("javascript:alert(1)"))
        assertFalse(MenuImageStorageHelper.isValidRemoteImageUrl("file:///sdcard/photo.jpg"))
        val oversized = "https://example.com/" + "a".repeat(1005)
        assertFalse(MenuImageStorageHelper.isValidRemoteImageUrl(oversized))
    }
}
