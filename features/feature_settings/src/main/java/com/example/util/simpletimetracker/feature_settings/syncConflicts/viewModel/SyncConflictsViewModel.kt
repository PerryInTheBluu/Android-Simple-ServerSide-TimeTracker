package com.example.util.simpletimetracker.feature_settings.syncConflicts.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.util.simpletimetracker.core.base.BaseViewModel
import com.example.util.simpletimetracker.core.mapper.TimeMapper
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.data_sync.engine.SyncEngine
import com.example.util.simpletimetracker.resources.R as resourcesR
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SyncConflictItemViewData(
    val id: String,
    val time: String,
    val entity: String,
    val resolution: String,
    val detail: String,
)

@HiltViewModel
class SyncConflictsViewModel @Inject constructor(
    private val syncEngine: SyncEngine,
    private val timeMapper: TimeMapper,
    private val resourceRepo: ResourceRepo,
) : BaseViewModel() {

    private val _conflicts = MutableLiveData<List<SyncConflictItemViewData>>()
    val conflicts: LiveData<List<SyncConflictItemViewData>> = _conflicts

    fun load() = viewModelScope.launch {
        val items = withContext(Dispatchers.IO) {
            syncEngine.getConflicts()
        }
        val mapped = items.map { conflict ->
            val entityStr = when (conflict.entityType) {
                "activity", "record_type" -> resourceRepo.getString(resourcesR.string.settings_sync_conflict_entity_activity)
                "time_entry", "record" -> resourceRepo.getString(resourcesR.string.settings_sync_conflict_entity_record)
                "category" -> resourceRepo.getString(resourcesR.string.settings_sync_conflict_entity_category)
                "tag" -> resourceRepo.getString(resourcesR.string.settings_sync_conflict_entity_tag)
                "timetable" -> resourceRepo.getString(resourcesR.string.settings_sync_conflict_entity_timetable)
                else -> conflict.entityType
            }
            val resStr = when (conflict.resolution) {
                "server_kept_newer" -> resourceRepo.getString(resourcesR.string.settings_sync_conflict_resolution_server_newer)
                "local_kept_name" -> resourceRepo.getString(resourcesR.string.settings_sync_conflict_resolution_local_name)
                "local_kept_start" -> resourceRepo.getString(resourcesR.string.settings_sync_conflict_resolution_local_start)
                "tombstone_kept" -> resourceRepo.getString(resourcesR.string.settings_sync_conflict_resolution_tombstone)
                "rejected" -> resourceRepo.getString(resourcesR.string.settings_sync_conflict_resolution_rejected)
                else -> conflict.resolution
            }
            val timeStr = timeMapper.formatDateTime(
                time = conflict.createdAt,
                useMilitaryTime = true,
                showSeconds = false,
            )
            SyncConflictItemViewData(
                id = "ID: ${conflict.entityId}",
                time = timeStr,
                entity = entityStr,
                resolution = resStr,
                detail = conflict.detail,
            )
        }
        _conflicts.value = mapped
    }

    fun clear(onCleared: () -> Unit) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            syncEngine.clearConflicts()
        }
        onCleared()
        load()
    }
}
