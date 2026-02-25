package com.personal.cinevault.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.personal.cinevault.ui.screens.DiaryScreen
import com.personal.cinevault.ui.screens.home.HomeScreen
import com.personal.cinevault.ui.screens.ListsScreen
import com.personal.cinevault.ui.screens.LogFilmScreen
import com.personal.cinevault.ui.screens.ProfileScreen
import com.personal.cinevault.ui.screens.SettingsScreen
import com.personal.cinevault.ui.screens.StatsScreen
import com.personal.cinevault.ui.screens.watchlist.WatchlistScreen
import com.personal.cinevault.ui.screens.moviedetail.MovieDetailScreen
import com.personal.cinevault.ui.screens.search.SearchScreen

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
        composable("search") {
            SearchScreen(navController)
        }
        composable("diary") {
            DiaryScreen(navController)
        }
        composable("watchlist") {
            WatchlistScreen(navController)
        }
        composable("profile") {
            ProfileScreen(navController)
        }

        // ── Deep destinations ──────────────────────────────────────────────────
        composable("lists") {
            ListsScreen(navController)
        }
        composable("stats") {
            StatsScreen(navController)
        }
        composable("settings") {
            SettingsScreen(navController)
        }

        // ── Movie detail: movie/{id} ───────────────────────────────────────────
        composable(
            route = "movie/{id}",
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStack ->
            val movieId = backStack.arguments!!.getInt("id")
            MovieDetailScreen(movieId = movieId, navController = navController)
        }

        // ── Log film: log/{movieId} ───────────────────────────────────────────
        composable(
            route = "log/{movieId}",
            arguments = listOf(navArgument("movieId") { type = NavType.IntType })
        ) { backStack ->
            val movieId = backStack.arguments!!.getInt("movieId")
            LogFilmScreen(movieId = movieId, navController = navController)
        }
    }
}
