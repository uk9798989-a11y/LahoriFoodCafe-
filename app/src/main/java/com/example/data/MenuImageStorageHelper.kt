package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.storage
import com.google.firebase.storage.storageMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object MenuImageStorageHelper {
    private const val TAG = "MenuImageStorage"
    private const val MAX_IMAGE_DIMENSION_PX = 1280
    private const val JPEG_QUALITY = 82
    private const val MAX_URL_LENGTH = 1000

    fun isValidRemoteImageUrl(raw: String): Boolean {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return true
        if (trimmed.length > MAX_URL_LENGTH) return false
        val lower = trimmed.lowercase()
        return lower.startsWith("https://") || lower.startsWith("http://")
    }

    suspend fun uploadMenuItemImage(
        context: Context,
        itemId: String,
        imageUri: Uri
    ): Result<String> {
        return try {
            val safeItemId = itemId
                .trim()
                .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                .ifBlank { "item_${System.currentTimeMillis()}" }

            val compressedBytes = withContext(Dispatchers.IO) {
                compressImageUriToJpeg(context.applicationContext, imageUri)
            } ?: return Result.failure(
                IllegalArgumentException("Could not read the selected image. Please pick another photo.")
            )

            val fileName = "photo_${System.currentTimeMillis()}.jpg"
            val storageRef = Firebase.storage.reference
                .child("menu-items")
                .child(safeItemId)
                .child(fileName)

            val metadata = storageMetadata {
                contentType = "image/jpeg"
                setCustomMetadata("menuItemId", safeItemId)
            }

            storageRef.putBytes(compressedBytes, metadata).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: CancellationException) {
            throw e
        } catch (e: StorageException) {
            Log.e(TAG, "Firebase Storage upload failed (code=${e.errorCode})", e)
            val friendly = when (e.errorCode) {
                StorageException.ERROR_NOT_AUTHORIZED,
                StorageException.ERROR_NOT_AUTHENTICATED ->
                    "Storage upload denied. Ensure Firebase Storage rules allow Super Admin uploads, or paste an HTTPS image URL."
                StorageException.ERROR_BUCKET_NOT_FOUND,
                StorageException.ERROR_PROJECT_NOT_FOUND,
                StorageException.ERROR_OBJECT_NOT_FOUND ->
                    "Firebase Storage is not enabled yet in Firebase Console. Enable Storage or paste an HTTPS image URL."
                else ->
                    "Image upload failed: ${e.localizedMessage ?: "check internet connection or paste an image URL."}"
            }
            Result.failure(IllegalStateException(friendly, e))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error uploading menu item photo", e)
            Result.failure(
                IllegalStateException(
                    "Image upload failed: ${e.localizedMessage ?: "please try again or paste an HTTPS image URL."}",
                    e
                )
            )
        }
    }

    suspend fun uploadBranchBannerImage(
        context: Context,
        branchId: String,
        bannerId: String,
        imageUri: Uri
    ): Result<String> {
        return try {
            val safeBranchId = branchId
                .trim()
                .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                .ifBlank { "branch_1" }
            val safeBannerId = bannerId
                .trim()
                .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                .ifBlank { "banner_${System.currentTimeMillis()}" }

            val compressedBytes = withContext(Dispatchers.IO) {
                compressImageUriToJpeg(context.applicationContext, imageUri)
            } ?: return Result.failure(
                IllegalArgumentException("Could not read the selected banner image. Please pick another photo.")
            )

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

            storageRef.putBytes(compressedBytes, metadata).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: CancellationException) {
            throw e
        } catch (e: StorageException) {
            Log.e(TAG, "Firebase Storage banner upload failed (code=${e.errorCode})", e)
            val friendly = when (e.errorCode) {
                StorageException.ERROR_NOT_AUTHORIZED,
                StorageException.ERROR_NOT_AUTHENTICATED ->
                    "Storage upload denied. Ensure Firebase Storage rules allow banner uploads, or paste an HTTPS banner URL."
                StorageException.ERROR_BUCKET_NOT_FOUND,
                StorageException.ERROR_PROJECT_NOT_FOUND,
                StorageException.ERROR_OBJECT_NOT_FOUND ->
                    "Firebase Storage is not enabled yet in Firebase Console. Enable Storage or paste an HTTPS banner URL."
                else ->
                    "Banner upload failed: ${e.localizedMessage ?: "check internet connection or paste an image URL."}"
            }
            Result.failure(IllegalStateException(friendly, e))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error uploading seasonal banner image", e)
            Result.failure(
                IllegalStateException(
                    "Banner upload failed: ${e.localizedMessage ?: "please try again or paste an HTTPS banner URL."}",
                    e
                )
            )
        }
    }

    private fun compressImageUriToJpeg(context: Context, uri: Uri): ByteArray? {
        val resolver = context.contentResolver

        // First pass: decode image bounds only to avoid OOM on high-res photos
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        } ?: return null

        val srcWidth = boundsOptions.outWidth
        val srcHeight = boundsOptions.outHeight
        if (srcWidth <= 0 || srcHeight <= 0) {
            return resolver.openInputStream(uri)?.use { it.readBytes() }
        }

        var sampleSize = 1
        while ((srcWidth / sampleSize) > MAX_IMAGE_DIMENSION_PX * 2 ||
            (srcHeight / sampleSize) > MAX_IMAGE_DIMENSION_PX * 2
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
        val finalBitmap = if (maxSide > MAX_IMAGE_DIMENSION_PX) {
            val scale = MAX_IMAGE_DIMENSION_PX.toFloat() / maxSide.toFloat()
            val targetW = (decoded.width * scale).roundToInt().coerceAtLeast(1)
            val targetH = (decoded.height * scale).roundToInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(decoded, targetW, targetH, true).also { scaled ->
                if (scaled !== decoded) decoded.recycle()
            }
        } else {
            decoded
        }

        return try {
            ByteArrayOutputStream().use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                out.toByteArray()
            }
        } finally {
            finalBitmap.recycle()
        }
    }
}
