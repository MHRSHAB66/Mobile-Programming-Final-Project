package com.example.project.domain.repository

import androidx.paging.PagingData
import com.example.project.domain.model.SearchFilter
import com.example.project.domain.model.SearchHit
import com.example.project.domain.model.SearchResults
import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    suspend fun search(query: String, filter: SearchFilter): SearchResults

    /** Spec §3 — long search results must go through Paging 3. */
    fun searchPaged(query: String, filter: SearchFilter): Flow<PagingData<SearchHit>>

    fun observeHistory(): Flow<List<String>>
    suspend fun addToHistory(query: String)
    suspend fun removeFromHistory(query: String)
    suspend fun clearHistory()
}


