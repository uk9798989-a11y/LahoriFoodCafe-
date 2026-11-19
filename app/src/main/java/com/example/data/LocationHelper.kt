package com.example.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.model.CafeBranch
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

data class DeviceLocationResult(
    val latitude: Double,
    val longitude: Double,
    val formattedAddress: String,
    val areaLabel: String,
    val cityLabel: String
)

object LocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocationWithAddress(context: Context): Result<DeviceLocationResult> {
        if (!hasLocationPermission(context)) {
            return Result.failure(SecurityException("Location permission not granted"))
        }
        return try {
            val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)
            val cts = CancellationTokenSource()
            val location: Location? = withTimeoutOrNull(8500L) {
                try {
                    client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).await()
                } catch (_: Exception) {
                    null
                }
            } ?: try {
                cts.cancel()
                client.lastLocation.await()
            } catch (_: Exception) {
                null
            }

            if (location == null) {
                return Result.failure(
                    IllegalStateException("Could not get GPS fix. Make sure device location is turned on or choose your area below.")
                )
            }

            val resolved = reverseGeocode(context.applicationContext, location.latitude, location.longitude)
            Result.success(resolved)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun reverseGeocode(
        context: Context,
        latitude: Double,
        longitude: Double
    ): DeviceLocationResult = withContext(Dispatchers.IO) {
        val fallbackAddr = "%.4f, %.4f".format(Locale.US, latitude, longitude)
        if (!Geocoder.isPresent()) {
            return@withContext DeviceLocationResult(
                latitude = latitude,
                longitude = longitude,
                formattedAddress = fallbackAddr,
                areaLabel = "Current GPS Location",
                cityLabel = ""
            )
        }

        val addresses: List<Address> = try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                withTimeoutOrNull(3500L) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(results: MutableList<Address>) {
                                if (cont.isActive) cont.resume(results)
                            }

                            override fun onError(errorMessage: String?) {
                                if (cont.isActive) cont.resume(emptyList())
                            }
                        })
                    }
                } ?: emptyList()
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1).orEmpty()
            }
        } catch (_: Exception) {
            emptyList()
        }

        val first = addresses.firstOrNull()
        if (first == null) {
            DeviceLocationResult(
                latitude = latitude,
                longitude = longitude,
                formattedAddress = fallbackAddr,
                areaLabel = "Current GPS Location",
                cityLabel = ""
            )
        } else {
            val subLocality = first.subLocality?.trim().orEmpty()
            val thoroughfare = listOfNotNull(first.subThoroughfare, first.thoroughfare)
                .joinToString(" ")
                .trim()
            val locality = first.locality?.trim().orEmpty()
            val subAdmin = first.subAdminArea?.trim().orEmpty()
            val fullLine = first.getAddressLine(0)?.trim().orEmpty()

            val area = subLocality.ifBlank {
                thoroughfare.ifBlank {
                    locality.ifBlank { subAdmin.ifBlank { "Detected Area" } }
                }
            }
            val city = locality.ifBlank { subAdmin }
            val formatted = fullLine.ifBlank {
                listOf(thoroughfare, subLocality, locality, subAdmin)
                    .filter { it.isNotBlank() }
                    .distinct()
                    .joinToString(", ")
                    .ifBlank { fallbackAddr }
            }

            DeviceLocationResult(
                latitude = latitude,
                longitude = longitude,
                formattedAddress = formatted,
                areaLabel = area,
                cityLabel = city
            )
        }
    }

    fun openBranchInMaps(context: Context, branch: CafeBranch) {
        val cleanUrl = branch.mapUrl.trim()
        val uri = when {
            cleanUrl.startsWith("https://", ignoreCase = true) ||
                cleanUrl.startsWith("http://", ignoreCase = true) -> Uri.parse(cleanUrl)
            branch.hasCoordinates() -> {
                val label = Uri.encode(branch.name.ifBlank { branch.displayShortLabel() })
                Uri.parse("geo:${branch.latitude},${branch.longitude}?q=${branch.latitude},${branch.longitude}($label)")
            }
            else -> {
                val query = Uri.encode("${branch.name} ${branch.shortAddress}")
                Uri.parse("geo:0,0?q=$query")
            }
        }
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            try {
                if (branch.hasCoordinates()) {
                    val webUri = Uri.parse(
                        "https://www.google.com/maps/search/?api=1&query=${branch.latitude},${branch.longitude}"
                    )
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, webUri).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                } else {
                    Toast.makeText(context, "No Maps app found on device", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(context, "Unable to open map directions", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openCustomerLocationInMaps(
        context: Context,
        latitude: Double,
        longitude: Double,
        address: String,
        customerName: String = ""
    ) {
        val hasCoords = (latitude != 0.0 || longitude != 0.0) &&
            latitude in -90.0..90.0 &&
            longitude in -180.0..180.0

        val geoUri = if (hasCoords) {
            val label = Uri.encode(
                customerName.ifBlank { "Customer Delivery" }
            )
            Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($label)")
        } else if (address.isNotBlank()) {
            Uri.parse("geo:0,0?q=${Uri.encode(address)}")
        } else {
            Toast.makeText(context, "No delivery location available for this order", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            try {
                val webUri = if (hasCoords) {
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
                } else {
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(address)}")
                }
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, webUri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            } catch (_: Exception) {
                Toast.makeText(context, "Unable to open Google Maps", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun dialCustomerPhone(context: Context, phone: String) {
        val cleaned = phone.trim()
        if (cleaned.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(cleaned)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to open phone dialer", Toast.LENGTH_SHORT).show()
        }
    }
}
