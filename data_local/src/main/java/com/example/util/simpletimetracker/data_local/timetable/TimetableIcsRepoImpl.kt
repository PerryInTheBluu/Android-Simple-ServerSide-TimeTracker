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
}
