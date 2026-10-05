package com.example.util.simpletimetracker.qs

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.util.simpletimetracker.core.extension.allowDiskRead
import com.example.util.simpletimetracker.core.interactor.RecordRepeatInteractor
import com.example.util.simpletimetracker.domain.record.interactor.RunningRecordInteractor
import com.example.util.simpletimetracker.resources.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Quick settings tile to repeat tracking without opening the app:
 * when nothing runs the before last finished activity is started
 * (the last one is usually the break that just ended); when
 * something runs it is stopped and the last finished activity is
 * started (the one from before the current activity).
 */
@AndroidEntryPoint
class RepeatTileService : TileService() {

    @Inject lateinit var recordRepeatInteractor: RecordRepeatInteractor
    @Inject lateinit var runningRecordInteractor: RunningRecordInteractor

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        scope.launch { updateTile() }
    }

    override fun onClick() {
        scope.launch {
            recordRepeatInteractor.repeatForQuickTileExternal()
            updateTile()
        }
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        scope.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private suspend fun updateTile() {
        val tile = qsTile ?: return
        val running = allowDiskRead { runningRecordInteractor.getAll() }
        tile.label = getString(R.string.qs_tile_repeat)
        // The tile is active while something is tracked; the action
        // itself is the same in both states.
        tile.state = if (running.isNotEmpty()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
