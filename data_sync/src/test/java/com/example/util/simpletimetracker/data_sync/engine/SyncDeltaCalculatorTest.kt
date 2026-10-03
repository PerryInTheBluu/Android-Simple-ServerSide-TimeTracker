package com.example.util.simpletimetracker.data_sync.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncDeltaCalculatorTest {

    private val calculator = SyncDeltaCalculator()

    private fun activity(id: Long, name: String = "Lernen"): SyncDeltaCalculator.Candidate {
        return SyncDeltaCalculator.Candidate(
            entityType = "activity",
            entityId = "a$id",
            payload = mapOf(
                "id" to "a$id",
                "name" to name,
                "icon" to "ic_school_24px",
                "archived" to false,
            ),
        )
    }

    private fun entry(id: Long, comment: String = ""): SyncDeltaCalculator.Candidate {
        return SyncDeltaCalculator.Candidate(
            entityType = "time_entry",
            entityId = "e$id",
            payload = mapOf(
                "id" to "e$id",
                "activity_id" to "a1",
                "started_at" to "2026-01-01T08:00:00Z",
                "ended_at" to "2026-01-01T09:00:00Z",
                "duration_seconds" to 3600,
                "comment" to comment,
            ),
        )
    }

    @Test
    fun emptyMirrorPushesEverything() {
        val candidates = listOf(activity(1), entry(1))

        val delta = calculator.calculate(candidates, mirror = emptyMap())

        assertEquals(candidates, delta.upserts)
        assertTrue(delta.tombstones.isEmpty())
    }

    @Test
    fun unchangedEntitiesAreSkipped() {
        val candidates = listOf(activity(1), entry(1))
        val mirror = candidates.associate {
            calculator.key(it.entityType, it.entityId) to calculator.contentHash(it.payload)
        }

        val delta = calculator.calculate(candidates, mirror)

        assertTrue(delta.upserts.isEmpty())
        assertTrue(delta.tombstones.isEmpty())
    }

    @Test
    fun changedEntityIsPushedAgain() {
        val old = entry(1)
        val mirror = mapOf(
            calculator.key(old.entityType, old.entityId) to calculator.contentHash(old.payload),
        )
        val changed = entry(1, comment = "geändert")

        val delta = calculator.calculate(listOf(changed), mirror)

        assertEquals(listOf(changed), delta.upserts)
        assertTrue(delta.tombstones.isEmpty())
    }

    @Test
    fun missingMirrorEntriesBecomeTombstones() {
        val mirror = mapOf(
            calculator.key("activity", "a1") to calculator.contentHash(activity(1).payload),
            calculator.key("time_entry", "e1") to "hash-entry",
            calculator.key("time_entry", "e2") to "hash-entry-2",
        )

        val delta = calculator.calculate(listOf(activity(1)), mirror)

        assertTrue(delta.upserts.isEmpty())
        val tombstoneIds = delta.tombstones.map { "${it.entityType}:${it.entityId}" }
        assertEquals(
            listOf("time_entry:e1", "time_entry:e2").sorted(),
            tombstoneIds.sorted(),
        )
    }

    @Test
    fun contentHashIsStableAcrossMapOrder() {
        val payloadOne = mapOf(
            "id" to "e1",
            "comment" to "x",
            "archived" to false,
        )
        val payloadTwo = mapOf(
            "archived" to false,
            "comment" to "x",
            "id" to "e1",
        )

        assertEquals(
            calculator.contentHash(payloadOne),
            calculator.contentHash(payloadTwo),
        )
    }
}
