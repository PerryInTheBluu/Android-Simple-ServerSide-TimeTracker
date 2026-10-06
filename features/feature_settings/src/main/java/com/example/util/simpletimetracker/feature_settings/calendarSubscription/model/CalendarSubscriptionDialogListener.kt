package com.example.util.simpletimetracker.feature_settings.calendarSubscription.model

interface CalendarSubscriptionDialogListener {
    fun onCalendarSubscriptionSaved(id: String, name: String, url: String, color: String, enabled: Boolean)
    fun onCalendarSubscriptionDeleted(id: String)
}
