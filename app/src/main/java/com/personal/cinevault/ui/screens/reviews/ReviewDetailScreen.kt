package com.personal.cinevault.ui.screens.reviews

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.personal.cinevault.domain.model.Review
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TMDB_BACKDROP = "https://image.tmdb.org/t/p/w780"
private const val TMDB_POSTER   = "https://image.tmdb.org/t/p/w342"
private val DATE_FMT = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewDetailScreen(
    reviewId: Long,
    navController: NavController,
    viewModel: ReviewDetailViewModel = koinViewModel(parameters = { parametersOf(reviewId) })
) {
    val review    by viewModel.review.collectAsStateWithLifecycle()
    val isDeleting by viewModel.isDeleting.collectAsStateWithLifecycle()
    val deleted   by viewModel.deleted.collectAsStateWithLifecycle()

    // UI state
    var menuExpanded     by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    // Pop back once deletion is confirmed
    LaunchedEffect(deleted) {
        if (deleted) navController.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(review?.movieTitle ?: "") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            text = { Text("Edit") },
                            onClick = {
                                menuExpanded = false
                                review?.let { r ->
                                    navController.navigate("log/${r.tmdbMovieId}")
                                }
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            text = {
                                Text(
                                    "Delete",
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                showDeleteDialog = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        when {
            isDeleting -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            review == null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            else -> ReviewDetailContent(
                review = review!!,
                modifier = Modifier.padding(padding),
                onSpoilerToggle = viewModel::toggleSpoiler
            )
        }
    }

    // ── Delete confirmation dialog ─────────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Review") },
            text = {
                Text("Are you sure you want to permanently delete your review of \"${review?.movieTitle}\"? This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteReview()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ── Main content ───────────────────────────────────────────────────────────────

@Composable
private fun ReviewDetailContent(
    review: Review,
    modifier: Modifier = Modifier,
    onSpoilerToggle: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Backdrop + Poster overlap ─────────────────────────────────────────
        Box {
            // Use the poster stretched as a backdrop background when no backdropPath is cached
            AsyncImage(
                model = review.posterPath?.let { "$TMDB_BACKDROP$it" },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .blur(6.dp)
            )
            // Gradient fade so content below blends cleanly
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background),
                            startY = 80f
                        )
                    )
            )
            // Poster thumbnail overlapping the bottom of the header
            AsyncImage(
                model = review.posterPath?.let { "$TMDB_POSTER$it" },
                contentDescription = "${review.movieTitle} poster",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 100.dp, height = 150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .align(Alignment.BottomStart)
                    .offset(x = 16.dp, y = 60.dp)
            )
        }

        Spacer(Modifier.height(68.dp))

        // ── Title ─────────────────────────────────────────────────────────────
        Text(
            text = review.movieTitle,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(8.dp))

        // ── Star rating + date row ────────────────────────────────────────────
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            review.rating?.let { StarRatingDisplay(it) }
            Text(
                text = DATE_FMT.format(Date(review.createdAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── Review section ────────────────────────────────────────────────────
        Text(
            text = "Review",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(8.dp))

        // Spoiler wrapper
        SpoilerText(
            text = review.content,
            isSpoiler = review.containsSpoilers,
            onReveal = onSpoilerToggle,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(24.dp))

        // ── Spoiler toggle button ─────────────────────────────────────────────
        if (review.containsSpoilers) {
            SpoilerToggleChip(
                containsSpoilers = true,
                onToggle = onSpoilerToggle,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            SpoilerToggleChip(
                containsSpoilers = false,
                onToggle = onSpoilerToggle,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}

// ── Spoiler-aware text block ───────────────────────────────────────────────────

@Composable
private fun SpoilerText(
    text: String,
    isSpoiler: Boolean,
    onReveal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        // The actual text — always rendered, blurred when spoiler is active
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isSpoiler) Modifier.blur(16.dp) else Modifier)
        )

        // Tap-to-reveal overlay when blurred
        if (isSpoiler) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(onClick = onReveal),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            Icons.Default.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Tap to reveal spoilers",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ── Spoiler toggle chip ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpoilerToggleChip(
    containsSpoilers: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Badge(
        containerColor = if (containsSpoilers)
            MaterialTheme.colorScheme.errorContainer
        else
            MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (containsSpoilers)
            MaterialTheme.colorScheme.onErrorContainer
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.clickable(onClick = onToggle)
    ) {
        Text(
            text = if (containsSpoilers) "⚠ Contains Spoilers — tap to hide" else "Mark as spoiler",
            style = MaterialTheme.typography.labelSmall,
            fontStyle = if (!containsSpoilers) FontStyle.Italic else FontStyle.Normal,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

// ── Star rating display ────────────────────────────────────────────────────────

@Composable
private fun StarRatingDisplay(rating: Float) {
    // rating is stored 0–10; display as 0–5 half-stars
    val displayRating = rating / 2f
    val full = displayRating.toInt().coerceIn(0, 5)
    val hasHalf = displayRating - full >= 0.5f
    val empty = 5 - full - if (hasHalf && full < 5) 1 else 0

    val stars = buildString {
        repeat(full) { append('★') }
        if (hasHalf && full < 5) append('½')
        repeat(empty) { append('☆') }
        append("  ")
        append("%.1f".format(displayRating))
        append(" / 5")
    }

    Text(
        text = stars,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Start
    )
}
