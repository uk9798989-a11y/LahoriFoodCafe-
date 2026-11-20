package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TakeoutDining
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.MenuImageStorageHelper
import com.example.model.MenuItem
import com.example.model.SeedMenuData
import com.example.ui.theme.LahoriBlack
import com.example.ui.theme.LahoriGold
import com.example.ui.theme.LahoriRed
import com.example.ui.theme.LahoriWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun formatPkr(amount: Long): String {
    val formatted = "%,d".format(amount)
    return "Rs. $formatted"
}

fun resolveMenuDrawableRes(imageKey: String, itemId: String = ""): Int? {
    val cleanKey = imageKey.trim().lowercase()
    val byExplicitPhotoKey: Int? = when (cleanKey) {
        "menu_hero_platter" -> R.drawable.menu_hero_platter
        // Pizzas (Pages 2-4)
        "menu_one_meter_pizza" -> R.drawable.menu_one_meter_pizza
        "menu_pizza_lahori_special" -> R.drawable.menu_pizza_lahori_special
        "menu_pizza_fajita" -> R.drawable.menu_pizza_fajita
        "menu_pizza_doner_stick" -> R.drawable.menu_pizza_doner_stick
        "menu_pizza_tikka" -> R.drawable.menu_pizza_tikka
        "menu_pizza_supreme" -> R.drawable.menu_pizza_supreme
        "menu_pizza_hot_spicy" -> R.drawable.menu_pizza_hot_spicy
        "menu_pizza_peri_peri" -> R.drawable.menu_pizza_peri_peri
        "menu_pizza_bonfire" -> R.drawable.menu_pizza_bonfire
        "menu_pizza_shahi" -> R.drawable.menu_pizza_shahi
        "menu_pizza_malai_boti" -> R.drawable.menu_pizza_malai_boti
        "menu_pizza_crunchy_cheese" -> R.drawable.menu_pizza_crunchy_cheese
        "menu_pizza_lazania" -> R.drawable.menu_pizza_lazania
        "menu_pizza_premium_pan" -> R.drawable.menu_pizza_premium_pan
        "menu_pizza_bihari_kabab" -> R.drawable.menu_pizza_bihari_kabab
        "menu_pizza_makhni_kabab" -> R.drawable.menu_pizza_makhni_kabab
        "menu_pizza_crown_crust" -> R.drawable.menu_pizza_crown_crust
        "menu_pizza_royal_crust" -> R.drawable.menu_pizza_royal_crust
        "menu_pizza_cheese_crust" -> R.drawable.menu_pizza_cheese_crust
        "menu_pizza_double_seekh" -> R.drawable.menu_pizza_double_seekh
        // Burgers (Page 5)
        "menu_burger_zinger" -> R.drawable.menu_burger_zinger
        "menu_burger_lahori_sp" -> R.drawable.menu_burger_lahori_sp
        "menu_burger_patty" -> R.drawable.menu_burger_patty
        "menu_burger_double_decker" -> R.drawable.menu_burger_double_decker
        "menu_burger_jalapeno" -> R.drawable.menu_burger_jalapeno
        "menu_burger_beef" -> R.drawable.menu_burger_beef
        "menu_burger_tower" -> R.drawable.menu_burger_tower
        "menu_burger_chicken" -> R.drawable.menu_burger_chicken
        "menu_extra_cheese_slice" -> R.drawable.menu_extra_cheese_slice
        // Rolls & Wraps (Page 6)
        "menu_roll_lahori_sp" -> R.drawable.menu_roll_lahori_sp
        "menu_roll_bihari_kabab" -> R.drawable.menu_roll_bihari_kabab
        "menu_roll_tikka_paratha" -> R.drawable.menu_roll_tikka_paratha
        "menu_roll_zinger" -> R.drawable.menu_roll_zinger
        "menu_roll_malai_boti" -> R.drawable.menu_roll_malai_boti
        "menu_wrap_lahori_grilled" -> R.drawable.menu_wrap_lahori_grilled
        "menu_shawarma_chicken" -> R.drawable.menu_shawarma_chicken
        "menu_shawarma_zinger" -> R.drawable.menu_shawarma_zinger
        // Fried Chicken, Fries, Sandwiches (Page 7)
        "menu_fc_crispy_wings" -> R.drawable.menu_fc_crispy_wings
        "menu_fc_nuggets" -> R.drawable.menu_fc_nuggets
        "menu_fc_glazed_wings" -> R.drawable.menu_fc_glazed_wings
        "menu_fries_regular" -> R.drawable.menu_fries_regular
        "menu_fries_loaded" -> R.drawable.menu_fries_loaded
        "menu_sandwich_club" -> R.drawable.menu_sandwich_club
        "menu_fc_hot_shot" -> R.drawable.menu_fc_hot_shot
        "menu_fc_broast" -> R.drawable.menu_fc_broast
        // Pasta, Drinks, Dips (Page 8)
        "menu_pasta_lahori_sp" -> R.drawable.menu_pasta_lahori_sp
        "menu_pasta_flaming" -> R.drawable.menu_pasta_flaming
        "menu_pasta_crunchy" -> R.drawable.menu_pasta_crunchy
        "menu_pasta_creamy" -> R.drawable.menu_pasta_creamy
        "menu_drinks_assorted" -> R.drawable.menu_drinks_assorted
        "menu_dips_assorted" -> R.drawable.menu_dips_assorted
        // Deals 1-19 + Birthday (Pages 9-10)
        "menu_deal_1" -> R.drawable.menu_deal_1
        "menu_deal_2" -> R.drawable.menu_deal_2
        "menu_deal_3" -> R.drawable.menu_deal_3
        "menu_deal_4" -> R.drawable.menu_deal_4
        "menu_deal_5" -> R.drawable.menu_deal_5
        "menu_deal_6" -> R.drawable.menu_deal_6
        "menu_deal_7" -> R.drawable.menu_deal_7
        "menu_deal_8" -> R.drawable.menu_deal_8
        "menu_deal_9" -> R.drawable.menu_deal_9
        "menu_deal_10" -> R.drawable.menu_deal_10
        "menu_deal_11" -> R.drawable.menu_deal_11
        "menu_deal_12" -> R.drawable.menu_deal_12
        "menu_deal_13" -> R.drawable.menu_deal_13
        "menu_deal_14" -> R.drawable.menu_deal_14
        "menu_deal_15" -> R.drawable.menu_deal_15
        "menu_deal_16" -> R.drawable.menu_deal_16
        "menu_deal_17" -> R.drawable.menu_deal_17
        "menu_deal_18" -> R.drawable.menu_deal_18
        "menu_deal_19" -> R.drawable.menu_deal_19
        "menu_deal_birthday" -> R.drawable.menu_deal_birthday
        else -> null
    }
    if (byExplicitPhotoKey != null) return byExplicitPhotoKey

    // Fallback by canonical item ID so pre-existing Firestore documents also show their exact PDF photo
    val normalizedId = itemId.trim().lowercase().removePrefix("seed_")
    if (normalizedId.isNotBlank()) {
        val byItemId: Int? = when (normalizedId) {
            "lfc_pasta_lahori_special" -> R.drawable.menu_pasta_lahori_sp
            "lfc_pasta_flaming" -> R.drawable.menu_pasta_flaming
            "lfc_pasta_crunchy" -> R.drawable.menu_pasta_crunchy
            "lfc_pasta_creamy" -> R.drawable.menu_pasta_creamy
            "lfc_deal_1" -> R.drawable.menu_deal_1
            "lfc_deal_2" -> R.drawable.menu_deal_2
            "lfc_deal_3" -> R.drawable.menu_deal_3
            "lfc_deal_4" -> R.drawable.menu_deal_4
            "lfc_deal_5" -> R.drawable.menu_deal_5
            "lfc_deal_6" -> R.drawable.menu_deal_6
            "lfc_deal_7" -> R.drawable.menu_deal_7
            "lfc_deal_8" -> R.drawable.menu_deal_8
            "lfc_deal_9" -> R.drawable.menu_deal_9
            "lfc_deal_10" -> R.drawable.menu_deal_10
            "lfc_deal_11" -> R.drawable.menu_deal_11
            "lfc_deal_12" -> R.drawable.menu_deal_12
            "lfc_deal_13" -> R.drawable.menu_deal_13
            "lfc_deal_14" -> R.drawable.menu_deal_14
            "lfc_deal_15" -> R.drawable.menu_deal_15
            "lfc_deal_16" -> R.drawable.menu_deal_16
            "lfc_deal_17" -> R.drawable.menu_deal_17
            "lfc_deal_18" -> R.drawable.menu_deal_18
            "lfc_deal_19" -> R.drawable.menu_deal_19
            "lfc_deal_birthday" -> R.drawable.menu_deal_birthday
            "lfc_burger_zinger" -> R.drawable.menu_burger_zinger
            "lfc_burger_signature" -> R.drawable.menu_burger_lahori_sp
            "lfc_burger_patty" -> R.drawable.menu_burger_patty
            "lfc_burger_double_decker" -> R.drawable.menu_burger_double_decker
            "lfc_burger_jalapeno" -> R.drawable.menu_burger_jalapeno
            "lfc_burger_beef" -> R.drawable.menu_burger_beef
            "lfc_burger_tower" -> R.drawable.menu_burger_tower
            "lfc_burger_chicken" -> R.drawable.menu_burger_chicken
            "lfc_burger_extra_cheese" -> R.drawable.menu_extra_cheese_slice
            "lfc_one_meter_pizza" -> R.drawable.menu_one_meter_pizza
            "lfc_ext_lazania" -> R.drawable.menu_pizza_lazania
            "lfc_ext_premium_pan" -> R.drawable.menu_pizza_premium_pan
            "lfc_ext_pizza_stick" -> R.drawable.menu_pizza_doner_stick
            "lfc_ext_peri_peri" -> R.drawable.menu_pizza_peri_peri
            "lfc_ext_malai_boti" -> R.drawable.menu_pizza_malai_boti
            "lfc_ext_kababish" -> R.drawable.menu_pizza_bihari_kabab
            "lfc_ext_crispy" -> R.drawable.menu_pizza_crunchy_cheese
            "lfc_trad_lahori_special" -> R.drawable.menu_pizza_lahori_special
            "lfc_trad_fajita" -> R.drawable.menu_pizza_fajita
            "lfc_trad_tikka" -> R.drawable.menu_pizza_tikka
            "lfc_trad_chicken_supreme" -> R.drawable.menu_pizza_supreme
            "lfc_trad_hot_spicy" -> R.drawable.menu_pizza_hot_spicy
            "lfc_trad_peri_tikka" -> R.drawable.menu_pizza_peri_peri
            "lfc_trad_bonfire" -> R.drawable.menu_pizza_bonfire
            "lfc_trad_shahi", "lfc_trad_nawabi" -> R.drawable.menu_pizza_shahi
            "lfc_trad_malai_boti", "lfc_trad_creamy_mughlai" -> R.drawable.menu_pizza_malai_boti
            "lfc_trad_crispy" -> R.drawable.menu_pizza_crunchy_cheese
            "lfc_prem_bihari_kabab" -> R.drawable.menu_pizza_bihari_kabab
            "lfc_prem_makhni_kabab", "lfc_prem_gola_kabab" -> R.drawable.menu_pizza_makhni_kabab
            "lfc_prem_crown_crust" -> R.drawable.menu_pizza_crown_crust
            "lfc_prem_royal_crust", "lfc_prem_kabab_crust" -> R.drawable.menu_pizza_royal_crust
            "lfc_prem_cheese_crust" -> R.drawable.menu_pizza_cheese_crust
            "lfc_prem_double_seekh" -> R.drawable.menu_pizza_double_seekh
            "lfc_wrap_lahori_special" -> R.drawable.menu_roll_lahori_sp
            "lfc_wrap_bihari_kabab" -> R.drawable.menu_roll_bihari_kabab
            "lfc_wrap_tikka_paratha" -> R.drawable.menu_roll_tikka_paratha
            "lfc_wrap_zinger_roll" -> R.drawable.menu_roll_zinger
            "lfc_wrap_malai_boti" -> R.drawable.menu_roll_malai_boti
            "lfc_wrap_grill" -> R.drawable.menu_wrap_lahori_grilled
            "lfc_wrap_chicken_shawarma" -> R.drawable.menu_shawarma_chicken
            "lfc_wrap_zinger_shawarma" -> R.drawable.menu_shawarma_zinger
            "lfc_sand_lahori_special", "lfc_sand_chicken", "lfc_sand_club", "lfc_sand_malai_panini" -> R.drawable.menu_sandwich_club
            "lfc_fries_plain", "lfc_fries_masala", "lfc_fries_single" -> R.drawable.menu_fries_regular
            "lfc_fries_pizza_loaded", "lfc_fries_crispy_loaded",
            "lfc_fries_loaded_small", "lfc_fries_loaded_large",
            "lfc_fries_crunchy_half", "lfc_fries_crunchy_full" -> R.drawable.menu_fries_loaded
            "lfc_fc_crispy_wings" -> R.drawable.menu_fc_crispy_wings
            "lfc_fc_nuggets" -> R.drawable.menu_fc_nuggets
            "lfc_fc_grilled_wings", "lfc_fc_smoky_wings",
            "lfc_fc_creamy_wings", "lfc_fc_peri_peri_wings" -> R.drawable.menu_fc_glazed_wings
            "lfc_fc_hot_shots" -> R.drawable.menu_fc_hot_shot
            "lfc_fc_leg_broast", "lfc_fc_chest_broast" -> R.drawable.menu_fc_broast
            "lfc_dip_sauce", "lfc_dip_special", "lfc_dip_bonfire",
            "lfc_dip_peri", "lfc_dip_garlic" -> R.drawable.menu_dips_assorted
            "lfc_top_chicken" -> R.drawable.menu_pizza_lahori_special
            "lfc_top_cheese" -> R.drawable.menu_extra_cheese_slice
            "lfc_drink_regular", "lfc_drink_500ml", "lfc_drink_1l",
            "lfc_drink_15l", "lfc_drink_jumbo",
            "lfc_drink_water_small", "lfc_drink_water_large" -> R.drawable.menu_drinks_assorted
            else -> null
        }
        if (byItemId != null) return byItemId
    }

    // Generic category theme fallbacks mapped to real Lahori Food Cafe photography
    return when (cleanKey) {
        "pasta_bowl" -> R.drawable.menu_pasta_lahori_sp
        "combo_deal" -> R.drawable.menu_deal_3
        "birthday_deal" -> R.drawable.menu_deal_birthday
        "zinger_burger" -> R.drawable.menu_burger_zinger
        "extreme_pizza" -> R.drawable.menu_pizza_lahori_special
        "wrap_roll" -> R.drawable.menu_roll_lahori_sp
        "signature_sandwich" -> R.drawable.menu_sandwich_club
        "crispy_fries" -> R.drawable.menu_fries_regular
        "hot_wings" -> R.drawable.menu_fc_crispy_wings
        "pickled_crunch" -> R.drawable.menu_dips_assorted
        "soft_drink" -> R.drawable.menu_drinks_assorted
        else -> null
    }
}

