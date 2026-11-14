package com.crumbcodex.crk.data.seed

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Downloads seed files from [baseUrl] (a folder URL ending in '/'). Blocking — call from Dispatchers.IO. */
class RemoteSeedSource(private val baseUrl: String) {

    fun fetch(fileName: String): String {
        val url = URL(baseUrl.trimEnd('/') + "/" + fileName)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json, text/plain")
            setRequestProperty("Cache-Control", "no-cache")
            useCaches = false
        }
        try {
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw IOException("HTTP $code for $fileName")
            val length = conn.contentLengthLong
            if (length > MAX_BYTES) throw IOException("$fileName is too large ($length bytes)")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private companion object {
        const val MAX_BYTES = 10L * 1024 * 1024
    }
}
