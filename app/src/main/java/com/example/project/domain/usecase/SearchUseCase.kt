package com.example.project.domain.usecase

import com.example.project.domain.model.SearchFilter
import com.example.project.domain.model.SearchResults
import com.example.project.domain.repository.SearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SearchUseCase(
    private val searchRepository: SearchRepository,
) {
    suspend operator fun invoke(query: String, filter: SearchFilter): SearchResults =
        withContext(Dispatchers.IO) {
            val trimmed = query.trim()
            if (trimmed.isBlank()) SearchResults()
            else searchRepository.search(trimmed, filter)
        }
}
