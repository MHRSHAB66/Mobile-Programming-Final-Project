package com.example.project.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.project.R
import com.example.project.core.util.asCompactCount
import com.example.project.domain.model.Artist
import com.example.project.domain.model.Playlist
import com.example.project.domain.model.Song
import com.example.project.ui.theme.LocalDimens

/** Square song card used in LazyRow sections (Most Popular, New Releases, …). */
@Composable
fun SongCard(song: Song, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dimens = LocalDimens.current
    Column(
        modifier = modifier
            .width(dimens.coverMedium)
            .bounceClick(onClick = onClick)
            .padding(dimens.spaceXs),
    ) {
        CoverImage(
            url = song.coverImageUrl,
            contentDescription = stringResource(R.string.cd_cover_art),
            modifier = Modifier.size(dimens.coverMedium),
        )
        Text(
            text = song.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = dimens.spaceS),
        )
        Text(
            text = song.artistName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    coverSize: Int = 120,
) {
    val dimens = LocalDimens.current
    Column(
        modifier = modifier
            .width(coverSize.dp)
            .bounceClick(onClick = onClick)
            .padding(dimens.spaceXs),
    ) {
        CoverImage(
            url = playlist.coverImageUrl,
            contentDescription = playlist.title,
            modifier = Modifier.size(coverSize.dp),
        )
        Text(
            text = playlist.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = dimens.spaceS),
        )
        Text(
            text = playlist.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ArtistCard(artist: Artist, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dimens = LocalDimens.current
    Column(
        modifier = modifier
            .width(dimens.coverMedium)
            .bounceClick(onClick = onClick)
            .padding(dimens.spaceXs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircleImage(
            url = artist.imageUrl,
            contentDescription = artist.name,
            sizeDp = 110,
        )
        Text(
            text = artist.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimens.spaceS),
        )
        Text(
            text = stringResource(R.string.followers_count, artist.followers.asCompactCount()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}
