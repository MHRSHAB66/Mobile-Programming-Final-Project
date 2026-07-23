package com.example.project.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.project.R
import com.example.project.domain.model.HomeFeed
import com.example.project.domain.model.Song
import com.example.project.ui.components.AppTopBar
import com.example.project.ui.components.ArtistCard
import com.example.project.ui.components.CoverImage
import com.example.project.ui.components.EmptyState
import com.example.project.ui.components.ErrorState
import com.example.project.ui.components.PlaylistCard
import com.example.project.ui.components.SectionHeader
import com.example.project.ui.components.ShimmerBox
import com.example.project.ui.components.SongCard
import com.example.project.ui.components.bounceClick
import com.example.project.ui.components.rememberAnimatedBrandGradient
import com.example.project.ui.navigation.TopBarActions
import com.example.project.ui.theme.BrandBlue
import com.example.project.ui.theme.BrandBlueDark
import com.example.project.ui.theme.BrandGreen
import com.example.project.ui.theme.BrandGreenDark
import com.example.project.ui.theme.BrandPink
import com.example.project.ui.theme.BrandPinkDark
import com.example.project.ui.theme.BrandViolet
import com.example.project.ui.theme.BrandVioletDark
import com.example.project.ui.theme.LocalDimens
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

private const val CAROUSEL_AUTO_SCROLL_INTERVAL_MS = 3_500L

@Composable
fun HomeScreen(
    topBar: TopBarActions,
    onPlaySong: (List<Song>, Int) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenLiked: () -> Unit,
    onOpenRecent: () -> Unit,
    onOpenPlaylistsTab: () -> Unit,
    onOpenFollowedArtists: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        AppTopBar(
            avatarUrl = topBar.avatarUrl,
            onProfileClick = topBar.onProfileClick,
            onNotificationsClick = topBar.onNotificationsClick,
            onSettingsClick = topBar.onSettingsClick,
        )
        when {
            state.isLoading && state.feed == null -> HomeShimmer(contentPadding)
            state.error != null && state.feed == null -> ErrorState(
                message = state.error!!.asString(),
                onRetry = viewModel::load,
            )
            state.feed != null -> HomeContent(
                feed = state.feed!!,
                contentPadding = contentPadding,
                onPlaySong = onPlaySong,
                onOpenPlaylist = onOpenPlaylist,
                onOpenArtist = onOpenArtist,
                onOpenLiked = onOpenLiked,
                onOpenRecent = onOpenRecent,
                onOpenPlaylistsTab = onOpenPlaylistsTab,
                onOpenFollowedArtists = onOpenFollowedArtists,
            )
            else -> EmptyState(
                icon = Icons.Filled.LibraryMusic,
                message = stringResource(R.string.empty_generic),
            )
        }
    }
}

