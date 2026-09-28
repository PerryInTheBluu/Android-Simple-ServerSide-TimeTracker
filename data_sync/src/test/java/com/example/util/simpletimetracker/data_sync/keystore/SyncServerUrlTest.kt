package com.example.util.simpletimetracker.data_sync.keystore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SyncServerUrlTest {

    @Test
    fun `empty url is rejected`() {
        assertNull(normalizeServerUrlOrNull(""))
    }

    @Test
    fun `whitespace url is rejected`() {
        assertNull(normalizeServerUrlOrNull("   "))
        assertNull(normalizeServerUrlOrNull("\t\n"))
    }

    @Test
    fun `invalid url is rejected`() {
        assertNull(normalizeServerUrlOrNull("not a url"))
        assertNull(normalizeServerUrlOrNull("example.com"))
        assertNull(normalizeServerUrlOrNull("https://"))
        assertNull(normalizeServerUrlOrNull("file:///data/sync"))
        assertNull(normalizeServerUrlOrNull("ftp://example.com"))
        assertNull(normalizeServerUrlOrNull("javascript:alert(1)"))
    }

    @Test
    fun `valid https url is normalized with trailing slash`() {
        assertEquals(
            "https://sync.example.com/",
            normalizeServerUrlOrNull("https://sync.example.com"),
        )
        assertEquals(
            "https://sync.example.com/",
            normalizeServerUrlOrNull("https://sync.example.com/"),
        )
    }

    @Test
    fun `surrounding whitespace is trimmed`() {
        assertEquals(
            "https://sync.example.com/",
            normalizeServerUrlOrNull("  https://sync.example.com  "),
        )
    }

    @Test
    fun `port and path are preserved`() {
        assertEquals(
            "http://sync.example.com:8080/timetracker/",
            normalizeServerUrlOrNull("http://sync.example.com:8080/timetracker"),
        )
    }

    @Test
    fun `scheme and host are lowercased`() {
        assertEquals(
            "https://sync.example.com/",
            normalizeServerUrlOrNull("HTTPS://Sync.Example.Com"),
        )
    }

    @Test
    fun `query and fragment are dropped`() {
        assertEquals(
            "https://sync.example.com/",
            normalizeServerUrlOrNull("https://sync.example.com?token=x#frag"),
        )
    }
}
