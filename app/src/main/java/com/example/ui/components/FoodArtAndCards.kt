package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MenuItem
import com.example.ui.theme.LahoriBlack
import com.example.ui.theme.LahoriGold
import com.example.ui.theme.LahoriRed
import com.example.ui.theme.LahoriWhite

fun formatPkr(amount: Long): String {
    val formatted = "%,d".format(amount)
    return "Rs. $formatted"
}

@Composable
fun FoodArtworkBox(
    imageKey: String,
    modifier: Modifier = Modifier
) {
    val (colors, icon, accentTint) = remember(imageKey) {
        when (imageKey) {
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
            shadowElevation = 10.dp,
            modifier = Modifier.size(78.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentTint,
                    modifier = Modifier.size(44.dp)
                )
            }
        }
    }
}

@Composable
fun MenuItemCard(
    item: MenuItem,
    isFavorite: Boolean,
    quantityInCart: Int,
    onToggleFavorite: () -> Unit,
    onAddToBucket: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                onClick = onAddToBucket
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
                    .height(144.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                FoodArtworkBox(
                    imageKey = item.imageKey,
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
                        text = formatPkr(item.pricePkr),
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

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onAddToBucket,
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
