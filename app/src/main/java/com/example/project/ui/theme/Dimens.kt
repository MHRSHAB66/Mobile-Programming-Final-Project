package com.example.project.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * App spacing/dimension tokens. Screens use [MaterialTheme]-style access via [LocalDimens]
 * instead of hardcoding dp values, keeping spacing consistent and easy to tune.
 */
@Immutable
data class Dimens(
    val spaceXxs: Dp = 2.dp,
    val spaceXs: Dp = 4.dp,
    val spaceS: Dp = 8.dp,
    val spaceM: Dp = 12.dp,
    val spaceL: Dp = 16.dp,
    val spaceXl: Dp = 24.dp,
    val spaceXxl: Dp = 32.dp,
    val cornerSmall: Dp = 10.dp,
    val cornerMedium: Dp = 16.dp,
    val cornerLarge: Dp = 24.dp,
    val iconSmall: Dp = 18.dp,
    val iconMedium: Dp = 24.dp,
    val iconLarge: Dp = 32.dp,
    val coverSmall: Dp = 56.dp,
    val coverMedium: Dp = 120.dp,
    val coverLarge: Dp = 160.dp,
    val carouselHeight: Dp = 190.dp,
    val miniPlayerHeight: Dp = 64.dp,
    val touchTarget: Dp = 48.dp,
)

val LocalDimens = staticCompositionLocalOf { Dimens() }

@Composable
fun ProvideDimens(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDimens provides Dimens(), content = content)
}
