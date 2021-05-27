package org.orbit.core.components

import java.io.Serializable

interface CompilationEvent : Serializable {
    val identifier: String
}