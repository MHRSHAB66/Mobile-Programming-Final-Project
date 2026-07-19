@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.example.project.ui.nowplaying

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Shared-element plumbing for the MiniPlayer → Now Playing cover transition (spec §5 "Shared
 * Element Transitions").
 *
 * The single [SharedTransitionScope] created by the `SharedTransitionLayout` in `MainScreen` is
 * published here so both cover composables can read it without threading it through every
 * navigation signature. Each cover still supplies its own [AnimatedVisibilityScope]: the
 * MiniPlayer from its `AnimatedVisibility` wrapper, Now Playing from its nav `composable` scope.
 * When one exits while the other enters (the same navigation event), the cover animates between
 * the small mini-player thumbnail and the large Now Playing disc.
 */
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/** Stable key that pairs the two cover composables so the framework tweens between them. */
private const val PLAYER_COVER_KEY = "player-cover"

/**
 * Marks a cover as the shared player-cover element. No-ops (returns the modifier unchanged) when
 * either scope is missing, so the covers render normally if the transition host isn't present.
 */
@Composable
fun Modifier.playerCoverSharedBounds(animatedVisibilityScope: AnimatedVisibilityScope?): Modifier {
    val sharedScope = LocalSharedTransitionScope.current
    if (sharedScope == null || animatedVisibilityScope == null) return this
    return with(sharedScope) {
        this@playerCoverSharedBounds.sharedBounds(
            sharedContentState = rememberSharedContentState(key = PLAYER_COVER_KEY),
            animatedVisibilityScope = animatedVisibilityScope,
        )
    }
}
