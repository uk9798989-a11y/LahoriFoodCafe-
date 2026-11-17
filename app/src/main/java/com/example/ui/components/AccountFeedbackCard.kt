package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RoomService
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.BranchDirectory
import com.example.model.CafeBranch
import com.example.model.CafeFeedback
import com.example.ui.theme.LahoriGold
import com.example.ui.theme.LahoriRed
import com.example.ui.theme.LahoriSuccess
import com.example.ui.theme.LahoriWhite

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountFeedbackCard(
    isSignedIn: Boolean,
    defaultBranchId: String,
    isSubmitting: Boolean,
    recentFeedback: List<CafeFeedback>,
    onSignInClick: () -> Unit,
    onSubmitFeedback: (
        branchId: String,
        foodRating: Int,
        serviceRating: Int,
        comment: String,
        onComplete: () -> Unit
    ) -> Unit,
    modifier: Modifier = Modifier,
    branches: List<CafeBranch> = BranchDirectory.defaultBranches
) {
    val activeBranches = remember(branches) {
        branches.filter { it.active }.ifEmpty { branches.ifEmpty { BranchDirectory.defaultBranches } }
    }
    var selectedBranchId by remember(defaultBranchId, activeBranches) {
        val valid = activeBranches.any { it.id == defaultBranchId }
        mutableStateOf(if (valid) defaultBranchId else activeBranches.first().id)
    }
    var foodRating by remember { mutableIntStateOf(5) }
    var serviceRating by remember { mutableIntStateOf(5) }
    var commentText by remember { mutableStateOf("") }
    var showSubmittedBanner by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val successAccent = if (isDark) Color(0xFF4ADE80) else LahoriSuccess

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .fillMaxWidth()
            .testTag("account_feedback_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.RateReview,
                    contentDescription = null,
                    tint = LahoriRed,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = stringResource(R.string.feedback_section_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.feedback_section_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isSignedIn) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.feedback_guest_prompt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = onSignInClick,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, LahoriRed),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LahoriRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("feedback_signin_button")
                        ) {
                            Text("SIGN IN WITH GOOGLE", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            } else {
                // Branch Selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.feedback_branch_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        activeBranches.forEach { branch ->
                            val selected = selectedBranchId == branch.id
                            val shortTitle = branch.displayShortLabel()
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    selectedBranchId = branch.id
                                    showSubmittedBanner = false
                                },
                                label = {
                                    Text(
                                        text = shortTitle,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    selectedContainerColor = LahoriRed.copy(alpha = if (isDark) 0.25f else 0.12f),
                                    selectedLabelColor = if (isDark) Color(0xFFFF8599) else LahoriRed
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = MaterialTheme.colorScheme.outline,
                                    selectedBorderColor = LahoriRed
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("feedback_branch_${branch.id}")
                            )
                        }
                    }
                }

                // Food Quality Star Rating Row
                FeedbackStarRow(
                    label = stringResource(R.string.feedback_food_label),
                    icon = Icons.Filled.Restaurant,
                    rating = foodRating,
                    tagPrefix = "feedback_food",
                    onRatingSelected = {
                        foodRating = it
                        showSubmittedBanner = false
                    }
                )

                // Service Star Rating Row
                FeedbackStarRow(
                    label = stringResource(R.string.feedback_service_label),
                    icon = Icons.Filled.RoomService,
                    rating = serviceRating,
                    tagPrefix = "feedback_service",
                    onRatingSelected = {
                        serviceRating = it
                        showSubmittedBanner = false
                    }
                )

                // Optional Comments Input
                OutlinedTextField(
                    value = commentText,
                    onValueChange = {
                        if (it.length <= 500) {
                            commentText = it
                            showSubmittedBanner = false
                        }
                    },
                    label = { Text(stringResource(R.string.feedback_comment_label)) },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.feedback_comment_placeholder),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    minLines = 2,
                    maxLines = 4,
                    supportingText = {
                        Text(
                            text = "${commentText.length}/500",
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
                        .testTag("feedback_comment_input")
                )

                AnimatedVisibility(visible = showSubmittedBanner) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = successAccent.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, successAccent.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("feedback_saved_confirmation")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = successAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.feedback_saved_banner),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = successAccent
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        onSubmitFeedback(selectedBranchId, foodRating, serviceRating, commentText.trim()) {
                            commentText = ""
                            showSubmittedBanner = true
                        }
                    },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LahoriRed,
                        contentColor = LahoriWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_cafe_feedback_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = LahoriWhite,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.feedback_submit_cta),
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp)
                        )
                    }
                }

                if (recentFeedback.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    Text(
                        text = "RECENT SUBMITTED FEEDBACK",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    recentFeedback.take(2).forEach { entry ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = entry.branchName.substringAfter("— ").trim(),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Food ${entry.foodRating}/5 ★ • Service ${entry.serviceRating}/5 ★",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = LahoriGold
                                    )
                                }
                                if (entry.comment.isNotBlank()) {
                                    Text(
                                        text = entry.comment,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackStarRow(
    label: String,
    icon: ImageVector,
    rating: Int,
    tagPrefix: String,
    onRatingSelected: (Int) -> Unit
) {
    val caption = when (rating) {
        1 -> "Poor"
        2 -> "Fair"
        3 -> "Good"
        4 -> "Great"
        else -> "Excellent"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = LahoriRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "$rating/5 • $caption",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = LahoriRed
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                (1..5).forEach { star ->
                    val selected = star <= rating
                    val scale by animateFloatAsState(
                        targetValue = if (star == rating) 1.15f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "${tagPrefix}_star_scale_$star"
                    )
                    IconButton(
                        onClick = { onRatingSelected(star) },
                        modifier = Modifier
                            .size(48.dp)
                            .scale(scale)
                            .testTag("${tagPrefix}_star_$star")
                    ) {
                        Icon(
                            imageVector = if (selected) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = "$label $star out of 5 stars",
                            tint = if (selected) LahoriGold else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(2.dp))
            }
        }
    }
}
