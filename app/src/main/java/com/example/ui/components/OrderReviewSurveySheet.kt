package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CafeOrder
import com.example.model.OrderReview
import com.example.ui.theme.LahoriGold
import com.example.ui.theme.LahoriRed
import com.example.ui.theme.LahoriSuccess
import com.example.ui.theme.LahoriWhite

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OrderReviewSurveySheet(
    order: CafeOrder,
    existingReview: OrderReview?,
    isSubmitting: Boolean,
    onSubmit: (rating: Int, comment: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedRating by remember(order.id, existingReview) {
        mutableIntStateOf((existingReview?.rating?.toInt() ?: 5).coerceIn(1, 5))
    }
    var commentText by remember(order.id, existingReview) {
        mutableStateOf(existingReview?.comment ?: "")
    }

    val quickHighlights = listOf(
        "Hot & Crispy",
        "Fast Delivery",
        "Bold Lahori Spice",
        "Fresh & Tasty"
    )

    val ratingCaption = when (selectedRating) {
        1 -> "Needs Improvement"
        2 -> "Below Expectations"
        3 -> "It Was Okay"
        4 -> "Really Good!"
        else -> "Lahori Perfection!"
    }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val deliveredAccent = if (isDark) Color(0xFF4ADE80) else LahoriSuccess

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = deliveredAccent.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = deliveredAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "DELIVERED • #${order.id.takeLast(6).uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = deliveredAccent
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("dismiss_review_x_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close feedback survey",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (existingReview != null) "UPDATE YOUR RATING" else "HOW WAS YOUR FEAST?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${order.branchName} • ${order.itemsSummary}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Star Rating Selector
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (1..5).forEach { star ->
                            val selected = star <= selectedRating
                            val scale by animateFloatAsState(
                                targetValue = if (star == selectedRating) 1.18f else 1.0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "star_scale_$star"
                            )
                            IconButton(
                                onClick = { selectedRating = star },
                                modifier = Modifier
                                    .size(48.dp)
                                    .scale(scale)
                                    .testTag("review_star_$star")
                            ) {
                                Icon(
                                    imageVector = if (selected) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                    contentDescription = "Rate $star out of 5 stars",
                                    tint = if (selected) LahoriGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "$selectedRating / 5 — $ratingCaption",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isDark) Color(0xFFFF6B81) else LahoriRed,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Quick Highlight Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "QUICK HIGHLIGHTS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickHighlights.forEach { tag ->
                        val isIncluded = commentText.contains(tag, ignoreCase = true)
                        FilterChip(
                            selected = isIncluded,
                            onClick = {
                                commentText = if (isIncluded) {
                                    commentText.replace(tag, "")
                                        .replace(" • ", " ")
                                        .trim(' ', '•', ',')
                                } else {
                                    val prefix = if (commentText.isBlank()) "" else "${commentText.trim()} • "
                                    (prefix + tag).take(400)
                                }
                            },
                            label = {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // Short Comment Field
            OutlinedTextField(
                value = commentText,
                onValueChange = { if (it.length <= 400) commentText = it },
                label = { Text("Short comment (optional)") },
                placeholder = {
                    Text(
                        text = "Tell us about crunch, spice level, or delivery speed...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                minLines = 2,
                maxLines = 4,
                supportingText = {
                    Text(
                        text = "${commentText.length}/400",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LahoriRed,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = if (isDark) Color(0xFFFF8599) else LahoriRed,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("review_comment_input")
            )

            Button(
                onClick = { onSubmit(selectedRating, commentText.trim()) },
                enabled = !isSubmitting,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LahoriRed,
                    contentColor = LahoriWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_review_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = LahoriWhite,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (existingReview != null) "UPDATE FEEDBACK" else "SUBMIT FEEDBACK",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp)
                    )
                }
            }

            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("dismiss_review_button")
            ) {
                Text(
                    text = "Maybe Later",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}