@Composable
private fun HomeContent(
    feed: HomeFeed,
    contentPadding: PaddingValues,
    onPlaySong: (List<Song>, Int) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenLiked: () -> Unit,
    onOpenRecent: () -> Unit,
    onOpenPlaylistsTab: () -> Unit,
    onOpenFollowedArtists: () -> Unit,
) {
    val dimens = LocalDimens.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item { HomeHero() }
        item { Carousel(feed.carousel, onPlaySong) }
        item {
            QuickActions(
                onOpenLiked = onOpenLiked,
                onOpenRecent = onOpenRecent,
                onOpenPlaylistsTab = onOpenPlaylistsTab,
                onOpenFollowedArtists = onOpenFollowedArtists,
            )
        }
        songRowSection(R.string.home_section_popular, feed.mostPopular, onPlaySong)
        songRowSection(R.string.home_section_new, feed.newReleases, onPlaySong)
        item {
            Column {
                SectionHeader(stringResource(R.string.home_section_global))
                LazyRow(contentPadding = PaddingValues(horizontal = dimens.spaceM)) {
                    items(feed.globalPlaylists, key = { it.id }) { playlist ->
                        PlaylistCard(playlist = playlist, onClick = { onOpenPlaylist(playlist.id) })
                    }
                }
            }
        }
        item {
            Column {
                SectionHeader(stringResource(R.string.home_section_local))
                LazyRow(contentPadding = PaddingValues(horizontal = dimens.spaceM)) {
                    items(feed.localPlaylists, key = { it.id }) { playlist ->
                        PlaylistCard(playlist = playlist, onClick = { onOpenPlaylist(playlist.id) })
                    }
                }
            }
        }
        item {
            Column {
                SectionHeader(stringResource(R.string.home_section_artists))
                LazyRow(contentPadding = PaddingValues(horizontal = dimens.spaceM)) {
                    items(feed.topArtists, key = { it.id }) { artist ->
                        ArtistCard(artist = artist, onClick = { onOpenArtist(artist.id) })
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.songRowSection(
    titleRes: Int,
    songs: List<Song>,
    onPlaySong: (List<Song>, Int) -> Unit,
) {
    item {
        Column {
            SectionHeader(stringResource(titleRes))
            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp)) {
                itemsIndexed(songs) { index, song ->
                    SongCard(song = song, onClick = { onPlaySong(songs, index) })
                }
            }
        }
    }
}

@Composable
private fun HomeHero() {
    val dimens = LocalDimens.current
    val gradient = rememberAnimatedBrandGradient(
        listOf(BrandViolet, BrandPink, BrandBlue, BrandGreen)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.spaceL, vertical = dimens.spaceS)
            .clip(RoundedCornerShape(dimens.cornerLarge))
            .background(gradient)
            .padding(horizontal = dimens.spaceL, vertical = dimens.spaceXl),
    ) {
        Column {
            Text(
                text = stringResource(R.string.home_greeting),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.9f),
            )
            Text(
                text = stringResource(R.string.home_daily),
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun Carousel(songs: List<Song>, onPlaySong: (List<Song>, Int) -> Unit) {
    if (songs.isEmpty()) return
    val dimens = LocalDimens.current
    val pagerState = rememberPagerState(pageCount = { songs.size })

    LaunchedEffect(pagerState, songs.size) {
        if (songs.size <= 1) return@LaunchedEffect

        while (true) {
            delay(CAROUSEL_AUTO_SCROLL_INTERVAL_MS)
            if (pagerState.isScrollInProgress) continue

            val nextPage = (pagerState.currentPage + 1) % songs.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = dimens.spaceXl),
        pageSpacing = dimens.spaceM,
        modifier = Modifier
            .fillMaxWidth()
            .height(dimens.carouselHeight)
            .padding(vertical = dimens.spaceS),
    ) { page ->
        val song = songs[page]
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimens.carouselHeight)
                .clip(RoundedCornerShape(dimens.cornerLarge))
                .bounceClick(onClick = { onPlaySong(songs, page) }),
        ) {
            CoverImage(
                url = song.coverImageUrl,
                contentDescription = song.title,
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 24,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.5f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.7f),
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(dimens.spaceL),
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = song.artistName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        }
        Row(
            modifier = Modifier.padding(top = dimens.spaceS),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(songs.size) { index ->
                val selected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(if (selected) 9.dp else 7.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                )
            }
        }
    }
}

@Composable
private fun QuickActions(
    onOpenLiked: () -> Unit,
    onOpenRecent: () -> Unit,
    onOpenPlaylistsTab: () -> Unit,
    onOpenFollowedArtists: () -> Unit,
) {
    val dimens = LocalDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.spaceL, vertical = dimens.spaceS),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceS),
    ) {
        QuickActionItem(
            Icons.Filled.Favorite, stringResource(R.string.home_quick_liked),
            Brush.linearGradient(listOf(BrandPink, BrandPinkDark)), onOpenLiked, Modifier.weight(1f),
        )
        QuickActionItem(
            Icons.Filled.History, stringResource(R.string.home_quick_recent),
            Brush.linearGradient(listOf(BrandViolet, BrandVioletDark)), onOpenRecent, Modifier.weight(1f),
        )
        QuickActionItem(
            Icons.Filled.LibraryMusic, stringResource(R.string.home_quick_playlists),
            Brush.linearGradient(listOf(BrandGreen, BrandGreenDark)), onOpenPlaylistsTab, Modifier.weight(1f),
        )
        QuickActionItem(
            Icons.Filled.MusicNote, stringResource(R.string.home_quick_artists),
            Brush.linearGradient(listOf(BrandBlue, BrandBlueDark)), onOpenFollowedArtists, Modifier.weight(1f),
        )
    }
}

@Composable
private fun QuickActionItem(
    icon: ImageVector,
    label: String,
    gradient: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .bounceClick(onClick = onClick)
            .background(gradient)
            .padding(vertical = dimens.spaceL, horizontal = dimens.spaceXs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(dimens.iconMedium),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = dimens.spaceXs),
        )
    }
}

@Composable
private fun HomeShimmer(contentPadding: PaddingValues) {
    val dimens = LocalDimens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(dimens.spaceL),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceM),
    ) {
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(dimens.carouselHeight), cornerRadius = 24)
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceS)) {
            repeat(4) {
                ShimmerBox(modifier = Modifier.weight(1f).height(64.dp))
            }
        }
        repeat(2) {
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceM)) {
                repeat(3) {
                    ShimmerBox(modifier = Modifier.size(dimens.coverMedium))
                }
            }
        }
    }
}
