package com.personal.cinevault.data.import

import android.content.Context
import android.net.Uri
import android.util.Log
import com.personal.cinevault.data.import.models.ImportResult
import com.personal.cinevault.data.local.dao.CineListDao
import com.personal.cinevault.data.local.dao.ListMovieDao
import com.personal.cinevault.data.local.dao.LogEntryDao
import com.personal.cinevault.data.local.dao.ReviewDao
import com.personal.cinevault.data.local.dao.WatchlistDao
import com.personal.cinevault.data.local.entity.CineListEntity
import com.personal.cinevault.data.local.entity.ListMovieEntity
import com.personal.cinevault.data.local.entity.LogEntryEntity
import com.personal.cinevault.data.local.entity.ReviewEntity
import com.personal.cinevault.data.local.entity.WatchlistEntity
import com.personal.cinevault.data.remote.TmdbApiService
import com.personal.cinevault.data.remote.dto.MovieDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

class LetterboxdImporter(
    private val tmdbApi: TmdbApiService,
    private val logDao: LogEntryDao,
    private val reviewDao: ReviewDao,
    private val watchlistDao: WatchlistDao,
    private val cineListDao: CineListDao,
    private val listMovieDao: ListMovieDao
) {

    suspend fun importFromZip(
        context: Context,
        zipUri: Uri,
        onProgress: (Int, Int) -> Unit
    ): ImportResult = withContext(Dispatchers.IO) {
        val cache = TmdbSearchCache()
        val movieDetailsCache = mutableMapOf<Int, MovieDto>()

        var logsImported = 0
        var reviewsImported = 0
        var watchlistImported = 0
        var listsImported = 0
        var failedLookups = 0

        val fileContents = mutableMapOf<String, List<List<String>>>()

        context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
            val zis = ZipInputStream(inputStream)
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".csv")) {
                    val reader = BufferedReader(InputStreamReader(zis))
                    val lines = mutableListOf<String>()
                    var line: String? = reader.readLine()
                    while (line != null) {
                        lines.add(line)
                        line = reader.readLine()
                    }
                    val parsedCsv = lines.map { parseCsvLine(it) }
                    fileContents[entry.name] = parsedCsv
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        // Calculate total rows for progress tracking
        var totalRowsProcessed = 0
        val totalRows = fileContents.values.sumOf { it.size - 1 } // Approximating without headers
        
        // Helper specifically for cache + lookup
        suspend fun findMovie(title: String, year: String): MovieDto? {
            val key = "${title}_${year}"
            val cachedId = cache.get(key)
            if (cachedId != null) {
                return movieDetailsCache[cachedId]
            } else if (cache.contains(key)) {
                return null // Already tried and failed
            }

            delay(50L) // Rate limit
            return try {
                val res = tmdbApi.searchMovies(query = title)
                val match = res.results.firstOrNull { it.releaseDate.startsWith(year) }
                    ?: res.results.firstOrNull() // fallback to exact title match
                    
                if (match != null) {
                    cache.put(key, match.id)
                    movieDetailsCache[match.id] = match
                    match
                } else {
                    cache.put(key, null)
                    null
                }
            } catch (e: Exception) {
                Log.e("LetterboxdImporter", "Search API failed for $title", e)
                cache.put(key, null)
                null
            }
        }

        val processedWatchedMovies = mutableSetOf<String>()

        // 1. Process diary.csv
        fileContents["diary.csv"]?.let { rows ->
            val headers = rows.firstOrNull() ?: emptyList()
            val dateIdx = headers.indexOf("Date")
            val nameIdx = headers.indexOf("Name")
            val yearIdx = headers.indexOf("Year")
            val ratingIdx = headers.indexOf("Rating")
            val rewatchIdx = headers.indexOf("Rewatch")
            val watchedDateIdx = headers.indexOf("Watched Date")

            for (i in 1 until rows.size) {
                val row = rows[i]
                if (nameIdx == -1 || row.size <= nameIdx) continue
                
                val name = row[nameIdx]
                val year = if (yearIdx != -1 && row.size > yearIdx) row[yearIdx] else ""
                val ratingStr = if (ratingIdx != -1 && row.size > ratingIdx) row[ratingIdx] else ""
                val rewatchStr = if (rewatchIdx != -1 && row.size > rewatchIdx) row[rewatchIdx] else ""
                val watchedDate = if (watchedDateIdx != -1 && row.size > watchedDateIdx) row[watchedDateIdx] else ""
                
                val movie = findMovie(name, year)
                if (movie != null) {
                    logDao.upsert(
                        LogEntryEntity(
                            tmdbMovieId = movie.id,
                            movieTitle = movie.title,
                            posterPath = movie.posterPath,
                            rating = ratingStr.toFloatOrNull(),
                            rewatch = rewatchStr.equals("Yes", ignoreCase = true),
                            watchedDate = watchedDate.takeIf { it.isNotBlank() }
                        )
                    )
                    logsImported++
                    processedWatchedMovies.add("${name}_${year}")
                } else {
                    failedLookups++
                }
                
                totalRowsProcessed++
                onProgress(totalRowsProcessed, totalRows)
            }
        }

        // 2. Process ratings.csv
        fileContents["ratings.csv"]?.let { rows ->
            val headers = rows.firstOrNull() ?: emptyList()
            val nameIdx = headers.indexOf("Name")
            val yearIdx = headers.indexOf("Year")
            val ratingIdx = headers.indexOf("Rating")

            for (i in 1 until rows.size) {
                val row = rows[i]
                if (nameIdx == -1 || row.size <= nameIdx) continue

                val name = row[nameIdx]
                val year = if (yearIdx != -1 && row.size > yearIdx) row[yearIdx] else ""
                // Skip if already imported in diary (optional but good idea if rating is already there)
                val isAlreadyProcessed = processedWatchedMovies.contains("${name}_${year}")

                val ratingStr = if (ratingIdx != -1 && row.size > ratingIdx) row[ratingIdx] else ""
                val movie = findMovie(name, year)
                if (movie != null) {
                    if (!isAlreadyProcessed) {
                        logDao.upsert(
                            LogEntryEntity(
                                tmdbMovieId = movie.id,
                                movieTitle = movie.title,
                                posterPath = movie.posterPath,
                                rating = ratingStr.toFloatOrNull(),
                                watchedDate = null 
                            )
                        )
                        logsImported++
                        processedWatchedMovies.add("${name}_${year}")
                    }
                } else {
                    if (!isAlreadyProcessed) failedLookups++
                }
                
                totalRowsProcessed++
                onProgress(totalRowsProcessed, totalRows)
            }
        }

        // 3. Process reviews.csv
        fileContents["reviews.csv"]?.let { rows ->
            val headers = rows.firstOrNull() ?: emptyList()
            val nameIdx = headers.indexOf("Name")
            val yearIdx = headers.indexOf("Year")
            val ratingIdx = headers.indexOf("Rating")
            val reviewIdx = headers.indexOf("Review")

            for (i in 1 until rows.size) {
                val row = rows[i]
                if (nameIdx == -1 || row.size <= nameIdx) continue

                val name = row[nameIdx]
                val year = if (yearIdx != -1 && row.size > yearIdx) row[yearIdx] else ""
                val ratingStr = if (ratingIdx != -1 && row.size > ratingIdx) row[ratingIdx] else ""
                val reviewText = if (reviewIdx != -1 && row.size > reviewIdx) row[reviewIdx] else ""

                val movie = findMovie(name, year)
                if (movie != null) {
                    reviewDao.upsert(
                        ReviewEntity(
                            tmdbMovieId = movie.id,
                            movieTitle = movie.title,
                            posterPath = movie.posterPath,
                            rating = ratingStr.toFloatOrNull(),
                            reviewText = reviewText
                        )
                    )
                    reviewsImported++
                } else {
                    failedLookups++
                }
                
                totalRowsProcessed++
                onProgress(totalRowsProcessed, totalRows)
            }
        }

        // 4. Process watchlist.csv
        fileContents["watchlist.csv"]?.let { rows ->
            val headers = rows.firstOrNull() ?: emptyList()
            val nameIdx = headers.indexOf("Name")
            val yearIdx = headers.indexOf("Year")

            for (i in 1 until rows.size) {
                val row = rows[i]
                if (nameIdx == -1 || row.size <= nameIdx) continue

                val name = row[nameIdx]
                val year = if (yearIdx != -1 && row.size > yearIdx) row[yearIdx] else ""

                val movie = findMovie(name, year)
                if (movie != null) {
                    watchlistDao.insert(
                        WatchlistEntity(
                            tmdbMovieId = movie.id,
                            movieTitle = movie.title,
                            posterPath = movie.posterPath,
                            releaseYear = year,
                            voteAverage = movie.voteAverage.toFloat()
                        )
                    )
                    watchlistImported++
                } else {
                    failedLookups++
                }
                
                totalRowsProcessed++
                onProgress(totalRowsProcessed, totalRows)
            }
        }

        // 5. Process watched.csv
        fileContents["watched.csv"]?.let { rows ->
            val headers = rows.firstOrNull() ?: emptyList()
            val nameIdx = headers.indexOf("Name")
            val yearIdx = headers.indexOf("Year")

            for (i in 1 until rows.size) {
                val row = rows[i]
                if (nameIdx == -1 || row.size <= nameIdx) continue

                val name = row[nameIdx]
                val year = if (yearIdx != -1 && row.size > yearIdx) row[yearIdx] else ""

                if (!processedWatchedMovies.contains("${name}_${year}")) {
                    val movie = findMovie(name, year)
                    if (movie != null) {
                        logDao.upsert(
                            LogEntryEntity(
                                tmdbMovieId = movie.id,
                                movieTitle = movie.title,
                                posterPath = movie.posterPath
                            )
                        )
                        logsImported++
                        processedWatchedMovies.add("${name}_${year}")
                    } else {
                        failedLookups++
                    }
                }
                totalRowsProcessed++
                onProgress(totalRowsProcessed, totalRows)
            }
        }

        // 6. Process lists/ folder
        fileContents.keys.filter { it.startsWith("lists/") }.forEach { listFilename ->
            val rows = fileContents[listFilename] ?: return@forEach
            // Structure:
            // Line 1: "Letterboxd list export v7"
            // Line 2: headers — Date, Name, Tags, URL, Description (list metadata)
            // Line 3: list metadata values (List Name is index 1)
            // Line 4: blank
            // Line 5: headers — Position, Name, Year, URL, Description
            // Line 6+: movie rows

            if (rows.size > 2) {
                val metaHeaders = rows[1]
                val metaRow = rows[2]
                
                val listNameIdx = metaHeaders.indexOf("Name")
                val listDescIdx = metaHeaders.indexOf("Description")
                val listName = if (listNameIdx != -1 && metaRow.size > listNameIdx) metaRow[listNameIdx] else "Letterboxd List"
                val listDesc = if (listDescIdx != -1 && metaRow.size > listDescIdx) metaRow[listDescIdx] else ""
                
                val insertedListId = cineListDao.insertList(
                    CineListEntity(
                        name = listName,
                        description = listDesc
                    )
                )
                listsImported++

                // Find movie headers (Line 5)
                var movieHeaderIdx = 4
                if (movieHeaderIdx < rows.size && rows[movieHeaderIdx].size == 1 && rows[movieHeaderIdx][0].isBlank()) {
                    movieHeaderIdx++ // In case list format has extra blank lines
                }
                
                if (movieHeaderIdx < rows.size) {
                    val movieHeaders = rows[movieHeaderIdx]
                    val mNameIdx = movieHeaders.indexOf("Name")
                    val mYearIdx = movieHeaders.indexOf("Year")
                    val mPosIdx = movieHeaders.indexOf("Position")

                    for (i in (movieHeaderIdx + 1) until rows.size) {
                        val row = rows[i]
                        if (mNameIdx == -1 || row.size <= mNameIdx) continue
                        val mName = row[mNameIdx]
                        val mYear = if (mYearIdx != -1 && row.size > mYearIdx) row[mYearIdx] else ""
                        val mPos = if (mPosIdx != -1 && row.size > mPosIdx) row[mPosIdx].toIntOrNull() ?: 0 else 0

                        val movie = findMovie(mName, mYear)
                        if (movie != null) {
                            listMovieDao.addMovieToList(
                                ListMovieEntity(
                                    listId = insertedListId.toInt(),
                                    tmdbMovieId = movie.id,
                                    movieTitle = movie.title,
                                    posterPath = movie.posterPath,
                                    releaseYear = mYear,
                                    sortOrder = mPos
                                )
                            )
                        } else {
                            failedLookups++
                        }
                    }
                }
            }
        }

        onProgress(totalRows, totalRows) // Ensure 100% completion
        
        ImportResult(
            logsImported = logsImported,
            reviewsImported = reviewsImported,
            watchlistImported = watchlistImported,
            listsImported = listsImported,
            failedLookups = failedLookups
        )
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    current.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString())
                current = StringBuilder()
            } else {
                current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
