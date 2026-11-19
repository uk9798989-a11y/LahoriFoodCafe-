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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CafeOrder
import com.example.ui.theme.LahoriBlack
import com.example.ui.theme.LahoriGold
import com.example.ui.theme.LahoriRed
import com.example.ui.theme.LahoriRedDark
import com.example.ui.theme.LahoriSuccess
import com.example.ui.theme.LahoriWhite

data class RewardVoucher(
    val code: String,
    val title: String,
    val subtitle: String,
    val discountPkr: Long,
    val requiredPoints: Int
)

fun calculateLoyaltyPoints(orders: List<CafeOrder>): Int {
    val totalSpentPkr = orders.sumOf { it.totalPkr }
    return ((totalSpentPkr / 100L) * 5L).toInt()
}

@Composable
fun RewardsScreen(
    orders: List<CafeOrder>,
    appliedPromoLabel: String,
    onApplyVoucher: (title: String, discountPkr: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val earnedPoints = calculateLoyaltyPoints(orders)
    val tier = when {
        earnedPoints >= 1000 -> "LAHORI LEGEND"
        earnedPoints >= 500 -> "SPICE MASTER"
        else -> "CRUNCH ROOKIE"
    }
    val nextTierPoints = if (earnedPoints >= 1000) 1500 else if (earnedPoints >= 500) 1000 else 500
    val progress = (earnedPoints.toFloat() / nextTierPoints.toFloat()).coerceIn(0f, 1f)

    val vouchers = listOf(
        RewardVoucher("WELCOME100", "Rs. 100 Off Bucket", "Unlock at 100 pts (Rs. 2,000 spent across orders)", 100L, 100),
        RewardVoucher("CRUNCH200", "Rs. 200 Off Feast", "Unlock at 300 Lahori Crunch Points", 200L, 300),
        RewardVoucher("KING350", "Rs. 350 Royal Discount", "Unlock at 600 Lahori Crunch Points", 350L, 600)
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "rewards_hero") {
            Surface(
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(LahoriBlack, LahoriRedDark, LahoriRed)))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "LAHORI REWARDS",
                                style = MaterialTheme.typography.labelLarge,
                                color = LahoriGold
                            )
                            Text(
                                text = "$earnedPoints PTS",
                                style = MaterialTheme.typography.displayLarge,
                                color = LahoriWhite
                            )
                        }
                        Surface(
                            color = LahoriWhite.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Filled.Stars, contentDescription = null, tint = LahoriGold, modifier = Modifier.size(18.dp))
                                Text(tier, style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp), color = LahoriWhite)
                            }
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        color = LahoriGold,
                        trackColor = LahoriWhite.copy(alpha = 0.25f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Text(
                        text = if (earnedPoints == 0) {
                            "Order to earn 5 pts per Rs. 100 spent • $nextTierPoints pts to next tier"
                        } else {
                            "Earn 5 points per Rs. 100 ordered • ${nextTierPoints - earnedPoints.coerceAtMost(nextTierPoints)} pts to next tier"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = LahoriWhite.copy(alpha = 0.85f)
                    )
                }
            }
        }

        item(key = "vouchers_heading") {
            Text(
                text = "REDEEMABLE VOUCHERS",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        items(vouchers, key = { it.code }) { voucher ->
            val unlocked = earnedPoints >= voucher.requiredPoints
            val isApplied = appliedPromoLabel == voucher.code
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, if (isApplied) LahoriSuccess else MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalOffer,
                            contentDescription = null,
                            tint = if (unlocked) LahoriRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = voucher.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = voucher.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Requires ${voucher.requiredPoints} pts • Code: ${voucher.code}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (unlocked) LahoriRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.size(8.dp))

                    Button(
                        onClick = { onApplyVoucher(voucher.code, voucher.discountPkr) },
                        enabled = unlocked && !isApplied,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isApplied) LahoriSuccess else LahoriRed,
                            contentColor = LahoriWhite,
                            disabledContainerColor = MaterialTheme.colorScheme.outline,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Text(
                            text = if (isApplied) "APPLIED" else if (unlocked) "APPLY" else "LOCKED",
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp)
                        )
                    }
                }
            }
        }
    }
}
