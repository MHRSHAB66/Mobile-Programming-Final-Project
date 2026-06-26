package com.example.project.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.project.R
import com.example.project.ui.theme.LocalDimens

/** Attractive gradient header with title, count and Play-all / Shuffle actions. */
@Composable
fun LibraryHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    songCount: Int,
    onPlayAll: () -> Unit,
    onShuffle: (() -> Unit)? = null,
) {
    val dimens = LocalDimens.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background,
                    )
                )
            )
            .padding(dimens.spaceL),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(dimens.iconLarge),
                )
            }
            Column(modifier = Modifier.padding(start = dimens.spaceM)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.songs_count, songCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimens.spaceM),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
        ) {
            Button(onClick = onPlayAll, modifier = Modifier.weight(1f), enabled = songCount > 0) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text(stringResource(R.string.play_all), modifier = Modifier.padding(start = dimens.spaceXs))
            }
            if (onShuffle != null) {
                OutlinedButton(onClick = onShuffle, modifier = Modifier.weight(1f), enabled = songCount > 0) {
                    Icon(Icons.Filled.Shuffle, contentDescription = null)
                    Text(stringResource(R.string.shuffle), modifier = Modifier.padding(start = dimens.spaceXs))
                }
            }
        }
    }
}
