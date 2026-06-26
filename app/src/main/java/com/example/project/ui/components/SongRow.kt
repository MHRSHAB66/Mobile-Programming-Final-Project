package com.example.project.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.project.R
import com.example.project.domain.model.Song
import com.example.project.ui.theme.LocalDimens

/**
 * Reusable song list item: cover, title/artist, a now-playing equalizer marker, an optional
 * like toggle and an optional trailing slot (download button, drag handle, etc.).
 */
@Composable
fun SongRow(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCurrent: Boolean = false,
    onToggleLike: ((Song) -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val dimens = LocalDimens.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick)
            .padding(horizontal = dimens.spaceL, vertical = dimens.spaceS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverImage(
            url = song.coverImageUrl,
            contentDescription = stringResource(R.string.cd_cover_art),
            modifier = Modifier.size(dimens.coverSmall),
            cornerRadius = 8,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = dimens.spaceM),
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artistName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isCurrent) {
            Icon(
                imageVector = Icons.Filled.Equalizer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(dimens.iconMedium),
            )
        }
        if (onToggleLike != null) {
            LikeButton(
                isLiked = song.isLiked,
                onToggle = { onToggleLike(song) },
            )
        }
        trailing?.invoke(this)
    }
}
