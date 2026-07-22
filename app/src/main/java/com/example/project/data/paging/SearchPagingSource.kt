package com.example.project.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.project.data.remote.api.CatalogApi
import com.example.project.data.remote.api.dto.toDomainArtist
import com.example.project.data.remote.api.dto.toDomainPlaylist
import com.example.project.data.remote.api.dto.toDomainSong
import com.example.project.data.remote.api.dto.toDomainUser
import com.example.project.domain.model.SearchFilter
import com.example.project.domain.model.SearchHit

/**
 * Network-backed PagingSource for Melodify `/search` (page/limit).
 */
class SearchPagingSource(
    private val catalogApi: CatalogApi,
    private val query: String,
    private val filter: SearchFilter,
) : PagingSource<Int, SearchHit>() {

    override fun getRefreshKey(state: PagingState<Int, SearchHit>): Int? {
        val anchor = state.anchorPosition ?: return null
        val page = state.closestPageToPosition(anchor) ?: return null
        return page.prevKey?.plus(1) ?: page.nextKey?.minus(1)
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SearchHit> {
        val page = params.key ?: 1
        val limit = params.loadSize.coerceAtMost(40)
        val type = when (filter) {
            SearchFilter.SONG -> "song"
            SearchFilter.ARTIST -> "artist"
            SearchFilter.PLAYLIST -> "playlist"
            SearchFilter.USER -> "user"
        }
        return try {
            val response = catalogApi.search(
                query = query.trim(),
                type = type,
                page = page,
                limit = limit,
            )
            val items: List<SearchHit> = when (filter) {
                SearchFilter.SONG -> response.songs.map { SearchHit.SongHit(it.toDomainSong()) }
                SearchFilter.ARTIST -> response.artists.map { SearchHit.ArtistHit(it.toDomainArtist()) }
                SearchFilter.PLAYLIST -> response.playlists.map {
                    SearchHit.PlaylistHit(it.toDomainPlaylist())
                }
                SearchFilter.USER -> response.users.map { SearchHit.UserHit(it.toDomainUser()) }
            }
            LoadResult.Page(
                data = items,
                prevKey = if (page <= 1) null else page - 1,
                nextKey = if (items.size < limit) null else page + 1,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
