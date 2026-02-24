package com.personal.cinevault.di

import com.personal.cinevault.data.local.CacheDatabase
import com.personal.cinevault.data.local.MovieCacheManager
import com.personal.cinevault.data.local.PersonalDatabase
import com.personal.cinevault.data.remote.TmdbApiService
import com.personal.cinevault.data.repository.MovieRepositoryImpl
import com.personal.cinevault.domain.repository.MovieRepository
import com.personal.cinevault.domain.usecase.SearchMoviesUseCase
import com.personal.cinevault.ui.screens.search.SearchViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // ── Network ───────────────────────────────────────────────────────────────
    single<TmdbApiService> { TmdbApiService.create() }

    // ── Databases (stubs — replace with Room when ready) ──────────────────────
    single<PersonalDatabase> { PersonalDatabase.create(androidContext()) }
    single<CacheDatabase>    { CacheDatabase.create(androidContext())    }

    // ── Cache ─────────────────────────────────────────────────────────────────
    single<MovieCacheManager> { MovieCacheManager() }

    // ── Repository ────────────────────────────────────────────────────────────
    single<MovieRepository> {
        MovieRepositoryImpl(api = get(), cache = get())
    }

    // ── Use Cases ─────────────────────────────────────────────────────────────
    factory { SearchMoviesUseCase(repository = get()) }

    // ── ViewModels ────────────────────────────────────────────────────────────
    viewModel { SearchViewModel(searchMoviesUseCase = get()) }
}
