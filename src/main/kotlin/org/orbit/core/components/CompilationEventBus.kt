package org.orbit.core.components

import org.orbit.core.phase.Observable
import java.io.Serializable

class CompilationEventBus : Serializable {
    val events = Observable<CompilationEvent>()

    fun notify(event: CompilationEvent) {
        events.post(event)
    }
}