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
    fun isValidStoredImageRef_acceptsEmptyHttpsAndCloudDataUris() {
        assertTrue(MenuImageStorageHelper.isValidStoredImageRef(""))
        assertTrue(MenuImageStorageHelper.isValidStoredImageRef("   "))
        assertTrue(
            MenuImageStorageHelper.isValidStoredImageRef(
                "https://firebasestorage.googleapis.com/v0/b/gen-lang-client-0251605363.firebasestorage.app/o/menu-items%2Fzinger%2Fphoto.jpg?alt=media"
            )
        )
        assertTrue(MenuImageStorageHelper.isValidStoredImageRef("http://cdn.example.com/zinger.jpg"))
        val dataUri = "data:image/jpeg;base64," + "A".repeat(4000)
        assertTrue(MenuImageStorageHelper.isDataImageUri(dataUri))
        assertTrue(MenuImageStorageHelper.isValidStoredImageRef(dataUri))
    }

    @Test
    fun isValidStoredImageRef_rejectsInvalidSchemesAndOversizedPayloads() {
        assertFalse(MenuImageStorageHelper.isValidStoredImageRef("not-a-url"))
        assertFalse(MenuImageStorageHelper.isValidStoredImageRef("ftp://example.com/burger.jpg"))
        assertFalse(MenuImageStorageHelper.isValidStoredImageRef("javascript:alert(1)"))
        assertFalse(MenuImageStorageHelper.isValidStoredImageRef("file:///sdcard/photo.jpg"))
        val oversized = "data:image/jpeg;base64," + "A".repeat(MenuImageStorageHelper.MAX_STORED_IMAGE_CHARS + 50)
        assertFalse(MenuImageStorageHelper.isValidStoredImageRef(oversized))
    }
}
