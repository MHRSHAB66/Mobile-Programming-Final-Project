package com.example.project.domain.repository

import com.example.project.domain.model.SearchFilter
import com.example.project.domain.model.SearchResults
import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    suspend fun search(query: String, filter: SearchFilter): SearchResults

    fun observeHistory(): Flow<List<String>>
    suspend fun addToHistory(query: String)
    suspend fun removeFromHistory(query: String)
    suspend fun clearHistory()
}
