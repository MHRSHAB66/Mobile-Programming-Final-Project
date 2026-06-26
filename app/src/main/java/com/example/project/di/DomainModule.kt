package com.example.project.di

import com.example.project.domain.usecase.DownloadSongUseCase
import com.example.project.domain.usecase.GetHomeFeedUseCase
import com.example.project.domain.usecase.PlaySongsUseCase
import com.example.project.domain.usecase.SearchUseCase
import com.example.project.domain.usecase.ToggleLikeUseCase
import com.example.project.domain.usecase.UpgradeToPremiumUseCase
import org.koin.dsl.module

/** Use cases — stateless, created per injection. */
val domainModule = module {
    factory { GetHomeFeedUseCase(get(), get()) }
    factory { SearchUseCase(get()) }
    factory { ToggleLikeUseCase(get()) }
    factory { PlaySongsUseCase(get(), get(), get()) }
    factory { DownloadSongUseCase(get(), get()) }
    factory { UpgradeToPremiumUseCase(get()) }
}
