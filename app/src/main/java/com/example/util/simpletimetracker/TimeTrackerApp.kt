package com.example.util.simpletimetracker

import android.app.Application
import android.os.StrictMode
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.util.simpletimetracker.data_sync.work.SyncOnDataChangeInteractor
import com.example.util.simpletimetracker.data_sync.work.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class TimeTrackerApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    lateinit var syncOnDataChangeInteractor: SyncOnDataChangeInteractor

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        initLanguage()
        initLog()
        initLibraries()
        initStrictMode()
        initSync()
    }

    private fun initLanguage() {
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(DEFAULT_LANGUAGE_TAG),
            )
        }
    }

    private fun initLog() {
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }
    }

    private fun initLibraries() {
        val config = BundledEmojiCompatConfig(applicationContext)
            .setReplaceAll(true)
        EmojiCompat.init(config)
    }

    private fun initSync() {
        syncScheduler.schedulePeriodicSync()
        syncOnDataChangeInteractor.subscribe()
        warmUpTimezoneDatabase()
    }

    /**
     * The timezone database is lazily mmapped on first use; if that happens
     * on the main thread (for example through PrefsRepoImpl first day of
     * week default) StrictMode kills debug builds. Load it here on a
     * background thread instead.
     */
    private fun warmUpTimezoneDatabase() {
        CoroutineScope(Dispatchers.IO).launch {
            Calendar.getInstance().firstDayOfWeek
        }
    }

    private fun initStrictMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build(),
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build(),
            )
        }
    }

    companion object {

        private const val DEFAULT_LANGUAGE_TAG = "de"
    }
}
