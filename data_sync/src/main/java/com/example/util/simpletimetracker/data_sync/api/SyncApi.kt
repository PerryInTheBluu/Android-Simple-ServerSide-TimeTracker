package com.example.util.simpletimetracker.data_sync.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class ActivityDto(
    val id: String,
    val name: String,
    val color: String = "",
    val icon: String = "",
    val sort_order: Int = 0,
    val archived: Boolean = false,
    val parent_activity_id: String? = null,
    val category: String = "",
    val goal_seconds_per_week: Int? = null,
    val goal_seconds_total: Int? = null,
    val goal_days_per_month: Int? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val deleted_at: String? = null,
)

data class TimeEntryDto(
    val id: String,
    val activity_id: String,
    val parent_activity_ids: String = "",
    val started_at: String,
    val ended_at: String? = null,
    val duration_seconds: Int = 0,
    val comment: String = "",
    val tags: String = "",
    val created_at: String? = null,
    val updated_at: String? = null,
    val sync_status: String = "synced",
    val deleted_at: String? = null,
)

data class GoalDto(
    val id: String,
    val activity_id: String,
    val goal_type: String = "seconds_per_week",
    val goal_value: Double = 0.0,
    val created_at: String? = null,
    val updated_at: String? = null,
    val deleted_at: String? = null,
)

data class SyncPushItem(
    val entity_type: String,
    val data: Map<String, Any?>,
)

data class SyncPushRequest(
    val items: List<SyncPushItem>,
)

data class SyncPushResponse(
    val applied: Int,
    val conflicts: List<SyncConflict>,
    val server_time: String? = null,
)

data class SyncConflict(
    val entity_type: String,
    val id: String,
    val resolution: String,
)

data class SyncPullResponse(
    val activities: List<ActivityDto> = emptyList(),
    val time_entries: List<TimeEntryDto> = emptyList(),
    val goals: List<GoalDto> = emptyList(),
    val server_time: String? = null,
)

data class LoginRequest(
    val username: String,
    val password: String,
)

data class TokenResponse(
    val access_token: String,
    val refresh_token: String,
    val token_type: String = "bearer",
    val expires_in: Int = 0,
)

interface SyncApi {

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): TokenResponse

    @POST("api/sync/push")
    suspend fun push(@Body body: SyncPushRequest): SyncPushResponse

    @GET("api/sync/pull")
    suspend fun pull(@Query("since") since: String?): SyncPullResponse

    @GET("api/sync/conflicts")
    suspend fun conflicts(): List<SyncConflict>
}
