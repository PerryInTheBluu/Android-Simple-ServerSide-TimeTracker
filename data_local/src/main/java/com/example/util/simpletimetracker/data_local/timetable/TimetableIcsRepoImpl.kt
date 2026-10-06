package com.example.util.simpletimetracker.data_local.timetable

import android.content.ContentResolver
import androidx.core.net.toUri
import com.example.util.simpletimetracker.domain.timetable.repo.TimetableIcsRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject

class TimetableIcsRepoImpl @Inject constructor(
    private val contentResolver: ContentResolver,
) : TimetableIcsRepo {

    override suspend fun readIcsFile(uriString: String): String = withContext(Dispatchers.IO) {
        if (isRemoteUrl(uriString)) {
            return@withContext fetchIcsFromUrl(uriString)
        }
        var inputStream: InputStream? = null
        try {
            inputStream = contentResolver.openInputStream(uriString.toUri())
            inputStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        } finally {
            try {
                inputStream?.close()
            } catch (_: Exception) {
                // Do nothing
            }
        }
    }

    override suspend fun fetchIcsFromUrl(urlString: String): String = withContext(Dispatchers.IO) {
        val normalizedUrl = normalizeUrl(urlString)
        val url = java.net.URL(normalizedUrl)
        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("User-Agent", "SimpleTimeTracker/1.0")
        connection.setRequestProperty("Accept", "text/calendar, text/plain, */*")
        try {
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                throw java.io.IOException("HTTP $responseCode: ${connection.responseMessage}")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun isRemoteUrl(uriString: String): Boolean {
        val lower = uriString.lowercase()
        return lower.startsWith("http://") ||
            lower.startsWith("https://") ||
            lower.startsWith("webcal://") ||
            lower.startsWith("webdav://") ||
            lower.startsWith("webdavs://")
    }

    private fun normalizeUrl(urlString: String): String {
        return urlString
            .replaceFirst(Regex("^webcal://", RegexOption.IGNORE_CASE), "https://")
            .replaceFirst(Regex("^webdavs://", RegexOption.IGNORE_CASE), "https://")
            .replaceFirst(Regex("^webdav://", RegexOption.IGNORE_CASE), "http://")
    }
}
