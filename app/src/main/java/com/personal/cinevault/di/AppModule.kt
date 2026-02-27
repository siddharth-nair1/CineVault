package com.personal.cinevault.di

import com.google.gson.Gson
import com.personal.cinevault.data.backup.BackupManager
import com.personal.cinevault.data.backup.CineVaultPreferences
import com.personal.cinevault.data.local.CacheDatabase
import com.personal.cinevault.data.local.MovieCacheManager
import com.personal.cinevault.data.local.PersonalDatabase
import com.personal.cinevault.data.remote.TmdbApiService
import com.personal.cinevault.data.repository.CineListRepositoryImpl
import com.personal.cinevault.data.repository.LogRepositoryImpl
import com.personal.cinevault.data.repository.MovieRepositoryImpl
import com.personal.cinevault.data.repository.ReviewRepositoryImpl
import com.personal.cinevault.data.repository.WatchlistRepositoryImpl
import com.personal.cinevault.domain.repository.CineListRepository
import com.personal.cinevault.domain.repository.LogRepository
import com.personal.cinevault.domain.repository.MovieRepository
import com.personal.cinevault.domain.repository.ReviewRepository
import com.personal.cinevault.domain.repository.WatchlistRepository
import com.personal.cinevault.domain.usecase.GetMovieDetailsUseCase
import com.personal.cinevault.domain.usecase.GetTrendingUseCase
import com.personal.cinevault.domain.usecase.LogMovieUseCase
import com.personal.cinevault.domain.usecase.SaveReviewUseCase
import com.personal.cinevault.domain.usecase.SearchMoviesUseCase
import com.personal.cinevault.ui.screens.diary.DiaryViewModel
import com.personal.cinevault.ui.screens.home.HomeViewModel
import com.personal.cinevault.ui.screens.logmovie.LogMovieViewModel
import com.personal.cinevault.ui.screens.moviedetail.MovieDetailViewModel
import com.personal.cinevault.ui.screens.mylists.CreateListViewModel
import com.personal.cinevault.ui.screens.mylists.ListDetailViewModel
import com.personal.cinevault.ui.screens.mylists.MyListsViewModel
import com.personal.cinevault.ui.screens.reviews.ReviewDetailViewModel
import com.personal.cinevault.ui.screens.reviews.ReviewsViewModel
import com.personal.cinevault.ui.screens.search.SearchViewModel
import com.personal.cinevault.ui.screens.watchlist.WatchlistViewModel
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
    single { get<PersonalDatabase>().cineListDao() }
    single { get<PersonalDatabase>().listMovieDao() }
    single { get<CacheDatabase>().movieCacheDao() }

    // ── Cache ─────────────────────────────────────────────────────────────────
    single<MovieCacheManager> { MovieCacheManager(dao = get()) }

    // ── Preferences ───────────────────────────────────────────────────────────
    single { CineVaultPreferences(androidContext()) }
    single { BackupManager(androidContext()) }

    // ── Repositories ──────────────────────────────────────────────────────────
    single<MovieRepository> {
        MovieRepositoryImpl(api = get(), cache = get(), gson = get())
    }
    single<LogRepository>       { LogRepositoryImpl(dao = get()) }
    single<ReviewRepository>    { ReviewRepositoryImpl(dao = get()) }
    single<WatchlistRepository> { WatchlistRepositoryImpl(dao = get()) }
    single<CineListRepository>  { CineListRepositoryImpl(listDao = get(), listMovieDao = get()) }

    // ── Use Cases ─────────────────────────────────────────────────────────────
    factory { SearchMoviesUseCase(repository = get()) }
    factory { GetMovieDetailsUseCase(repository = get()) }
    factory { GetTrendingUseCase(repository = get()) }
    factory { LogMovieUseCase(logRepository = get()) }
    factory { SaveReviewUseCase(reviewRepository = get()) }

    // ── ViewModels ────────────────────────────────────────────────────────────
    viewModel { HomeViewModel(getTrendingUseCase = get(), logRepository = get()) }
    viewModel { SearchViewModel(searchMoviesUseCase = get(), cineListRepository = get()) }
    viewModel { (movieId: Int) ->
        MovieDetailViewModel(
            movieId = movieId,
            getMovieDetailsUseCase = get(),
            watchlistRepository = get(),
            logRepository = get()
        )
    }
    viewModel { (movieId: Int, existingEntryId: Int) ->
        LogMovieViewModel(
            movieId = movieId,
            existingEntryId = existingEntryId,
            getMovieDetailsUseCase = get(),
            logMovieUseCase = get(),
            logRepository = get(),
            saveReviewUseCase = get()
        )
    }
    viewModel { DiaryViewModel(logRepository = get()) }
    viewModel { WatchlistViewModel(watchlistRepository = get()) }
    viewModel { ReviewsViewModel(reviewRepository = get()) }
    viewModel { (reviewId: Long) -> ReviewDetailViewModel(reviewId = reviewId, reviewRepository = get()) }
    viewModel { MyListsViewModel(repository = get()) }
    viewModel { CreateListViewModel(repository = get()) }
    viewModel { (listId: Int) -> ListDetailViewModel(listId = listId, repository = get()) }
    viewModel { com.personal.cinevault.ui.screens.settings.SettingsViewModel(preferences = get(), backupManager = get()) }
}
