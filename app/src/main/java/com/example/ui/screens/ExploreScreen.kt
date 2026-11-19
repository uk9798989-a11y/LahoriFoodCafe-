package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BranchBanner
import com.example.model.MenuItem
import com.example.model.Promotion
import com.example.model.SeedMenuData
import com.example.ui.components.FoodArtworkBox
import com.example.ui.components.MenuItemCard
import com.example.ui.components.formatPkr
import com.example.ui.theme.LahoriBlack
import com.example.ui.theme.LahoriGold
import com.example.ui.theme.LahoriRed
import com.example.ui.theme.LahoriRedDark
import com.example.ui.theme.LahoriSuccess
import com.example.ui.theme.LahoriWhite
import kotlinx.coroutines.delay

@Composable
fun ExploreScreen(
    menuItems: List<MenuItem>,
    favoriteIds: Set<String>,
    cartQuantities: Map<String, Int>,
    searchQuery: String,
    onToggleFavorite: (String) -> Unit,
    onAddToBucket: (MenuItem) -> Unit,
    modifier: Modifier = Modifier,
    selectedBranchLabel: String = "",
    branchBanners: List<BranchBanner> = emptyList(),
    branchPromos: List<Promotion> = emptyList(),
    appliedPromoCode: String = "",
    onApplyBranchPromo: (String) -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf("ALL") }

    val filteredItems = remember(menuItems, selectedCategory, searchQuery) {
        menuItems.filter { item ->
            val matchesCat = selectedCategory == "ALL" || item.category.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true) ||
                item.category.contains(searchQuery, ignoreCase = true)
            matchesCat && matchesSearch
        }
    }

    val pairedRows = remember(filteredItems) {
        filteredItems.chunked(2)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 0. Branch-Exclusive Seasonal Banners (ONLY shown for customers who selected this branch)
        if (branchBanners.isNotEmpty()) {
            item(key = "branch_seasonal_banners") {
                BranchSeasonalBannersCarousel(
                    branchLabel = selectedBranchLabel.ifBlank { "Branch" },
                    banners = branchBanners,
                    onSelectCategory = { category -> selectedCategory = category },
                    onApplyPromoCode = onApplyBranchPromo
                )
            }
        }

        // 0b. Branch-Exclusive Temporary Discount Codes
        if (branchPromos.isNotEmpty()) {
            item(key = "branch_exclusive_promos") {
                BranchExclusivePromosRow(
                    branchLabel = selectedBranchLabel.ifBlank { "Branch" },
                    promos = branchPromos,
                    appliedPromoCode = appliedPromoCode,
                    onApplyPromo = onApplyBranchPromo
                )
            }
        }

        // 1. Large, Striking Auto-Scrolling Hero Carousel
        item(key = "hero_banner") {
            HeroPromoCarousel(
                menuItems = menuItems,
                onBannerClick = { category ->
                    selectedCategory = category
                }
            )
        }

        // 2. Modern Pill-Shaped Horizontal Categories (No visible scrollbars)
        item(key = "categories_row") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "EXPLORE MENU",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("horizontal_categories_list")
                ) {
                    items(SeedMenuData.categories) { category ->
                        val isSelected = selectedCategory == category
                        Surface(
                            onClick = { selectedCategory = category },
                            shape = CircleShape,
                            color = if (isSelected) LahoriRed else MaterialTheme.colorScheme.surfaceContainerHigh,
                            shadowElevation = if (isSelected) 4.dp else 0.dp,
                            modifier = Modifier
                                .height(42.dp)
                                .testTag("category_chip_${category.lowercase().replace(" ", "_")}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 18.dp)
                            ) {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
                                    ),
                                    color = if (isSelected) LahoriWhite else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Section Header for 2-Column Food Grid
        item(key = "menu_section_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedCategory == "ALL") "HOT & CRISPY FAVORITES" else selectedCategory,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${filteredItems.size} ITEMS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (pairedRows.isEmpty()) {
            item(key = "empty_explore") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No matching items found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try another category or clear your search filter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(pairedRows) { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val left = pair[0]
                    MenuItemCard(
                        item = left,
                        isFavorite = favoriteIds.contains(left.id),
                        cartQuantities = cartQuantities,
                        onToggleFavorite = { onToggleFavorite(left.id) },
                        onAddToBucket = onAddToBucket,
                        modifier = Modifier.weight(1f)
                    )
                    if (pair.size > 1) {
                        val right = pair[1]
                        MenuItemCard(
                            item = right,
                            isFavorite = favoriteIds.contains(right.id),
                            cartQuantities = cartQuantities,
                            onToggleFavorite = { onToggleFavorite(right.id) },
                            onAddToBucket = onAddToBucket,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroPromoCarousel(
    menuItems: List<MenuItem>,
    onBannerClick: (String) -> Unit
) {
    val banners = SeedMenuData.promoBanners
    val initialPage = remember { (Int.MAX_VALUE / 2) - ((Int.MAX_VALUE / 2) % banners.size) }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { Int.MAX_VALUE })

    LaunchedEffect(pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            while (true) {
                delay(3000L)
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }

    val activeDot = Math.floorMod(pagerState.currentPage, banners.size)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 14.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .testTag("hero_promo_carousel")
        ) { virtualPage ->
            val banner = banners[Math.floorMod(virtualPage, banners.size)]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = Color.Black.copy(alpha = 0.25f),
                        spotColor = LahoriRed.copy(alpha = 0.25f)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF141414), Color(0xFF4A0713), LahoriRedDark, LahoriRed)
                        )
                    )
                    .clickable { onBannerClick(banner.targetCategory) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = LahoriGold,
                            shape = CircleShape
                        ) {
                            Text(
                                text = banner.eyebrow,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = LahoriBlack,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = banner.title,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = LahoriWhite,
                            maxLines = 2
                        )
                        Text(
                            text = banner.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = LahoriWhite.copy(alpha = 0.88f),
                            maxLines = 2
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = LahoriWhite,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = banner.priceTag,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = LahoriRed,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = "ORDER NOW →",
                                style = MaterialTheme.typography.labelMedium,
                                color = LahoriWhite
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    val bannerPhotoUrl = remember(menuItems, banner.targetCategory) {
                        menuItems.firstOrNull {
                            it.category.equals(banner.targetCategory, ignoreCase = true) &&
                                it.imageUrl.isNotBlank()
                        }?.imageUrl.orEmpty()
                    }
                    FoodArtworkBox(
                        imageKey = banner.imageKey,
                        imageUrl = bannerPhotoUrl,
                        contentDescription = banner.title,
                        modifier = Modifier
                            .size(124.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
            }
        }

        // Pagination Dots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            banners.indices.forEach { idx ->
                val active = idx == activeDot
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .height(8.dp)
                        .width(if (active) 24.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (active) LahoriRed else MaterialTheme.colorScheme.outline)
                )
            }
        }
    }
}

@Composable
private fun BranchSeasonalBannersCarousel(
    branchLabel: String,
    banners: List<BranchBanner>,
    onSelectCategory: (String) -> Unit,
    onApplyPromoCode: (String) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { banners.size })

    LaunchedEffect(banners.size, pagerState.isScrollInProgress) {
        if (banners.size > 1 && !pagerState.isScrollInProgress) {
            while (true) {
                delay(4500L)
                val nextPage = (pagerState.currentPage + 1) % banners.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("branch_seasonal_banners_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Campaign,
                    contentDescription = null,
                    tint = LahoriRed,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "${branchLabel.uppercase()} SEASONAL SPECIALS",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Surface(
                shape = CircleShape,
                color = LahoriGold.copy(alpha = 0.22f),
                border = BorderStroke(1.dp, LahoriGold)
            ) {
                Text(
                    text = "BRANCH EXCLUSIVE",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(196.dp)
        ) { page ->
            val banner = banners[page]
            val expiryLabel = formatPromoExpiryLabel(banner.expiresAt)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(18.dp),
                        ambientColor = Color.Black.copy(alpha = 0.25f),
                        spotColor = LahoriRed.copy(alpha = 0.28f)
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                        onSelectCategory(banner.targetCategory.ifBlank { "ALL" })
                    }
                    .testTag("branch_seasonal_banner_${banner.id}")
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
                                    Color.Black.copy(alpha = 0.30f),
                                    Color.Black.copy(alpha = 0.45f),
                                    Color.Black.copy(alpha = 0.88f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = LahoriGold
                        ) {
                            Text(
                                text = banner.badgeText.ifBlank { "SEASONAL" }.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = LahoriBlack,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        if (banner.expiresAt != null) {
                            Surface(
                                shape = CircleShape,
                                color = LahoriBlack.copy(alpha = 0.72f),
                                border = BorderStroke(1.dp, LahoriWhite.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Schedule,
                                        contentDescription = null,
                                        tint = LahoriGold,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = expiryLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LahoriWhite
                                    )
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = banner.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp
                            ),
                            color = LahoriWhite,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (banner.subtitle.isNotBlank()) {
                            Text(
                                text = banner.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = LahoriWhite.copy(alpha = 0.88f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (banner.promoCode.isNotBlank()) {
                                Surface(
                                    onClick = { onApplyPromoCode(banner.promoCode) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = LahoriRed,
                                    modifier = Modifier.testTag("banner_apply_code_${banner.promoCode}")
                                ) {
                                    Text(
                                        text = "USE CODE: ${banner.promoCode}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                        color = LahoriWhite,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (banner.targetCategory == "ALL") "VIEW MENU →" else "VIEW ${banner.targetCategory} →",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = LahoriGold
                            )
                        }
                    }
                }
            }
        }

        if (banners.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                banners.indices.forEach { idx ->
                    val active = idx == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(7.dp)
                            .width(if (active) 22.dp else 7.dp)
                            .clip(CircleShape)
                            .background(if (active) LahoriGold else MaterialTheme.colorScheme.outline)
                    )
                }
            }
        }
    }
}

@Composable
private fun BranchExclusivePromosRow(
    branchLabel: String,
    promos: List<Promotion>,
    appliedPromoCode: String,
    onApplyPromo: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("branch_exclusive_promos_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.LocalOffer,
                contentDescription = null,
                tint = LahoriRed,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "${branchLabel.uppercase()} PROMO CODES",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(promos, key = { it.id }) { promo ->
                val isApplied = appliedPromoCode.equals(promo.code, ignoreCase = true)
                val discountLabel = if (promo.discountType.equals("PERCENT", ignoreCase = true)) {
                    "${promo.discountValue}% OFF"
                } else {
                    "${formatPkr(promo.discountValue)} OFF"
                }
                val expiryText = formatPromoExpiryLabel(promo.expiresAt)

                Surface(
                    onClick = { onApplyPromo(promo.code) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isApplied) LahoriSuccess.copy(alpha = 0.12f) else LahoriBlack,
                    border = BorderStroke(
                        1.5.dp,
                        if (isApplied) LahoriSuccess else LahoriGold
                    ),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .width(214.dp)
                        .testTag("explore_branch_promo_${promo.code}")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isApplied) LahoriSuccess else LahoriRed
                            ) {
                                Text(
                                    text = discountLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                    color = LahoriWhite,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                            Text(
                                text = expiryText,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isApplied) LahoriSuccess else LahoriGold
                            )
                        }

                        Text(
                            text = promo.code,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = if (isApplied) MaterialTheme.colorScheme.onSurface else LahoriWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (promo.minOrderPkr > 0L) "Min ${formatPkr(promo.minOrderPkr)}" else "No min order",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isApplied) MaterialTheme.colorScheme.onSurfaceVariant else LahoriWhite.copy(alpha = 0.78f)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                if (isApplied) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = LahoriSuccess,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = if (isApplied) "APPLIED" else "TAP TO APPLY",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = if (isApplied) LahoriSuccess else LahoriGold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

