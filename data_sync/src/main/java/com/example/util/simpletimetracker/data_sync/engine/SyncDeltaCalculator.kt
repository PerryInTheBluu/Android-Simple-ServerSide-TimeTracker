package com.example.util.simpletimetracker.data_sync.engine

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.inject.Inject

/**
 * Pure delta computation between the current local state and the mirror of
 * the last successful push. Kept free of Android and database dependencies
 * so it can be unit tested on the JVM.
 *
 * The mirror maps "entityType:entityId" to the content hash of the payload
 * that was last pushed for that entity. Entities missing from the mirror
 * (or with a changed hash) are pushed again; mirror entries missing from
 * the local state were deleted locally and become tombstones.
 */
class SyncDeltaCalculator @Inject constructor() {

    data class Candidate(
        val entityType: String,
        val entityId: String,
        val payload: Map<String, Any?>,
    )

    data class Delta(
        val upserts: List<Candidate>,
        val tombstones: List<Candidate>,
    )

    fun calculate(
        candidates: List<Candidate>,
        mirror: Map<String, String>,
    ): Delta {
        val seen = mutableSetOf<String>()
        val upserts = mutableListOf<Candidate>()
        candidates.forEach { candidate ->
            val key = key(candidate.entityType, candidate.entityId)
            seen.add(key)
            if (mirror[key] != contentHash(candidate.payload)) {
                upserts.add(candidate)
            }
        }
        val tombstones = mirror.keys
            .filter { it !in seen }
            .map { mirroredKey ->
                Candidate(
                    entityType = mirroredKey.substringBefore(KEY_SEPARATOR),
                    entityId = mirroredKey.substringAfter(KEY_SEPARATOR),
                    payload = emptyMap(),
                )
            }
        return Delta(upserts = upserts, tombstones = tombstones)
    }

    fun contentHash(payload: Map<String, Any?>): String {
        val canonical = payload.entries
            .sortedBy { it.key }
            .joinToString(separator = ";") { "${it.key}=${it.value}" }
        return sha256(canonical)
    }

    fun key(entityType: String, entityId: String): String = "$entityType$KEY_SEPARATOR$entityId"

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_SEPARATOR = ":"
    }
}
