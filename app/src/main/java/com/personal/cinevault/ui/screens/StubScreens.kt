package com.personal.cinevault.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

// ── Home ──────────────────────────────────────────────────────────────────────
@Composable
fun HomeScreen(navController: NavController) {
    StubScreen("Home")
}

// ── Diary ─────────────────────────────────────────────────────────────────────
@Composable
fun DiaryScreen(navController: NavController) {
    StubScreen("Diary")
}


// ── Lists ─────────────────────────────────────────────────────────────────────
@Composable
fun ListsScreen(navController: NavController) {
    StubScreen("Lists")
}

// ── Profile ───────────────────────────────────────────────────────────────────
@Composable
fun ProfileScreen(navController: NavController) {
    StubScreen("Profile")
}

// ── Log Film ──────────────────────────────────────────────────────────────────
@Composable
fun LogFilmScreen(movieId: Int, navController: NavController) {
    StubScreen("Log Film #$movieId")
}

// ── Stats ─────────────────────────────────────────────────────────────────────
@Composable
fun StatsScreen(navController: NavController) {
    StubScreen("Stats")
}

// ── Settings ──────────────────────────────────────────────────────────────────
@Composable
fun SettingsScreen(navController: NavController) {
    StubScreen("Settings")
}

// ── Generic stub ─────────────────────────────────────────────────────────────
@Composable
private fun StubScreen(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("$name — coming soon", style = MaterialTheme.typography.titleMedium)
    }
}
