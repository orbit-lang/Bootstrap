package org.orbit.core.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.Serializable

interface CompilationEventBusAware {
    val compilationEventBus: CompilationEventBus
}

object CompilationEventBusAwareImpl : CompilationEventBusAware, KoinComponent, Serializable {
    override val compilationEventBus: CompilationEventBus by inject()
}