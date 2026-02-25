package com.personal.cinevault.di

import com.google.gson.Gson
import com.personal.cinevault.data.local.CacheDatabase
import com.personal.cinevault.data.local.MovieCacheManager
import com.personal.cinevault.data.local.PersonalDatabase
import com.personal.cinevault.data.remote.TmdbApiService
import com.personal.cinevault.data.repository.MovieRepositoryImpl
import com.personal.cinevault.domain.repository.MovieRepository
import com.personal.cinevault.domain.usecase.GetMovieDetailsUseCase
import com.personal.cinevault.domain.usecase.GetTrendingUseCase
import com.personal.cinevault.domain.usecase.SearchMoviesUseCase
import com.personal.cinevault.ui.screens.home.HomeViewModel
import com.personal.cinevault.ui.screens.moviedetail.MovieDetailViewModel
import com.personal.cinevault.ui.screens.search.SearchViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // ── Network ───────────────────────────────────────────────────────────────
    single<TmdbApiService> { TmdbApiService.create() }

    // ── JSON ──────────────────────────────────────────────────────────────────
    single<Gson> { Gson() }

    // ── Databases ─────────────────────────────────────────────────────────────
    single<PersonalDatabase> { PersonalDatabase.getInstance(androidContext()) }
    single<CacheDatabase>    { CacheDatabase.getInstance(androidContext())    }

    // ── DAOs ──────────────────────────────────────────────────────────────────
    single { get<PersonalDatabase>().logEntryDao() }
    single { get<PersonalDatabase>().reviewDao() }
    single { get<PersonalDatabase>().watchlistDao() }
    single { get<CacheDatabase>().movieCacheDao() }

    // ── Cache ─────────────────────────────────────────────────────────────────
    single<MovieCacheManager> { MovieCacheManager(dao = get()) }

    // ── Repository ────────────────────────────────────────────────────────────
    single<MovieRepository> {
        MovieRepositoryImpl(api = get(), cache = get(), gson = get())
    }

    // ── Use Cases ─────────────────────────────────────────────────────────────
    factory { SearchMoviesUseCase(repository = get()) }
    factory { GetMovieDetailsUseCase(repository = get()) }
    factory { GetTrendingUseCase(repository = get()) }

    // ── ViewModels ────────────────────────────────────────────────────────────
    viewModel { HomeViewModel(getTrendingUseCase = get()) }
    viewModel { SearchViewModel(searchMoviesUseCase = get()) }
    viewModel { (movieId: Int) ->
        MovieDetailViewModel(movieId = movieId, getMovieDetailsUseCase = get())
    }
}
