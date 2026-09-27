package com.example.util.simpletimetracker

import android.app.Application
import android.os.StrictMode
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.util.simpletimetracker.data_sync.work.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import timber.log.Timber.DebugTree
import javax.inject.Inject

@HiltAndroidApp
class TimeTrackerApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncScheduler: SyncScheduler

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
    }

    private fun initStrictMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyDialog()
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
