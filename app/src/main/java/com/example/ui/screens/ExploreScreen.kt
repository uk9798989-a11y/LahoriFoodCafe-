package com.example.ui.screens

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MenuItem
import com.example.model.SeedMenuData
import com.example.ui.components.FoodArtworkBox
import com.example.ui.components.MenuItemCard
import com.example.ui.theme.LahoriBlack
import com.example.ui.theme.LahoriGold
import com.example.ui.theme.LahoriRed
import com.example.ui.theme.LahoriRedDark
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
    modifier: Modifier = Modifier
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
        // 1. Large, Striking Auto-Scrolling Hero Carousel
        item(key = "hero_banner") {
            HeroPromoCarousel(
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
                        quantityInCart = cartQuantities[left.id] ?: 0,
                        onToggleFavorite = { onToggleFavorite(left.id) },
                        onAddToBucket = { onAddToBucket(left) },
                        modifier = Modifier.weight(1f)
                    )
                    if (pair.size > 1) {
                        val right = pair[1]
                        MenuItemCard(
                            item = right,
                            isFavorite = favoriteIds.contains(right.id),
                            quantityInCart = cartQuantities[right.id] ?: 0,
                            onToggleFavorite = { onToggleFavorite(right.id) },
                            onAddToBucket = { onAddToBucket(right) },
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

                    FoodArtworkBox(
                        imageKey = banner.imageKey,
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
