package org.orbit.backend.typesystem.components

sealed interface RefEntry {
    data class Use(val ref: Ref) : RefEntry
}