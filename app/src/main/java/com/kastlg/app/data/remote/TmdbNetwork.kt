package com.kastlg.app.data.remote

import com.kastlg.app.data.repository.TmdbMovieRepository
import com.kastlg.app.domain.repositories.MovieRepository
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object TmdbNetwork {
    const val LANGUAGE = "en-US"
    private const val BASE_URL = "https://api.themoviedb.org/3/"

    fun createRepository(
        accessToken: String,
        baseUrl: String = BASE_URL,
    ): MovieRepository {
        val normalizedToken = accessToken.trim()
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .apply {
                        if (normalizedToken.isNotEmpty()) {
                            header("Authorization", "Bearer $normalizedToken")
                        }
                    }
                    .build()
                chain.proceed(request)
            }
            .build()

        val api = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TmdbApi::class.java)

        return TmdbMovieRepository(
            api = api,
            hasAccessToken = normalizedToken.isNotEmpty(),
        )
    }
}
