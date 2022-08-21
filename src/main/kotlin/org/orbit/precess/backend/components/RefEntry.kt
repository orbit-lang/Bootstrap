package org.orbit.precess.backend.components

sealed interface RefEntry {
    data class Use(val ref: Ref) : RefEntry
}