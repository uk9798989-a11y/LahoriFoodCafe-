package com.example

import com.example.model.CafeOrder
import com.example.model.OrderType
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
}
