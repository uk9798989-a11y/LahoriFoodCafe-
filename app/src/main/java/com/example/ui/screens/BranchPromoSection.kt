package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.MenuImageStorageHelper
import com.example.model.BranchBanner
import com.example.model.Promotion
import com.example.model.SeedMenuData
import com.example.ui.components.formatPkr
import com.example.ui.theme.LahoriBlack
import com.example.ui.theme.LahoriGold
import com.example.ui.theme.LahoriRed
import com.example.ui.theme.LahoriSuccess
import com.example.ui.theme.LahoriWarning
import com.example.ui.theme.LahoriWhite
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DURATION_PRESETS = listOf(
    1 to "24 Hours",
    3 to "3 Days",
    7 to "7 Days",
    14 to "14 Days",
    30 to "30 Days",
    0 to "No Expiry"
)

fun formatPromoExpiryLabel(expiresAt: Timestamp?, nowMillis: Long = System.currentTimeMillis()): String {
    if (expiresAt == null) return "No expiry"
    val diffMs = expiresAt.toDate().time - nowMillis
    if (diffMs <= 0L) {
        val fmt = SimpleDateFormat("MMM d", Locale.US)
        return "Expired ${fmt.format(expiresAt.toDate())}"
    }
    val hours = diffMs / (1000L * 60L * 60L)
    if (hours < 24L) {
        return "Ends in ${hours.coerceAtLeast(1L)}h"
    }
    val days = (hours + 23L) / 24L
    return "Ends in ${days}d"
}

