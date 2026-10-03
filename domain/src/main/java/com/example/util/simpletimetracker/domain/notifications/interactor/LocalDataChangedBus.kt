package com.example.util.simpletimetracker.domain.notifications.interactor

import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Published whenever tracked data changes (timers started or stopped,
 * records added, changed or removed, activities changed). The sync
 * engine subscribes and runs a debounced sync, so local changes reach
 * the server within a few seconds without touching the UI.
 */
object LocalDataChangedBus {

    val events: MutableSharedFlow<Unit> = MutableSharedFlow(extraBufferCapacity = 32)

    fun publish() {
        events.tryEmit(Unit)
    }
}
