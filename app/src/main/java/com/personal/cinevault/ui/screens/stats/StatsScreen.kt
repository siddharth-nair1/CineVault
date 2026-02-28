package com.personal.cinevault.ui.screens.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.IconButton
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.personal.cinevault.domain.model.UserStats
import com.personal.cinevault.ui.components.HalfStarRating
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    navController: NavHostController,
    viewModel: StatsViewModel = koinViewModel()
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Stats",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (stats == null) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        val s = stats!!

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── 1. Big number stats row ────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BigStatCard(
                    value = s.totalFilmsWatched.toString(),
                    label = "Films\nWatched",
                    modifier = Modifier.weight(1f)
                )
                BigStatCard(
                    value = s.totalHoursWatched.toString(),
                    label = "Hours\nWatched",
                    modifier = Modifier.weight(1f)
                )
                BigStatCard(
                    value = s.filmsThisYear.toString(),
                    label = "Films\nThis Year",
                    modifier = Modifier.weight(1f)
                )
            }

            // ── 2. Average rating ─────────────────────────────────────────────
            StatCard(title = "Average Rating") {
                if (s.averageRating == null) {
                    Text(
                        "No ratings yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val displayRating = (s.averageRating / 2f).toFloat()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "%.1f".format(s.averageRating),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            HalfStarRating(
                                rating = displayRating,
                                onRatingChange = {},
                                starSize = 20.dp,
                                readOnly = true
                            )
                            Text(
                                text = "out of 10",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── 3. Rating distribution bar chart ──────────────────────────────
            if (s.ratingDistribution.isNotEmpty()) {
                StatCard(title = "Rating Distribution") {
                    RatingBarChart(distribution = s.ratingDistribution)
                }
            }

            // ── 4 & 5. Favourite genre + Most watched decade ──────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Top Genre",
                    modifier = Modifier.weight(1f)
                ) {
                    if (s.favouriteGenre != null) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    s.favouriteGenre,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    } else {
                        EmptyValue()
                    }
                }

                StatCard(
                    title = "Top Decade",
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (s.mostWatchedDecade != null) "${s.mostWatchedDecade}s"
                               else "—",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // ── 6. Liked + Rewatches ──────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Liked",
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = s.totalLiked.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                StatCard(
                    title = "Rewatches",
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = s.totalRewatches.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Bar chart ─────────────────────────────────────────────────────────────────

/**
 * Manual Canvas bar chart for the rating distribution.
 * Each bar corresponds to a 0.5-step display-scale rating (0.5–5.0).
 * Bar heights animate in with [animateFloatAsState].
 */
@Composable
private fun RatingBarChart(distribution: Map<Float, Int>) {
    // All possible half-star ratings in order
    val allKeys = (1..10).map { it / 2f }  // 0.5, 1.0, 1.5, … 5.0

    val maxCount = distribution.values.maxOrNull()?.takeIf { it > 0 } ?: 1

    val primaryColor  = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    // Trigger animation once composed
    var animated by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animated = true }

    // Pre-compute animated fractions for each key
    val fractions = allKeys.map { key ->
        val target = if (animated) (distribution[key] ?: 0) / maxCount.toFloat() else 0f
        animateFloatAsState(
            targetValue = target,
            animationSpec = tween(durationMillis = 600),
            label = "bar_$key"
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val barCount   = allKeys.size
            val spacing    = 4.dp.toPx()
            val totalSpacing = spacing * (barCount - 1)
            val barWidth   = (size.width - totalSpacing) / barCount
            val maxBarH    = size.height

            allKeys.forEachIndexed { index, key ->
                val fraction = fractions[index].value
                val barH     = maxBarH * fraction
                val left     = index * (barWidth + spacing)
                val top      = maxBarH - barH

                // Background track
                drawRoundRect(
                    color     = surfaceVariant,
                    topLeft   = Offset(left, 0f),
                    size      = Size(barWidth, maxBarH),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
                // Filled bar
                if (barH > 0f) {
                    drawRoundRect(
                        color     = primaryColor,
                        topLeft   = Offset(left, top),
                        size      = Size(barWidth, barH),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
            }
        }

        // X-axis labels: show only whole-star labels to avoid crowding
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            allKeys.forEach { key ->
                val label = if (key == key.toInt().toFloat()) "${key.toInt()}★" else ""
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ── Reusable card composables ─────────────────────────────────────────────────

@Composable
private fun StatCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            content()
        }
    }
}

@Composable
private fun BigStatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun EmptyValue() {
    Text(
        text = "—",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