@Composable
fun BranchPromoManagerTab(
    branchId: String,
    branchName: String,
    branchLabel: String,
    branchPromotions: List<Promotion>,
    branchBanners: List<BranchBanner>,
    isSavingPromo: Boolean,
    isSavingBanner: Boolean,
    onSaveBranchPromo: (
        promoDocId: String?,
        code: String,
        discountType: String,
        discountValue: Long,
        minOrderPkr: Long,
        active: Boolean,
        expiresAt: Timestamp?,
        onError: (String) -> Unit,
        onDone: () -> Unit
    ) -> Unit,
    onDeleteBranchPromo: (String) -> Unit,
    onSaveBranchBanner: (
        bannerId: String?,
        title: String,
        subtitle: String,
        promoCode: String,
        badgeText: String,
        imageUrl: String,
        pickedImageUri: Uri?,
        targetCategory: String,
        active: Boolean,
        expiresAt: Timestamp?,
        onError: (String) -> Unit,
        onDone: () -> Unit
    ) -> Unit,
    onDeleteBranchBanner: (String) -> Unit
) {
    var showPromoDialog by remember { mutableStateOf(false) }
    var editingPromo by remember { mutableStateOf<Promotion?>(null) }
    var showBannerDialog by remember { mutableStateOf(false) }
    var editingBanner by remember { mutableStateOf<BranchBanner?>(null) }

    val nowMillis = System.currentTimeMillis()
    val liveBannersCount = remember(branchBanners, nowMillis) {
        branchBanners.count { it.isVisibleForBranch(branchId, nowMillis) }
    }
    val livePromosCount = remember(branchPromotions, nowMillis) {
        branchPromotions.count { it.isCurrentlyValidForBranch(branchId, nowMillis) }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("branch_promo_manager_list")
    ) {
        item(key = "branch_promo_intro") {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = LahoriBlack,
                border = BorderStroke(1.5.dp, LahoriGold.copy(alpha = 0.65f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${branchLabel.uppercase()} PROMO STUDIO",
                            style = MaterialTheme.typography.labelLarge,
                            color = LahoriGold
                        )
                        Surface(
                            color = LahoriRed,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "$liveBannersCount BANNERS • $livePromosCount CODES",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = LahoriWhite,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = "Temporary discount codes & seasonal banners created here appear ONLY for customers who selected $branchLabel.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LahoriWhite.copy(alpha = 0.85f)
                    )
                }
            }
        }

        item(key = "branch_promo_actions") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        editingPromo = null
                        showPromoDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LahoriRed),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("branch_add_promo_btn")
                ) {
                    Icon(Icons.Filled.LocalOffer, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("NEW CODE", style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp), maxLines = 1)
                }

                Button(
                    onClick = {
                        editingBanner = null
                        showBannerDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LahoriBlack, contentColor = LahoriGold),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("branch_add_banner_btn")
                ) {
                    Icon(Icons.Filled.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("NEW BANNER", style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp), maxLines = 1)
                }
            }
        }

        // Section 1: Seasonal Branch Banners
        item(key = "seasonal_banners_header") {
            Text(
                text = "SEASONAL BRANCH BANNERS (${branchBanners.size})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (branchBanners.isEmpty()) {
            item(key = "empty_branch_banners") {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No seasonal banners for $branchLabel yet. Tap NEW BANNER to upload a photo banner that appears at the top of Explore for customers of this branch.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(branchBanners, key = { "banner_${it.id}" }) { banner ->
                val expired = banner.isExpired(nowMillis)
                val statusLabel = when {
                    expired -> "EXPIRED"
                    banner.active -> "LIVE"
                    else -> "PAUSED"
                }
                val statusColor = when {
                    expired -> LahoriRed
                    banner.active -> LahoriSuccess
                    else -> LahoriWarning
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("branch_banner_card_${banner.id}")
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(132.dp)
                                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                        ) {
                            BranchBannerPhotoSurface(
                                imageUrl = banner.imageUrl,
                                previewModel = null,
                                contentDescription = banner.title,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.25f),
                                                Color.Black.copy(alpha = 0.72f)
                                            )
                                        )
                                    )
                            )
                            Surface(
                                color = statusColor,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "$statusLabel • ${formatPromoExpiryLabel(banner.expiresAt, nowMillis)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = LahoriWhite,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = banner.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = LahoriWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (banner.subtitle.isNotBlank()) {
                                    Text(
                                        text = banner.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LahoriWhite.copy(alpha = 0.88f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = buildString {
                                        append(banner.badgeText.ifBlank { "SEASONAL OFFER" })
                                        append(" • ")
                                        append(banner.targetCategory)
                                        if (banner.promoCode.isNotBlank()) {
                                            append(" • Code: ${banner.promoCode}")
                                        }
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        editingBanner = banner
                                        showBannerDialog = true
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit banner", tint = MaterialTheme.colorScheme.onSurface)
                                }
                                IconButton(
                                    onClick = { onDeleteBranchBanner(banner.id) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete banner", tint = LahoriRed)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Temporary Branch Discount Codes
        item(key = "branch_promos_header") {
            Text(
                text = "TEMPORARY DISCOUNT CODES (${branchPromotions.size})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        if (branchPromotions.isEmpty()) {
            item(key = "empty_branch_promos") {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No branch-exclusive promo codes yet. Tap NEW CODE to launch a limited-time discount valid only at $branchLabel.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(branchPromotions, key = { "promo_${it.id}" }) { promo ->
                val expired = promo.isExpired(nowMillis)
                val discountLabel = if (promo.discountType.equals("PERCENT", ignoreCase = true)) {
                    "${promo.discountValue}% OFF"
                } else {
                    "${formatPkr(promo.discountValue)} OFF"
                }
                val statusText = when {
                    expired -> "EXPIRED"
                    promo.active -> "ACTIVE"
                    else -> "PAUSED"
                }
                val statusTint = when {
                    expired -> LahoriRed
                    promo.active -> LahoriSuccess
                    else -> LahoriWarning
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("branch_promo_card_${promo.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = promo.code,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    color = LahoriRed.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = discountLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = LahoriRed,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Min order: ${formatPkr(promo.minOrderPkr)} • $branchLabel only",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = statusTint,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "$statusText • ${formatPromoExpiryLabel(promo.expiresAt, nowMillis)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusTint
                                )
                            }
                        }

                        Row {
                            IconButton(
                                onClick = {
                                    editingPromo = promo
                                    showPromoDialog = true
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit promo", tint = MaterialTheme.colorScheme.onSurface)
                            }
                            IconButton(
                                onClick = { onDeleteBranchPromo(promo.id) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete promo", tint = LahoriRed)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPromoDialog) {
        BranchPromoEditorDialog(
            branchLabel = branchLabel,
            initialPromo = editingPromo,
            isSaving = isSavingPromo,
            onDismiss = {
                if (!isSavingPromo) {
                    showPromoDialog = false
                    editingPromo = null
                }
            },
            onSave = { docId, code, discountType, discountValue, minOrderPkr, active, expiresAt, onError ->
                onSaveBranchPromo(
                    docId,
                    code,
                    discountType,
                    discountValue,
                    minOrderPkr,
                    active,
                    expiresAt,
                    onError
                ) {
                    showPromoDialog = false
                    editingPromo = null
                }
            }
        )
    }

    if (showBannerDialog) {
        BranchBannerEditorDialog(
            branchLabel = branchLabel,
            initialBanner = editingBanner,
            isSaving = isSavingBanner,
            onDismiss = {
                if (!isSavingBanner) {
                    showBannerDialog = false
                    editingBanner = null
                }
            },
            onSave = { bannerId, title, subtitle, promoCode, badgeText, imageUrl, pickedUri, targetCategory, active, expiresAt, onError ->
                onSaveBranchBanner(
                    bannerId,
                    title,
                    subtitle,
                    promoCode,
                    badgeText,
                    imageUrl,
                    pickedUri,
                    targetCategory,
                    active,
                    expiresAt,
                    onError
                ) {
                    showBannerDialog = false
                    editingBanner = null
                }
            }
        )
    }
}

@Composable
fun BranchBannerPhotoSurface(
    imageUrl: String,
    previewModel: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val model: Any? = previewModel ?: imageUrl.trim().takeIf { it.isNotBlank() }
    if (model != null) {
        val context = LocalContext.current
        val req = remember(model, context) {
            ImageRequest.Builder(context)
                .data(model)
                .crossfade(true)
                .build()
        }
        SubcomposeAsyncImage(
            model = req,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier,
            loading = {
                DefaultSeasonalBannerPlaceholder(modifier = Modifier.fillMaxSize())
            },
            error = {
                DefaultSeasonalBannerPlaceholder(modifier = Modifier.fillMaxSize())
            }
        )
    } else {
        DefaultSeasonalBannerPlaceholder(modifier = modifier)
    }
}

@Composable
private fun DefaultSeasonalBannerPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = listOf(Color(0xFF1A1A1A), Color(0xFF5A0A18), LahoriRed)
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Campaign,
                contentDescription = null,
                tint = LahoriGold,
                modifier = Modifier.size(34.dp)
            )
            Text(
                text = "Upload Seasonal Banner Image",
                style = MaterialTheme.typography.labelMedium,
                color = LahoriWhite.copy(alpha = 0.85f)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BranchPromoEditorDialog(
    branchLabel: String,
    initialPromo: Promotion?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        promoDocId: String?,
        code: String,
        discountType: String,
        discountValue: Long,
        minOrderPkr: Long,
        active: Boolean,
        expiresAt: Timestamp?,
        onError: (String) -> Unit
    ) -> Unit
) {
    var code by remember(initialPromo?.id) { mutableStateOf(initialPromo?.code ?: "") }
    var discountType by remember(initialPromo?.id) { mutableStateOf(initialPromo?.discountType ?: "PERCENT") }
    var discountValueStr by remember(initialPromo?.id) { mutableStateOf(initialPromo?.discountValue?.toString() ?: "15") }
    var minOrderStr by remember(initialPromo?.id) { mutableStateOf(initialPromo?.minOrderPkr?.toString() ?: "500") }
    var durationDays by remember(initialPromo?.id) {
        mutableIntStateOf(
            if (initialPromo == null) 7
            else if (initialPromo.expiresAt == null) 0
            else {
                val remDays = ((initialPromo.expiresAt.toDate().time - System.currentTimeMillis()) / (1000L * 60L * 60L * 24L)).toInt()
                when {
                    remDays <= 1 -> 1
                    remDays <= 3 -> 3
                    remDays <= 7 -> 7
                    remDays <= 14 -> 14
                    else -> 30
                }
            }
        )
    }
    var active by remember(initialPromo?.id) { mutableStateOf(initialPromo?.active ?: true) }
    var errorMsg by remember(initialPromo?.id) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column {
                Text(
                    text = if (initialPromo == null) "New Branch Promo Code" else "Edit Branch Promo Code",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Valid exclusively for $branchLabel customers",
                    style = MaterialTheme.typography.bodySmall,
                    color = LahoriRed
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it.uppercase().filter { ch -> ch.isLetterOrDigit() || ch == '_' || ch == '-' }
                        errorMsg = null
                    },
                    label = { Text("Promo Code (e.g. TMP15, EID20)") },
                    enabled = initialPromo == null && !isSaving,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("branch_promo_code_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("PERCENT" to "% Percent", "FLAT" to "PKR Flat").forEach { (type, label) ->
                        FilterChip(
                            selected = discountType == type,
                            enabled = !isSaving,
                            onClick = { discountType = type; errorMsg = null },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = discountValueStr,
                        onValueChange = { discountValueStr = it.filter { c -> c.isDigit() }; errorMsg = null },
                        enabled = !isSaving,
                        label = { Text(if (discountType == "PERCENT") "Percent (1-90)" else "Flat (PKR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minOrderStr,
                        onValueChange = { minOrderStr = it.filter { c -> c.isDigit() }; errorMsg = null },
                        enabled = !isSaving,
                        label = { Text("Min Order PKR") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Temporary Validity Duration",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DURATION_PRESETS.forEach { (days, label) ->
                        FilterChip(
                            selected = durationDays == days,
                            enabled = !isSaving,
                            onClick = { durationDays = days },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LahoriRed,
                                selectedLabelColor = LahoriWhite
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Code Active", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = active,
                        enabled = !isSaving,
                        onCheckedChange = { active = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = LahoriRed)
                    )
                }

                if (errorMsg != null) {
                    Text(errorMsg!!, style = MaterialTheme.typography.bodySmall, color = LahoriRed)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                onClick = {
                    val clean = code.trim().uppercase()
                    val valNum = discountValueStr.toLongOrNull() ?: 0L
                    val minOrder = minOrderStr.toLongOrNull() ?: 0L
                    when {
                        clean.length !in 2..30 -> errorMsg = "Code must be 2–30 letters or numbers."
                        discountType == "PERCENT" && valNum !in 1..90 -> errorMsg = "Percent must be 1 to 90."
                        discountType == "FLAT" && valNum !in 1..100000 -> errorMsg = "Flat discount must be > 0 PKR."
                        else -> {
                            errorMsg = null
                            val expiresAt = if (durationDays > 0) {
                                Timestamp(Date(System.currentTimeMillis() + durationDays * 24L * 60L * 60L * 1000L))
                            } else {
                                null
                            }
                            onSave(
                                initialPromo?.id?.takeIf { it.isNotBlank() },
                                clean,
                                discountType,
                                valNum,
                                minOrder,
                                active,
                                expiresAt
                            ) { err -> errorMsg = err }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LahoriRed),
                modifier = Modifier.testTag("branch_promo_save_btn")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = LahoriWhite, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("SAVING...")
                } else {
                    Text("SAVE CODE")
                }
            }
        },
        dismissButton = {
            TextButton(enabled = !isSaving, onClick = onDismiss) {
                Text("CANCEL", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BranchBannerEditorDialog(
    branchLabel: String,
    initialBanner: BranchBanner?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        bannerId: String?,
        title: String,
        subtitle: String,
        promoCode: String,
        badgeText: String,
        imageUrl: String,
        pickedImageUri: Uri?,
        targetCategory: String,
        active: Boolean,
        expiresAt: Timestamp?,
        onError: (String) -> Unit
    ) -> Unit
) {
    var title by remember(initialBanner?.id) { mutableStateOf(initialBanner?.title ?: "") }
    var subtitle by remember(initialBanner?.id) { mutableStateOf(initialBanner?.subtitle ?: "") }
    var badgeText by remember(initialBanner?.id) { mutableStateOf(initialBanner?.badgeText ?: "SEASONAL OFFER") }
    var promoCode by remember(initialBanner?.id) { mutableStateOf(initialBanner?.promoCode ?: "") }
    var imageUrl by remember(initialBanner?.id) { mutableStateOf(initialBanner?.imageUrl ?: "") }
    var pickedImageUri by remember(initialBanner?.id) { mutableStateOf<Uri?>(null) }
    var targetCategory by remember(initialBanner?.id) { mutableStateOf(initialBanner?.targetCategory ?: "DEALS") }
    var durationDays by remember(initialBanner?.id) {
        mutableIntStateOf(
            if (initialBanner == null) 7
            else if (initialBanner.expiresAt == null) 0
            else {
                val remDays = ((initialBanner.expiresAt.toDate().time - System.currentTimeMillis()) / (1000L * 60L * 60L * 24L)).toInt()
                when {
                    remDays <= 1 -> 1
                    remDays <= 3 -> 3
                    remDays <= 7 -> 7
                    remDays <= 14 -> 14
                    else -> 30
                }
            }
        )
    }
    var active by remember(initialBanner?.id) { mutableStateOf(initialBanner?.active ?: true) }
    var errorMsg by remember(initialBanner?.id) { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedImageUri = uri
            errorMsg = null
        }
    }

    val hasImage = pickedImageUri != null || imageUrl.isNotBlank()
    val categories = SeedMenuData.categories.filter { it != "ALL" }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column {
                Text(
                    text = if (initialBanner == null) "New Seasonal Banner" else "Edit Seasonal Banner",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Shown ONLY to $branchLabel customers",
                    style = MaterialTheme.typography.bodySmall,
                    color = LahoriRed
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Banner Image Upload & Live Preview
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Banner Image",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when {
                                    pickedImageUri != null -> LahoriRed
                                    imageUrl.isNotBlank() -> LahoriSuccess
                                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                                }
                            ) {
                                Text(
                                    text = when {
                                        pickedImageUri != null -> "READY TO UPLOAD"
                                        imageUrl.isNotBlank() -> "BANNER SET"
                                        else -> "REQUIRED"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.ExtraBold),
                                    color = if (hasImage) LahoriWhite else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(136.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .testTag("branch_banner_preview_box")
                        ) {
                            BranchBannerPhotoSurface(
                                imageUrl = imageUrl,
                                previewModel = pickedImageUri,
                                contentDescription = title.ifBlank { "Seasonal banner preview" },
                                modifier = Modifier.fillMaxSize()
                            )
                            if (hasImage) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                                            )
                                        )
                                )
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = title.ifBlank { "Your Seasonal Headline" },
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                        color = LahoriWhite,
                                        maxLines = 1
                                    )
                                }
                            }
                            if (isSaving && pickedImageUri != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(LahoriBlack.copy(alpha = 0.58f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = LahoriGold, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                enabled = !isSaving,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, LahoriRed),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("branch_banner_upload_btn")
                            ) {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = LahoriRed, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (hasImage) "CHANGE IMAGE" else "UPLOAD IMAGE",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = LahoriRed,
                                    maxLines = 1
                                )
                            }

                            if (hasImage) {
                                OutlinedButton(
                                    onClick = {
                                        pickedImageUri = null
                                        imageUrl = ""
                                        errorMsg = null
                                    },
                                    enabled = !isSaving,
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("branch_banner_remove_img_btn")
                                ) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove banner image", modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("REMOVE", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = imageUrl,
                            onValueChange = { updated ->
                                imageUrl = updated
                                if (updated.isNotBlank()) pickedImageUri = null
                                errorMsg = null
                            },
                            enabled = !isSaving,
                            label = { Text("Or paste Banner Image URL (https://...)") },
                            leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            trailingIcon = if (imageUrl.isNotBlank() && !isSaving) {
                                {
                                    IconButton(onClick = { imageUrl = ""; errorMsg = null }, modifier = Modifier.size(40.dp)) {
                                        Icon(Icons.Filled.Close, contentDescription = "Clear URL", modifier = Modifier.size(16.dp))
                                    }
                                }
                            } else null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("branch_banner_url_input")
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; errorMsg = null },
                    enabled = !isSaving,
                    label = { Text("Banner Headline (e.g. Winter Pizza Fest)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("branch_banner_title_input")
                )

                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    enabled = !isSaving,
                    label = { Text("Offer Subtitle (optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = badgeText,
                        onValueChange = { badgeText = it },
                        enabled = !isSaving,
                        label = { Text("Badge") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = promoCode,
                        onValueChange = { promoCode = it.uppercase().filter { ch -> ch.isLetterOrDigit() || ch == '_' || ch == '-' } },
                        enabled = !isSaving,
                        label = { Text("Linked Code (opt.)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Target Menu Category", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = targetCategory == cat,
                            enabled = !isSaving,
                            onClick = { targetCategory = cat },
                            label = { Text(cat, fontSize = 10.sp) }
                        )
                    }
                }

                Text("Banner Duration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DURATION_PRESETS.forEach { (days, label) ->
                        FilterChip(
                            selected = durationDays == days,
                            enabled = !isSaving,
                            onClick = { durationDays = days },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LahoriRed,
                                selectedLabelColor = LahoriWhite
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Banner Active", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = active,
                        enabled = !isSaving,
                        onCheckedChange = { active = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = LahoriRed)
                    )
                }

                if (errorMsg != null) {
                    Text(errorMsg!!, style = MaterialTheme.typography.bodySmall, color = LahoriRed)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                onClick = {
                    val trimmedUrl = imageUrl.trim()
                    when {
                        title.trim().length < 2 -> errorMsg = "Enter a banner headline (min 2 chars)."
                        pickedImageUri == null && trimmedUrl.length < 8 -> errorMsg = "Upload a banner photo or paste an https:// image URL."
                        pickedImageUri == null && !MenuImageStorageHelper.isValidRemoteImageUrl(trimmedUrl) ->
                            errorMsg = "Banner Image URL must start with https:// or http://"
                        else -> {
                            errorMsg = null
                            val expiresAt = if (durationDays > 0) {
                                Timestamp(Date(System.currentTimeMillis() + durationDays * 24L * 60L * 60L * 1000L))
                            } else {
                                null
                            }
                            onSave(
                                initialBanner?.id?.takeIf { it.isNotBlank() },
                                title.trim(),
                                subtitle.trim(),
                                promoCode.trim().uppercase(),
                                badgeText.trim().ifBlank { "SEASONAL OFFER" },
                                trimmedUrl,
                                pickedImageUri,
                                targetCategory,
                                active,
                                expiresAt
                            ) { err -> errorMsg = err }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LahoriRed),
                modifier = Modifier.testTag("branch_banner_save_btn")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = LahoriWhite, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (pickedImageUri != null) "UPLOADING..." else "SAVING...")
                } else {
                    Text("PUBLISH BANNER")
                }
            }
        },
        dismissButton = {
            TextButton(enabled = !isSaving, onClick = onDismiss) {
                Text("CANCEL", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    )
}
