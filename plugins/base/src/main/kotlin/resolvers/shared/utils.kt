package org.jetbrains.dokka.base.resolvers.shared

import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection

internal fun URL.readContent(timeout: Int = 10000, redirectsAllowed: Int = 16): InputStream {
    fun URL.doOpenConnection(timeout: Int, redirectsAllowed: Int): URLConnection {
        val connection = this.openConnection().apply {
            connectTimeout = timeout
            readTimeout = timeout
        }

        when (connection) {
            is HttpURLConnection -> return when (connection.responseCode) {
                in 200..299 -> connection

                HttpURLConnection.HTTP_MOVED_PERM,
                HttpURLConnection.HTTP_MOVED_TEMP,
                HttpURLConnection.HTTP_SEE_OTHER -> {
                    if (redirectsAllowed > 0) {
                        val newUrl = connection.getHeaderField("Location")
                        URL(newUrl).doOpenConnection(timeout, redirectsAllowed - 1)
                    } else {
                        throw RuntimeException("Too many redirects")
                    }
                }

                else -> throw RuntimeException("Unhandled HTTP code: ${connection.responseCode}")
            }

            else -> return connection
        }
    }
    return doOpenConnection(timeout, redirectsAllowed).getInputStream()
}
