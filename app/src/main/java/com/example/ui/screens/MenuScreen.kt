package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MenuItem
import com.example.model.SeedMenuData
import com.example.ui.components.MenuItemCard
import com.example.ui.theme.LahoriGold
import com.example.ui.theme.LahoriRed
import com.example.ui.theme.LahoriWhite

@Composable
fun MenuScreen(
    menuItems: List<MenuItem>,
    favoriteIds: Set<String>,
    cartQuantities: Map<String, Int>,
    searchQuery: String,
    onToggleFavorite: (String) -> Unit,
    onAddToBucket: (MenuItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("ALL") }
    var favoritesOnly by remember { mutableStateOf(false) }

    val filtered = remember(menuItems, selectedCategory, favoritesOnly, searchQuery) {
        menuItems.filter { item ->
            val catOk = selectedCategory == "ALL" || item.category.equals(selectedCategory, ignoreCase = true)
            val favOk = !favoritesOnly || favoriteIds.contains(item.id)
            val searchOk = searchQuery.isBlank() ||
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true)
            catOk && favOk && searchOk
        }
    }

    val pairedRows = remember(filtered) { filtered.chunked(2) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "menu_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "FULL MENU",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Live-synced prices in PKR • Freshly fried to order",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilterChip(
                        selected = favoritesOnly,
                        onClick = { favoritesOnly = !favoritesOnly },
                        label = { Text("Favorites", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (favoritesOnly) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = null,
                                tint = if (favoritesOnly) LahoriGold else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                            selectedLabelColor = MaterialTheme.colorScheme.background
                        )
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(SeedMenuData.categories) { category ->
                        val selected = selectedCategory == category
                        Surface(
                            onClick = { selectedCategory = category },
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = if (selected) LahoriRed else MaterialTheme.colorScheme.surfaceContainerHigh,
                            shadowElevation = if (selected) 4.dp else 0.dp,
                            modifier = Modifier.height(40.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                                    color = if (selected) LahoriWhite else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        if (pairedRows.isEmpty()) {
            item(key = "empty_menu") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (favoritesOnly) "No favorite items yet" else "No menu items match",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (favoritesOnly) "Tap the star icon on any food card to save it here."
                        else "Try selecting ALL categories or clearing search.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(pairedRows) { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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
