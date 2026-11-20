package com.example

import com.example.data.PersistedManualAddress
import com.example.model.AdminDeliveryNotification
import com.example.model.CafeOrder
import com.example.model.MenuItem
import com.example.model.OrderType
import com.example.model.SavedAddress
import com.example.ui.SessionCartController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryLocationAndNotificationTest {

    @Test
    fun deliveryOrder_withValidCoordinates_reportsHasDeliveryLocationAndFormatsCoordinates() {
        val order = CafeOrder(
            id = "ord_gps_1",
            orderType = OrderType.DELIVERY.name,
            deliveryAddress = "Main KLP Road, Taranda Muhammad Panah",
            deliveryLatitude = 29.1003,
            deliveryLongitude = 70.9080
        )

        assertTrue(order.hasDeliveryLocation())
        assertEquals("29.10030, 70.90800", order.formattedCoordinates())
        assertEquals(
            "https://www.google.com/maps/search/?api=1&query=29.1003,70.908",
            order.deliveryGoogleMapsUrl()
        )
    }

    @Test
    fun deliveryOrder_withZeroOrOutOfRangeCoordinates_reportsNoGpsPin() {
        val noPinOrder = CafeOrder(
            id = "ord_no_gps",
            orderType = OrderType.DELIVERY.name,
            deliveryAddress = "House 12, Street 4",
            deliveryLatitude = 0.0,
            deliveryLongitude = 0.0
        )
        assertFalse(noPinOrder.hasDeliveryLocation())
        assertEquals("", noPinOrder.formattedCoordinates())

        val invalidLatOrder = CafeOrder(
            id = "ord_bad_lat",
            deliveryLatitude = 105.0,
            deliveryLongitude = 70.9080
        )
        assertFalse(invalidLatOrder.hasDeliveryLocation())

        val invalidLngOrder = CafeOrder(
            id = "ord_bad_lng",
            deliveryLatitude = 29.1003,
            deliveryLongitude = -195.0
        )
        assertFalse(invalidLngOrder.hasDeliveryLocation())
    }

    @Test
    fun sessionCart_applyDetectedGpsLocation_preservesDeliveryOrderTypeAndStoresCoordinates() {
        val controller = SessionCartController()
        controller.setOrderType(OrderType.DELIVERY)
        controller.applyDetectedGpsLocation(
            latitude = 28.8124,
            longitude = 70.5241,
            formattedAddress = "M-5 Interchange, Zahir Pir",
            areaLabel = "Zahir Pir",
            nearestBranchId = "branch_2",
            keepOrderType = true
        )

        val state = controller.state.value
        assertTrue(state.hasGpsFix)
        assertEquals(OrderType.DELIVERY, state.orderType)
        assertEquals("branch_2", state.selectedBranchId)
        assertEquals(28.8124, state.deviceLatitude ?: 0.0, 0.0001)
        assertEquals(70.5241, state.deviceLongitude ?: 0.0, 0.0001)
        assertEquals("M-5 Interchange, Zahir Pir", state.selectedAddressStreet)
        assertEquals("M-5 Interchange, Zahir Pir", state.detectedGpsAddress)
    }

    @Test
    fun manualAddress_defaultDeliveryMode_savesEditsAndCombinesOptionalLandmarkWithoutGps() {
        val controller = SessionCartController()
        assertEquals(OrderType.DELIVERY, controller.state.value.orderType)

        val added = controller.addGuestAddress(
            label = "Home",
            streetAddress = "House 14, Street 3, Model Town, Taranda Muhammad Panah",
            area = "",
            landmark = "Opposite Allied Bank"
        )
        assertEquals("branch_1", added.branchId)
        assertEquals(
            "House 14, Street 3, Model Town, Taranda Muhammad Panah (Landmark: Opposite Allied Bank)",
            controller.state.value.combinedManualAddress()
        )
        assertFalse(controller.state.value.hasGpsFix)

        val updated = controller.updateGuestAddress(
            addressId = added.id,
            label = "Office",
            streetAddress = "Shop 9, Main Bazaar, Zahir Pir",
            landmark = "Near M-5 Interchange"
        )
        assertEquals("branch_2", updated.branchId)
        assertEquals("Office", controller.state.value.selectedAddressLabel)
        assertEquals(
            "Shop 9, Main Bazaar, Zahir Pir (Landmark: Near M-5 Interchange)",
            updated.fullFormattedAddress()
        )

        // Optional GPS detection must NOT overwrite customer's manually typed address
        controller.applyDetectedGpsLocation(
            latitude = 28.8124,
            longitude = 70.5241,
            formattedAddress = "Detected Highway Point",
            areaLabel = "Zahir Pir",
            nearestBranchId = "branch_2",
            keepOrderType = true
        )
        assertEquals("Shop 9, Main Bazaar, Zahir Pir", controller.state.value.selectedAddressStreet)
    }

    @Test
    fun persistedManualAddress_formatsAddressAndLandmarkCorrectly() {
        val empty = PersistedManualAddress()
        assertFalse(empty.hasSavedAddress)
        assertEquals("", empty.combinedDeliveryAddress())

        val withLandmark = PersistedManualAddress(
            addressId = "addr_1",
            label = "Home",
            fullAddress = "House 22, Canal Road, TMP",
            landmark = "Near Jamia Masjid",
            branchId = "branch_1"
        )
        assertTrue(withLandmark.hasSavedAddress)
        assertEquals("House 22, Canal Road, TMP (Landmark: Near Jamia Masjid)", withLandmark.combinedDeliveryAddress())

        val savedAddress = SavedAddress(
            id = "addr_2",
            label = "Home",
            streetAddress = "House 5, Colony A, Zahir Pir",
            landmark = "Near City Hospital"
        )
        assertEquals("House 5, Colony A, Zahir Pir (Landmark: Near City Hospital)", savedAddress.fullFormattedAddress())
    }

    @Test
    fun freeDeliverySystem_deliveryChargesAlwaysZeroAndTotalMatchesSubtotalMinusPromo() {
        val controller = SessionCartController()
        val zinger = MenuItem(id = "m_zinger", name = "Zinger Burger", pricePkr = 650L)
        val pizza = MenuItem(id = "m_pizza", name = "Extreme Pizza", pricePkr = 1850L)

        controller.addToCart(zinger)
        controller.addToCart(pizza)

        val subtotal = controller.state.value.subtotalPkr
        val deliveryFee = 0L
        val totalWithoutPromo = subtotal + deliveryFee
        assertEquals(2500L, subtotal)
        assertEquals(2500L, totalWithoutPromo)

        controller.applyPromo("LAHORI20", 500L)
        val totalWithPromo = (controller.state.value.subtotalPkr + deliveryFee - controller.state.value.appliedDiscountPkr)
            .coerceAtLeast(0L)
        assertEquals(2000L, totalWithPromo)

        val order = CafeOrder(
            id = "ord_free_1",
            orderType = OrderType.DELIVERY.name,
            subtotalPkr = subtotal,
            deliveryFeePkr = 0L,
            discountPkr = 500L,
            totalPkr = totalWithPromo
        )
        assertEquals(0L, order.deliveryFeePkr)
        assertEquals(2000L, order.totalPkr)
    }

    @Test
    fun adminDeliveryNotification_defaultsUnreadAndContainsOrderMetadata() {
        val notification = AdminDeliveryNotification(
            id = "delivered_ord_99",
            orderId = "ord_99",
            orderNumber = "#ORD_99",
            branchId = "branch_1",
            branchName = "Lahori Food Cafe TMP",
            customerName = "Ali Raza",
            customerPhone = "0300-9876543",
            totalPkr = 1850L,
            deliveryAddress = "House 8, KLP Road, TMP",
            read = false
        )

        assertFalse(notification.read)
        assertEquals("#ORD_99", notification.orderNumber)
        assertEquals(1850L, notification.totalPkr)
        assertEquals("branch_1", notification.branchId)
    }
}