@Composable
fun FoodArtworkBox(
    imageKey: String,
    modifier: Modifier = Modifier,
    imageUrl: String = "",
    previewModel: Any? = null,
    contentDescription: String? = null,
    compact: Boolean = false,
    itemId: String = ""
) {
    val trimmedUrl = imageUrl.trim()
    val bundledDrawable = remember(imageKey, itemId) { resolveMenuDrawableRes(imageKey, itemId) }
    val isInlineCloudPhoto = previewModel == null && MenuImageStorageHelper.isDataImageUri(trimmedUrl)

    if (isInlineCloudPhoto) {
        val decodedBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = trimmedUrl) {
            value = withContext(Dispatchers.IO) {
                MenuImageStorageHelper.decodeDataImageBitmap(trimmedUrl)
            }
        }
        val bmp = decodedBitmap
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        } else if (bundledDrawable != null) {
            Image(
                painter = painterResource(id = bundledDrawable),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        } else {
            DefaultFoodArtworkCanvas(
                imageKey = imageKey,
                compact = compact,
                modifier = modifier
            )
        }
        return
    }

    val resolvedModel: Any? = previewModel ?: trimmedUrl.takeIf { it.isNotBlank() }
    if (resolvedModel != null) {
        val context = LocalContext.current
        val request = remember(resolvedModel, context) {
            ImageRequest.Builder(context)
                .data(resolvedModel)
                .crossfade(true)
                .build()
        }
        SubcomposeAsyncImage(
            model = request,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier,
            loading = {
                if (bundledDrawable != null) {
                    Image(
                        painter = painterResource(id = bundledDrawable),
                        contentDescription = contentDescription,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    DefaultFoodArtworkCanvas(
                        imageKey = imageKey,
                        compact = compact,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            },
            error = {
                if (bundledDrawable != null) {
                    Image(
                        painter = painterResource(id = bundledDrawable),
                        contentDescription = contentDescription,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    DefaultFoodArtworkCanvas(
                        imageKey = imageKey,
                        compact = compact,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        )
    } else if (bundledDrawable != null) {
        Image(
            painter = painterResource(id = bundledDrawable),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        DefaultFoodArtworkCanvas(
            imageKey = imageKey,
            compact = compact,
            modifier = modifier
        )
    }
}

@Composable
private fun DefaultFoodArtworkCanvas(
    imageKey: String,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val (colors, icon, accentTint) = remember(imageKey) {
        when (imageKey) {
            "pasta_bowl" -> Triple(
                listOf(Color(0xFFFFF5DC), Color(0xFFFFCA6C), Color(0xFFE8611A)),
                Icons.Filled.DinnerDining,
                Color(0xFF9C3006)
            )
            "combo_deal" -> Triple(
                listOf(Color(0xFFFFEFE0), Color(0xFFFF9C4A), Color(0xFFD60027)),
                Icons.Filled.TakeoutDining,
                Color(0xFF94001B)
            )
            "birthday_deal" -> Triple(
                listOf(Color(0xFFFFEAF2), Color(0xFFFF8CB2), Color(0xFFE4002B)),
                Icons.Filled.Cake,
                Color(0xFF990024)
            )
            "zinger_burger" -> Triple(
                listOf(Color(0xFFFFF1DF), Color(0xFFFFC178), Color(0xFFFF7A3D)),
                Icons.Filled.LunchDining,
                Color(0xFFB32400)
            )
            "extreme_pizza" -> Triple(
                listOf(Color(0xFFFFEED9), Color(0xFFFFAE52), Color(0xFFE4002B)),
                Icons.Filled.LocalPizza,
                Color(0xFF9E140B)
            )
            "wrap_roll" -> Triple(
                listOf(Color(0xFFFFF4DE), Color(0xFFF7B963), Color(0xFFC95E18)),
                Icons.Filled.Fastfood,
                Color(0xFF7D3405)
            )
            "crispy_fries" -> Triple(
                listOf(Color(0xFFFFFBE0), Color(0xFFFFD84D), Color(0xFFE68A00)),
                Icons.Filled.TakeoutDining,
                Color(0xFFA85200)
            )
            "hot_wings" -> Triple(
                listOf(Color(0xFFFFE5DF), Color(0xFFFF8261), Color(0xFFD60027)),
                Icons.Filled.LocalFireDepartment,
                Color(0xFF99001A)
            )
            "signature_sandwich" -> Triple(
                listOf(Color(0xFFFFF6DE), Color(0xFFFFD37A), Color(0xFFE08824)),
                Icons.Filled.BakeryDining,
                Color(0xFF7D4806)
            )
            "pickled_crunch" -> Triple(
                listOf(Color(0xFFEBF9E0), Color(0xFFA6E07B), Color(0xFF2D8C45)),
                Icons.Filled.Fastfood,
                Color(0xFF145224)
            )
            "soft_drink" -> Triple(
                listOf(Color(0xFFE3F5FF), Color(0xFF69C0FF), Color(0xFF0E68B8)),
                Icons.Filled.LocalDrink,
                Color(0xFF084278)
            )
            else -> Triple(
                listOf(Color(0xFFFFE6EB), Color(0xFFFF889E), Color(0xFFE4002B)),
                Icons.Filled.Restaurant,
                Color(0xFF8A0019)
            )
        }
    }

    Box(
        modifier = modifier
            .background(Brush.radialGradient(colors = colors)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.34f),
                radius = size.minDimension * 0.46f,
                center = Offset(size.width * 0.5f, size.height * 0.52f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.22f),
                radius = size.minDimension * 0.28f,
                center = Offset(size.width * 0.80f, size.height * 0.22f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.16f),
                radius = size.minDimension * 0.22f,
                center = Offset(size.width * 0.18f, size.height * 0.82f)
            )
        }
        Surface(
            shape = CircleShape,
            color = LahoriWhite,
            shadowElevation = if (compact) 4.dp else 10.dp,
            modifier = Modifier.size(if (compact) 36.dp else 78.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentTint,
                    modifier = Modifier.size(if (compact) 20.dp else 44.dp)
                )
            }
        }
    }
}

@Composable
fun MenuItemCard(
    item: MenuItem,
    isFavorite: Boolean,
    cartQuantities: Map<String, Int>,
    onToggleFavorite: () -> Unit,
    onAddToBucket: (MenuItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val sizeOptions = remember(item.id, item.name) { SeedMenuData.sizeOptionsFor(item) }
    var selectedSizeIndex by remember(item.id) { mutableIntStateOf(0) }
    val safeIndex = if (sizeOptions.isNotEmpty()) selectedSizeIndex.coerceIn(0, sizeOptions.lastIndex) else -1
    val selectedOption = if (safeIndex >= 0) sizeOptions[safeIndex] else null

    val effectivePricePkr = selectedOption?.pricePkr ?: item.pricePkr
    val resolvedCartItem = remember(item, selectedOption) {
        if (selectedOption != null) {
            val suffix = selectedOption.label.lowercase().replace(" ", "_")
            item.copy(
                id = "${item.id}_$suffix",
                name = "${item.name} (${selectedOption.label})",
                pricePkr = selectedOption.pricePkr
            )
        } else {
            item
        }
    }
    val quantityInCart = cartQuantities[resolvedCartItem.id] ?: 0

    val cardInteraction = remember { MutableInteractionSource() }
    val cardPressed by cardInteraction.collectIsPressedAsState()
    val cardHovered by cardInteraction.collectIsHoveredAsState()

    val btnInteraction = remember { MutableInteractionSource() }
    val btnPressed by btnInteraction.collectIsPressedAsState()
    val btnHovered by btnInteraction.collectIsHoveredAsState()

    val cardScale by animateFloatAsState(
        targetValue = if (cardPressed || cardHovered) 1.025f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_scale"
    )
    val btnScale by animateFloatAsState(
        targetValue = if (btnPressed || btnHovered) 1.05f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "button_scale"
    )
    val starScale by animateFloatAsState(
        targetValue = if (isFavorite) 1.2f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "star_scale"
    )
    val starTint by animateColorAsState(
        targetValue = if (isFavorite) LahoriGold else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
        label = "star_tint"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.14f),
                spotColor = Color.Black.copy(alpha = 0.16f)
            )
            .clickable(
                interactionSource = cardInteraction,
                indication = null,
                onClick = { if (item.available) onAddToBucket(resolvedCartItem) }
            )
            .testTag("menu_card_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Larger, vibrant food visual centered at top of card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                FoodArtworkBox(
                    imageKey = item.imageKey,
                    imageUrl = item.imageUrl,
                    contentDescription = item.name,
                    itemId = item.id,
                    modifier = Modifier.fillMaxSize()
                )

                // Bold Red Price Badge (Top-Left)
                Surface(
                    color = LahoriRed,
                    shape = RoundedCornerShape(10.dp),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = formatPkr(effectivePricePkr),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = LahoriWhite,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                // Favorite Star (Top-Right)
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(38.dp)
                ) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("favorite_star_${item.id}")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                            tint = starTint,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer {
                                    scaleX = starScale
                                    scaleY = starScale
                                }
                        )
                    }
                }

                // Optional Promo Tag (Bottom-Left)
                if (item.badgeText.isNotBlank()) {
                    Surface(
                        color = LahoriBlack.copy(alpha = 0.86f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = item.badgeText.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = LahoriWhite,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (sizeOptions.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        sizeOptions.forEachIndexed { idx, opt ->
                            val selected = idx == safeIndex
                            Surface(
                                onClick = { selectedSizeIndex = idx },
                                shape = CircleShape,
                                color = if (selected) LahoriRed else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = if (selected) null else BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = opt.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = if (selected) FontWeight.Black else FontWeight.Bold
                                        ),
                                        color = if (selected) LahoriWhite else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Button(
                    onClick = { onAddToBucket(resolvedCartItem) },
                    enabled = item.available,
                    interactionSource = btnInteraction,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LahoriRed,
                        contentColor = LahoriWhite,
                        disabledContainerColor = MaterialTheme.colorScheme.outline
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .graphicsLayer {
                            scaleX = btnScale
                            scaleY = btnScale
                        }
                        .testTag("add_to_bucket_${item.id}")
                ) {
                    Text(
                        text = if (!item.available) "SOLD OUT"
                        else if (quantityInCart > 0) "IN BUCKET ($quantityInCart) +"
                        else "ADD TO BUCKET",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
