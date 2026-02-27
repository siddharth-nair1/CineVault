package com.personal.cinevault.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.personal.cinevault.ui.screens.diary.DiaryScreen
import com.personal.cinevault.ui.screens.home.HomeScreen
import com.personal.cinevault.ui.screens.LogFilmScreen
import com.personal.cinevault.ui.screens.ProfileScreen
import com.personal.cinevault.ui.screens.watchlist.WatchlistScreen
import com.personal.cinevault.ui.screens.moviedetail.MovieDetailScreen
import com.personal.cinevault.ui.screens.mylists.CreateListScreen
import com.personal.cinevault.ui.screens.mylists.ListDetailScreen
import com.personal.cinevault.ui.screens.mylists.MyListsScreen
import com.personal.cinevault.ui.screens.reviews.ReviewDetailScreen
import com.personal.cinevault.ui.screens.reviews.ReviewsScreen
import com.personal.cinevault.ui.screens.search.SearchScreen
import com.personal.cinevault.ui.screens.settings.SettingsScreen
import com.personal.cinevault.ui.screens.stats.StatsScreen

import androidx.compose.ui.Modifier

@Composable
fun NavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {
        // ── Bottom nav destinations ────────────────────────────────────────────
        composable("home") {
            HomeScreen(navController)
        }
        // Normal search — no addToListId
        composable("search") {
            SearchScreen(navController)
        }
        composable("diary") {
            DiaryScreen(
                onEditLog = { logEntryId, tmdbMovieId ->
                    navController.navigate("log/$tmdbMovieId?existingEntryId=$logEntryId")
                },
                onSearchClick = { navController.navigate("search") }
            )
        }
        composable("watchlist") {
            WatchlistScreen(navController)
        }
        composable("profile") {
            ProfileScreen(navController)
        }

        // ── My Lists destinations ──────────────────────────────────────────────
        composable("lists") {
            MyListsScreen(navController)
        }
        composable("create_list") {
            CreateListScreen(navController)
        }
        composable(
            route = "list/{id}",
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStack ->
            val listId = backStack.arguments!!.getInt("id")
            ListDetailScreen(listId = listId, navController = navController)
        }

        // ── Search in "add to list" mode: search?addToListId={id} ─────────────
        composable(
            route = "search?addToListId={addToListId}",
            arguments = listOf(
                navArgument("addToListId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStack ->
            val raw = backStack.arguments!!.getLong("addToListId")
            SearchScreen(
                navController = navController,
                addToListId   = if (raw == -1L) null else raw
            )
        }

        // ── Deep destinations ──────────────────────────────────────────────────
        composable("stats")    { StatsScreen() }
        composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }) }
        composable("reviews")  { ReviewsScreen(navController) }

        // ── Review detail: review/{id} ────────────────────────────────────
        composable(
            route = "review/{id}",
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStack ->
            val reviewId = backStack.arguments!!.getInt("id")
            ReviewDetailScreen(reviewId = reviewId.toLong(), navController = navController)
        }

        // ── Movie detail: movie/{id} ───────────────────────────────────────────
        composable(
            route = "movie/{id}",
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStack ->
            val movieId = backStack.arguments!!.getInt("id")
            MovieDetailScreen(movieId = movieId, navController = navController)
        }

        // ── Log film: log/{movieId} ────────────────────────────────────────────
        composable(
            route = "log/{movieId}",
            arguments = listOf(navArgument("movieId") { type = NavType.IntType })
        ) { backStack ->
            val movieId = backStack.arguments!!.getInt("movieId")
            LogFilmScreen(movieId = movieId, navController = navController)
        }

        // ── Log film with optional pre-populated entry: log/{movieId}?existingEntryId={id} ──
        composable(
            route = "log/{movieId}?existingEntryId={existingEntryId}",
            arguments = listOf(
                navArgument("movieId") { type = NavType.IntType },
                navArgument("existingEntryId") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) { backStack ->
            val movieId         = backStack.arguments!!.getInt("movieId")
            val existingEntryId = backStack.arguments!!.getInt("existingEntryId")
            com.personal.cinevault.ui.screens.logmovie.LogMovieScreen(
                movieId = movieId,
                existingEntryId = existingEntryId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
