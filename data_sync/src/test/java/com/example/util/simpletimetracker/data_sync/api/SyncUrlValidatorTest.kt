package com.example.util.simpletimetracker.data_sync.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncUrlValidatorTest {

    @Test
    fun emptyInputIsNotNormalized() {
        assertNull(SyncUrlValidator.normalizeOrNull(""))
        assertNull(SyncUrlValidator.normalizeOrNull("   "))
        assertTrue(SyncUrlValidator.isValid(""))
    }

    @Test
    fun httpsSchemeIsAddedWhenMissing() {
        assertEquals(
            "https://example.ts.net/",
            SyncUrlValidator.normalizeOrNull("example.ts.net"),
        )
    }

    @Test
    fun httpSchemeIsPreserved() {
        assertEquals(
            "http://example.ts.net/",
            SyncUrlValidator.normalizeOrNull("http://example.ts.net"),
        )
    }

    @Test
    fun garbageInputIsRejected() {
        assertNull(SyncUrlValidator.normalizeOrNull("not a url"))
        assertNull(SyncUrlValidator.normalizeOrNull("https://"))
        assertFalse(SyncUrlValidator.isValid("not a url"))
    }

    @Test
    fun queryAndFragmentAreStripped() {
        assertEquals(
            "https://example.ts.net/",
            SyncUrlValidator.normalizeOrNull("https://example.ts.net?q=1#frag"),
        )
    }

    @Test
    fun pathAndPortArePreserved() {
        assertEquals(
            "https://example.ts.net:8443/sync/",
            SyncUrlValidator.normalizeOrNull("example.ts.net:8443/sync"),
        )
    }

    @Test
    fun inputIsTrimmed() {
        assertEquals(
            "https://example.ts.net/",
            SyncUrlValidator.normalizeOrNull("  https://example.ts.net  "),
        )
    }
}
