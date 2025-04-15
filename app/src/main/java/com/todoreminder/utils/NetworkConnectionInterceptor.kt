package com.todoreminder.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.todoreminder.R
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * An OkHttp Interceptor that checks for internet connectivity before proceeding with a network request.
 *
 * This interceptor ensures that a request is only executed when there is an active and validated internet connection.
 * If no internet connection is available, it throws an [IOException] with a localized message.
 *
 * @property context The context used to access system services and string resources.
 */
class NetworkConnectionInterceptor(private val context: Context) : Interceptor {

    /**
     * Intercepts the outgoing request and checks for internet connectivity.
     *
     * @param chain The request chain.
     * @return The response if the internet is available.
     * @throws IOException if there is no internet connection.
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!isInternetAvailable()) {
            throw IOException(context.getString(R.string.no_internet_connection))
        }
        return chain.proceed(chain.request())
    }

    /**
     * Checks whether the device has an active and validated internet connection.
     *
     * @return True if internet is available and validated, false otherwise.
     */
    private fun isInternetAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

