package org.orbit.core.components

import org.orbit.core.phase.Observable

class CompilationEventBus {
    val events = Observable<CompilationEvent>()

    fun notify(event: CompilationEvent) {
        events.post(event)
    }
}