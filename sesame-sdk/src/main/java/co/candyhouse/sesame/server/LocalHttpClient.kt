package co.candyhouse.sesame.server

import co.candyhouse.sesame.utils.L
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Local HTTP client for communication with local Sesame server.
 * Replaces AWS API Gateway for offline/local operation.
 */
internal object LocalHttpClient {

    private const val TAG = "LocalHttpClient"
    private const val TIMEOUT_MS = 10000
    private val gson = Gson()

    suspend fun makeRequest(
        method: String,
        path: String,
        body: Any? = null,
        headers: Map<String, String> = emptyMap()
    ): String = withContext(Dispatchers.IO) {
        try {
            val url = URL(LocalServerConfig.getFullApiUrl(path))
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = method
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")

            // Add default headers
            headers.forEach { (key, value) ->
                connection.setRequestProperty(key, value)
            }

            // Send body if present
            body?.let {
                connection.doOutput = true
                val bodyJson = if (body is String) body else gson.toJson(body)
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(bodyJson)
                    writer.flush()
                }
            }

            // Read response
            val responseCode = connection.responseCode
            val responseBody = readResponse(connection)

            L.d(TAG, "Request: $method $path, Response: $responseCode")

            if (responseCode in 200..299) {
                responseBody
            } else {
                throw Exception("HTTP $responseCode: $responseBody")
            }
        } catch (e: Exception) {
            L.e(TAG, "Network error: ${e.message}", e)
            throw e
        }
    }

    private fun readResponse(connection: HttpURLConnection): String {
        val inputStream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }

        return inputStream?.use {
            BufferedReader(InputStreamReader(it)).use { reader ->
                reader.readText()
            }
        } ?: ""
    }

    suspend fun postJson(path: String, body: Any): String {
        return makeRequest("POST", path, body)
    }

    suspend fun getJson(path: String): String {
        return makeRequest("GET", path)
    }

    suspend fun putJson(path: String, body: Any): String {
        return makeRequest("PUT", path, body)
    }

    suspend fun deleteJson(path: String, body: Any? = null): String {
        return makeRequest("DELETE", path, body)
    }
}

