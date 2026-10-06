package com.example.util.simpletimetracker.domain.timetable.interactor

import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.timetable.ics.IcsImportInteractor
import com.example.util.simpletimetracker.domain.timetable.model.CalendarSubscription
import com.example.util.simpletimetracker.domain.timetable.repo.TimetableIcsRepo
import javax.inject.Inject

class CalendarSubscriptionSyncInteractor @Inject constructor(
    private val prefsInteractor: PrefsInteractor,
    private val timetableIcsRepo: TimetableIcsRepo,
    private val icsImportInteractor: IcsImportInteractor,
) {

    sealed class SyncResult {
        data class Success(
            val subscription: CalendarSubscription,
            val importResult: IcsImportInteractor.ImportResult,
        ) : SyncResult()

        data class Error(
            val subscription: CalendarSubscription,
            val error: String,
        ) : SyncResult()
    }

    suspend fun syncSubscription(
        subscription: CalendarSubscription,
        clearExisting: Boolean = false,
    ): SyncResult {
        return runCatching {
            val icsContent = timetableIcsRepo.fetchIcsFromUrl(subscription.url)
            val importResult = icsImportInteractor.import(
                content = icsContent,
                clearExisting = clearExisting,
            )
            val updated = subscription.copy(lastFetched = System.currentTimeMillis())
            prefsInteractor.addCalendarSubscription(updated)
            SyncResult.Success(updated, importResult)
        }.getOrElse { e ->
            SyncResult.Error(subscription, e.message ?: "Sync failed")
        }
    }

    suspend fun syncAll(): List<SyncResult> {
        val subscriptions = prefsInteractor.getCalendarSubscriptions()
            .filter { it.enabled && it.url.isNotBlank() }
        // For the first active subscription we clear if it's the sole source, otherwise merge.
        return subscriptions.mapIndexed { index, sub ->
            syncSubscription(sub, clearExisting = (index == 0))
        }
    }
}
