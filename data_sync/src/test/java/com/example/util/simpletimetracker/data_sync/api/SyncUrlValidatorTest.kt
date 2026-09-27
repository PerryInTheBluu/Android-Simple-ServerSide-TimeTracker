package com.example.util.simpletimetracker.data_sync.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SyncUrlValidatorTest {

    @Test
    fun returnsNullForEmptyUrl() {
        assertNull(SyncUrlValidator.normalizeOrNull(""))
    }

    @Test
    fun returnsNullForBlankUrl() {
        assertNull(SyncUrlValidator.normalizeOrNull("   "))
    }

    @Test
    fun returnsNullForUrlWithoutHost() {
        assertNull(SyncUrlValidator.normalizeOrNull("https://"))
    }

    @Test
    fun returnsNullForNonHttpScheme() {
        assertNull(SyncUrlValidator.normalizeOrNull("file:///data/sync"))
        assertNull(SyncUrlValidator.normalizeOrNull("ftp://example.com"))
    }

    @Test
    fun returnsNullForGarbageInput() {
        assertNull(SyncUrlValidator.normalizeOrNull("not a url ://"))
    }

    @Test
    fun prefixesHttpsSchemeWhenMissing() {
        assertEquals(
            "https://sync.example.com/",
            SyncUrlValidator.normalizeOrNull("sync.example.com"),
        )
    }

    @Test
    fun keepsHttpScheme() {
        assertEquals(
            "http://sync.example.com/",
            SyncUrlValidator.normalizeOrNull("http://sync.example.com"),
        )
    }

    @Test
    fun keepsHttpsScheme() {
        assertEquals(
            "https://sync.example.com/",
            SyncUrlValidator.normalizeOrNull("https://sync.example.com"),
        )
    }

    @Test
    fun appendsTrailingSlash() {
        assertEquals(
            "https://sync.example.com/",
            SyncUrlValidator.normalizeOrNull("https://sync.example.com"),
        )
    }

    @Test
    fun collapsesDoubleTrailingSlashes() {
        assertEquals(
            "https://sync.example.com/",
            SyncUrlValidator.normalizeOrNull("https://sync.example.com//"),
        )
    }

    @Test
    fun keepsPath() {
        assertEquals(
            "https://sync.example.com/timetracker/",
            SyncUrlValidator.normalizeOrNull("https://sync.example.com/timetracker"),
        )
    }

    @Test
    fun keepsPort() {
        assertEquals(
            "https://sync.example.com:8443/",
            SyncUrlValidator.normalizeOrNull("https://sync.example.com:8443"),
        )
    }

    @Test
    fun stripsQueryAndFragment() {
        assertEquals(
            "https://sync.example.com/",
            SyncUrlValidator.normalizeOrNull("https://sync.example.com/?token=abc#section"),
        )
    }

    @Test
    fun trimsWhitespace() {
        assertEquals(
            "https://sync.example.com/",
            SyncUrlValidator.normalizeOrNull("  https://sync.example.com  "),
        )
    }
}
