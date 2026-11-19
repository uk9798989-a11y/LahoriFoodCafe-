package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import com.google.firebase.storage.storageMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object MenuImageStorageHelper {
    private const val TAG = "MenuImageStorage"
    private const val STORAGE_MAX_DIMENSION_PX = 1080
    private const val STORAGE_JPEG_QUALITY = 82
    private const val STORAGE_UPLOAD_TIMEOUT_MS = 6000L

    // Compact cloud JPEG stored directly in Firestore when Storage is not enabled
    private const val CLOUD_INLINE_MAX_BASE64_CHARS = 190_000
    const val MAX_STORED_IMAGE_CHARS = 220_000

    private val decodedBitmapCache = object : LruCache<Int, ImageBitmap>(48) {}

    fun isDataImageUri(raw: String): Boolean {
        val trimmed = raw.trim()
        return trimmed.startsWith("data:image/", ignoreCase = true) &&
            trimmed.contains(";base64,", ignoreCase = true)
    }

    fun isValidStoredImageRef(raw: String): Boolean {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return true
        if (trimmed.length > MAX_STORED_IMAGE_CHARS) return false
        if (isDataImageUri(trimmed)) return true
        val lower = trimmed.lowercase()
        return lower.startsWith("https://") || lower.startsWith("http://")
    }

    fun decodeDataImageBitmap(dataUri: String): ImageBitmap? {
        if (!isDataImageUri(dataUri)) return null
        val cacheKey = dataUri.hashCode() * 31 + dataUri.length
        synchronized(decodedBitmapCache) {
            decodedBitmapCache.get(cacheKey)?.let { return it }
        }
        return try {
            val commaIndex = dataUri.indexOf(',')
            if (commaIndex <= 0 || commaIndex >= dataUri.lastIndex) return null
            val b64 = dataUri.substring(commaIndex + 1)
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            if (bytes.isEmpty()) return null
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val imageBitmap = bitmap.asImageBitmap()
            synchronized(decodedBitmapCache) {
                decodedBitmapCache.put(cacheKey, imageBitmap)
            }
            imageBitmap
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode cloud photo data URI", e)
            null
        }
    }

    /**
     * Uploads a menu item photo to the cloud:
     * 1. Attempts Firebase Storage first (full quality).
     * 2. If Firebase Storage is not enabled or denies access, seamlessly compresses & encodes
     *    a crisp JPEG cloud photo stored directly in Cloud Firestore so all customers see it live.
     */
    suspend fun uploadMenuItemImage(
        context: Context,
        itemId: String,
        imageUri: Uri
    ): Result<String> {
        val appContext = context.applicationContext
        val safeItemId = itemId
            .trim()
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .ifBlank { "item_${System.currentTimeMillis()}" }

        val storageBytes = withContext(Dispatchers.IO) {
            compressImageUriToJpeg(
                context = appContext,
                uri = imageUri,
                maxDimensionPx = STORAGE_MAX_DIMENSION_PX,
                jpegQuality = STORAGE_JPEG_QUALITY
            )
        } ?: return Result.failure(
            IllegalArgumentException("Could not read the selected photo. Please choose another image from your gallery.")
        )

        // Try Firebase Storage first with a short timeout
        try {
            val downloadUrl = withTimeout(STORAGE_UPLOAD_TIMEOUT_MS) {
                val fileName = "photo_${System.currentTimeMillis()}.jpg"
                val storageRef = Firebase.storage.reference
                    .child("menu-items")
                    .child(safeItemId)
                    .child(fileName)
                val metadata = storageMetadata {
                    contentType = "image/jpeg"
                    setCustomMetadata("menuItemId", safeItemId)
                }
                storageRef.putBytes(storageBytes, metadata).await()
                storageRef.downloadUrl.await().toString()
            }
            if (downloadUrl.isNotBlank()) {
                return Result.success(downloadUrl)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.i(TAG, "Firebase Storage unavailable (${e.message}); storing compressed cloud photo in Firestore.")
        }

        // Seamless Cloud Firestore photo fallback (works 100% reliably & syncs live to every customer)
        val cloudDataUri = withContext(Dispatchers.IO) {
            buildCompactCloudDataUri(appContext, imageUri, startMaxDim = 680)
        } ?: return Result.failure(
            IllegalStateException("Could not process this photo for cloud upload. Please try another image.")
        )

        return Result.success(cloudDataUri)
    }

    /**
     * Uploads a branch seasonal banner photo to the cloud with the same seamless Firestore fallback.
     */
    suspend fun uploadBranchBannerImage(
        context: Context,
        branchId: String,
        bannerId: String,
        imageUri: Uri
    ): Result<String> {
        val appContext = context.applicationContext
        val safeBranchId = branchId
            .trim()
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .ifBlank { "branch_1" }
        val safeBannerId = bannerId
            .trim()
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .ifBlank { "banner_${System.currentTimeMillis()}" }

        val storageBytes = withContext(Dispatchers.IO) {
            compressImageUriToJpeg(
                context = appContext,
                uri = imageUri,
                maxDimensionPx = STORAGE_MAX_DIMENSION_PX,
                jpegQuality = STORAGE_JPEG_QUALITY
            )
        } ?: return Result.failure(
            IllegalArgumentException("Could not read the selected banner photo. Please choose another image.")
        )

        try {
            val downloadUrl = withTimeout(STORAGE_UPLOAD_TIMEOUT_MS) {
                val fileName = "banner_${System.currentTimeMillis()}.jpg"
                val storageRef = Firebase.storage.reference
                    .child("branch-banners")
                    .child(safeBranchId)
                    .child(safeBannerId)
                    .child(fileName)
                val metadata = storageMetadata {
                    contentType = "image/jpeg"
                    setCustomMetadata("branchId", safeBranchId)
                    setCustomMetadata("bannerId", safeBannerId)
                }
                storageRef.putBytes(storageBytes, metadata).await()
                storageRef.downloadUrl.await().toString()
            }
            if (downloadUrl.isNotBlank()) {
                return Result.success(downloadUrl)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.i(TAG, "Firebase Storage unavailable for banner (${e.message}); storing compressed cloud banner in Firestore.")
        }

        val cloudDataUri = withContext(Dispatchers.IO) {
            buildCompactCloudDataUri(appContext, imageUri, startMaxDim = 760)
        } ?: return Result.failure(
            IllegalStateException("Could not process this banner photo for cloud upload. Please try another image.")
        )

        return Result.success(cloudDataUri)
    }

    private fun buildCompactCloudDataUri(
        context: Context,
        uri: Uri,
        startMaxDim: Int
    ): String? {
        val attempts = listOf(
            startMaxDim to 78,
            (startMaxDim * 0.85f).roundToInt() to 72,
            540 to 66,
            440 to 60,
            360 to 55
        )
        for ((maxDim, quality) in attempts) {
            val bytes = compressImageUriToJpeg(context, uri, maxDim, quality) ?: continue
            val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val dataUri = "data:image/jpeg;base64,$encoded"
            if (dataUri.length <= CLOUD_INLINE_MAX_BASE64_CHARS) {
                return dataUri
            }
        }
        return null
    }

    private fun compressImageUriToJpeg(
        context: Context,
        uri: Uri,
        maxDimensionPx: Int,
        jpegQuality: Int
    ): ByteArray? {
        val bitmap = decodeOrientedScaledBitmap(context, uri, maxDimensionPx) ?: return null
        return try {
            ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out)
                out.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeOrientedScaledBitmap(
        context: Context,
        uri: Uri,
        maxDimensionPx: Int
    ): Bitmap? {
        val resolver = context.contentResolver

        // Android 9+ ImageDecoder automatically respects EXIF camera orientation and scales smoothly
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val source = ImageDecoder.createSource(resolver, uri)
                return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val srcW = info.size.width
                    val srcH = info.size.height
                    val maxSide = max(srcW, srcH)
                    if (maxSide > maxDimensionPx && srcW > 0 && srcH > 0) {
                        val scale = maxDimensionPx.toFloat() / maxSide.toFloat()
                        val targetW = (srcW * scale).roundToInt().coerceAtLeast(1)
                        val targetH = (srcH * scale).roundToInt().coerceAtLeast(1)
                        decoder.setTargetSize(targetW, targetH)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "ImageDecoder fallback to BitmapFactory: ${e.message}")
            }
        }

        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        } ?: return null

        val srcWidth = boundsOptions.outWidth
        val srcHeight = boundsOptions.outHeight
        if (srcWidth <= 0 || srcHeight <= 0) {
            return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }

        var sampleSize = 1
        while ((srcWidth / sampleSize) > maxDimensionPx * 2 ||
            (srcHeight / sampleSize) > maxDimensionPx * 2
        ) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: return null

        val maxSide = max(decoded.width, decoded.height)
        return if (maxSide > maxDimensionPx) {
            val scale = maxDimensionPx.toFloat() / maxSide.toFloat()
            val targetW = (decoded.width * scale).roundToInt().coerceAtLeast(1)
            val targetH = (decoded.height * scale).roundToInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(decoded, targetW, targetH, true).also { scaled ->
                if (scaled !== decoded) decoded.recycle()
            }
        } else {
            decoded
        }
    }
}
