package com.todoreminder.di

import android.content.Context
import com.todoreminder.data.remote.ReminderApiService
import com.todoreminder.utils.NetworkConnectionInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/**
 * Dagger-Hilt module that provides network-related dependencies like Retrofit,
 * OkHttpClient, and interceptors. Installed in the SingletonComponent, which
 * means all dependencies will be singletons throughout the app lifecycle.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Provides a singleton instance of [HttpLoggingInterceptor] used to log HTTP request and response data.
     *
     * @return [HttpLoggingInterceptor] with BODY level logging enabled.
     */
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    /**
     * Provides a singleton instance of [OkHttpClient], configured with logging and network connectivity interceptors.
     *
     * @param context Application context injected by Hilt.
     * @param loggingInterceptor Logging interceptor for HTTP logs.
     * @return Configured [OkHttpClient] instance.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(NetworkConnectionInterceptor(context))
            .build()
    }

    /**
     * Provides a singleton instance of [Retrofit] configured with base URL and JSON converter.
     *
     * @param okHttpClient Custom [OkHttpClient] used for HTTP requests.
     * @return [Retrofit] instance for network calls.
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://jsonplaceholder.typicode.com/") // Replace with your actual base URL if needed
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Provides a singleton instance of [ReminderApiService], an interface defining API endpoints.
     *
     * @param retrofit [Retrofit] instance used to create the API service.
     * @return Implementation of [ReminderApiService].
     */
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ReminderApiService {
        return retrofit.create(ReminderApiService::class.java)
    }
}
