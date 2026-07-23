package com.example.project.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BottomBar(
    currentRoute: String?,
    onTabSelected: (TopLevelTab) -> Unit,
) {
    val currentDensity = LocalDensity.current

    // Configuration.fontScale contains Android's system font scale.
    // It does not contain Melodify's additional in-app font multiplier.
    val systemFontScale = LocalConfiguration.current.fontScale

    CompositionLocalProvider(
        LocalDensity provides Density(
            density = currentDensity.density,
            fontScale = systemFontScale,
        )
    ) {
        Column(modifier = Modifier.shadow(elevation = 16.dp)) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            )
            NavigationBar {
            TopLevelTab.entries.forEach { tab ->
                val selected = currentRoute == tab.route
                val label = stringResource(tab.labelRes)

                NavigationBarItem(
                    selected = selected,
                    onClick = { onTabSelected(tab) },
                    icon = {
                        Icon(
                            imageVector = if (selected) {
                                tab.selectedIcon
                            } else {
                                tab.unselectedIcon
                            },
                            contentDescription = label,
                        )
                    },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 0.sp,
                            ),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    alwaysShowLabel = true,
                )
            }
        }
        }
    }
}
