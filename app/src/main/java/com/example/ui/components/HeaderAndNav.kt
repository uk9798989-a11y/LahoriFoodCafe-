package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.BranchDirectory
import com.example.model.OrderType
import com.example.ui.theme.LahoriRed
import com.example.ui.theme.LahoriWhite

enum class CafeTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    Explore("Explore", Icons.Filled.Explore, Icons.Outlined.Explore),
    Menu("Menu", Icons.Filled.RestaurantMenu, Icons.Outlined.RestaurantMenu),
    Rewards("Rewards", Icons.Filled.Stars, Icons.Outlined.Stars),
    Orders("Orders", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
    Account("Account", Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun LahoriTopHeader(
    orderType: OrderType,
    selectedBranchId: String,
    selectedAddressStreet: String,
    searchVisible: Boolean,
    searchQuery: String,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onDeliveryClick: () -> Unit,
    onPickupClick: () -> Unit
) {
    val activeBranch = BranchDirectory.byId(selectedBranchId)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Top Row: Logo on left, Search icon on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_lahori_logo),
                        contentDescription = "Lahori Food Cafe logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .height(42.dp)
                            .width(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Column {
                        Text(
                            text = "LAHORI FOOD CAFE",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = LahoriRed,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (orderType == OrderType.DELIVERY && selectedAddressStreet.isNotBlank()) {
                                    "${activeBranch.id.uppercase()} • $selectedAddressStreet"
                                } else {
                                    activeBranch.name
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onToggleSearch,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("header_search_button")
                ) {
                    Icon(
                        imageVector = if (searchVisible) Icons.Filled.Close else Icons.Filled.Search,
                        contentDescription = if (searchVisible) "Close search" else "Search menu",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            AnimatedVisibility(visible = searchVisible) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = "Search Pasta, Zinger, Pizza, Deals...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = LahoriRed)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LahoriRed,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = LahoriRed
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .testTag("header_search_input")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Two Distinct Toggle Buttons: DELIVERY and PICKUP
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val isDelivery = orderType == OrderType.DELIVERY
                val isPickup = orderType == OrderType.PICKUP

                if (isDelivery) {
                    Button(
                        onClick = onDeliveryClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LahoriRed,
                            contentColor = LahoriWhite
                        ),
                        contentPadding = PaddingValues(vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("toggle_delivery")
                    ) {
                        Icon(Icons.Filled.DeliveryDining, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("DELIVERY", style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    OutlinedButton(
                        onClick = onDeliveryClick,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurface),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("toggle_delivery")
                    ) {
                        Icon(Icons.Filled.DeliveryDining, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("DELIVERY", style = MaterialTheme.typography.labelLarge)
                    }
                }

                if (isPickup) {
                    Button(
                        onClick = onPickupClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LahoriRed,
                            contentColor = LahoriWhite
                        ),
                        contentPadding = PaddingValues(vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("toggle_pickup")
                    ) {
                        Icon(Icons.Filled.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PICKUP", style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    OutlinedButton(
                        onClick = onPickupClick,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurface),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("toggle_pickup")
                    ) {
                        Icon(Icons.Filled.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PICKUP", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun LahoriBottomBar(
    currentTab: CafeTab,
    activeOrderCount: Int,
    onTabSelected: (CafeTab) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 10.dp
    ) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets(0, 0, 0, 0)
            ) {
                CafeTab.entries.forEach { tab ->
                    val selected = tab == currentTab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onTabSelected(tab) },
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
                        icon = {
                            Box {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(24.dp)
                                )
                                if (tab == CafeTab.Orders && activeOrderCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(LahoriRed)
                                    )
                                }
                            }
                        },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                ),
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = LahoriRed,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingBucketFab(
    itemCount: Int,
    totalPkr: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = LahoriRed,
        contentColor = LahoriWhite,
        shadowElevation = 8.dp,
        modifier = modifier
            .height(54.dp)
            .testTag("floating_bucket_fab")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 18.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.LocalMall,
                    contentDescription = "Bucket Cart",
                    tint = LahoriWhite,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = if (itemCount == 0) "BUCKET" else "$itemCount ITEM${if (itemCount > 1) "S" else ""}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = LahoriWhite.copy(alpha = 0.9f)
                )
                Text(
                    text = formatPkr(totalPkr),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp),
                    color = LahoriWhite
                )
            }
        }
    }
}
