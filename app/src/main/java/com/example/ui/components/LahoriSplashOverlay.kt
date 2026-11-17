package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LahoriSplashCream
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object SplashSessionState {
    var hasPlayedThisSession: Boolean = false
}

@Composable
fun LahoriSplashOverlay(
    modifier: Modifier = Modifier,
    onSplashFinished: () -> Unit = {}
) {
    var splashVisible by remember { mutableStateOf(!SplashSessionState.hasPlayedThisSession) }
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.82f) }

    LaunchedEffect(Unit) {
        if (!SplashSessionState.hasPlayedThisSession) {
            launch {
                logoAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
                )
            }
            launch {
                logoScale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
                )
            }
            delay(2000L)
            SplashSessionState.hasPlayedThisSession = true
            splashVisible = false
            onSplashFinished()
        } else {
            onSplashFinished()
        }
    }

    AnimatedVisibility(
        visible = splashVisible,
        exit = fadeOut(animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LahoriSplashCream)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .testTag("lahori_splash_screen"),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_splash_logo),
                contentDescription = "Lahori Food Cafe",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(240.dp)
                    .graphicsLayer {
                        alpha = logoAlpha.value
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                    }
                    .testTag("lahori_splash_logo")
            )
        }
    }
}
