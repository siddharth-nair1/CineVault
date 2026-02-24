package com.personal.cinevault.data.remote

import com.personal.cinevault.BuildConfig
import com.personal.cinevault.data.remote.dto.MovieDetailResponse
import com.personal.cinevault.data.remote.dto.MovieListResponse
import com.personal.cinevault.data.remote.dto.SearchResponse
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// ─── API Interface ────────────────────────────────────────────────────────────

interface TmdbApiService {

    /**
     * Search for movies by query string.
     * GET /search/movie?query=...&page=...
     */
    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("page")  page: Int = 1
    ): SearchResponse

    /**
     * Get full details for a single movie.
     * GET /movie/{movie_id}
     */
    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int
    ): MovieDetailResponse

    /**
     * Get trending movies for a given time window ("day" or "week").
     * GET /trending/movie/{time_window}
     */
    @GET("trending/movie/{time_window}")
    suspend fun getTrending(
        @Path("time_window") timeWindow: String = "week"
    ): MovieListResponse

    /**
     * Get a page of currently popular movies.
     * GET /movie/popular?page=...
     */
    @GET("movie/popular")
    suspend fun getPopular(
        @Query("page") page: Int = 1
    ): MovieListResponse

    companion object {

        private const val BASE_URL = "https://api.themoviedb.org/3/"

        /** Bearer-token auth interceptor using the read access token from BuildConfig. */
        private val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${BuildConfig.TMDB_READ_ACCESS_TOKEN}")
                .addHeader("Accept", "application/json")
                .build()
            chain.proceed(request)
        }

        fun create(): TmdbApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(TmdbApiService::class.java)
        }
    }
}
