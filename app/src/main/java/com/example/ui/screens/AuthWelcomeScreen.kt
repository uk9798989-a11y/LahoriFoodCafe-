package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LahoriRed
import com.example.ui.theme.LahoriWhite

@Composable
fun AuthWelcomeScreen(
    isLoading: Boolean,
    authError: String?,
    onGoogleSignInClick: () -> Unit,
    onContinueAsGuestClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_lahori_logo),
                contentDescription = "Lahori Food Cafe Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(180.dp)
            )

            Surface(
                color = LahoriRed,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "CRISPY • SPICY • LAHORI",
                    style = MaterialTheme.typography.labelLarge,
                    color = LahoriWhite,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Text(
                text = "LAHORE'S BOLDEST CRUNCH, DELIVERED HOT",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Order Zinger Burgers, Thick-crust Extreme Pizza, Hot Wings & Signature Sandwiches with instant branch routing.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (authError != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, LahoriRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = authError,
                        style = MaterialTheme.typography.bodySmall,
                        color = LahoriRed,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Button(
                onClick = onGoogleSignInClick,
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LahoriRed,
                    contentColor = LahoriWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("google_sign_in_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = LahoriWhite,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "SIGN IN WITH GOOGLE",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp)
                    )
                }
            }

            OutlinedButton(
                onClick = onContinueAsGuestClick,
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.onBackground),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("browse_as_guest_button")
            ) {
                Text(
                    text = "BROWSE & CHECKOUT AS GUEST",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp)
                )
            }

            Text(
                text = "Guest orders use a temporary Guest ID and verify cleanly at checkout.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
