package org.orbit.core.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface CompilationEventBusAware {
    val compilationEventBus: CompilationEventBus
}

object CompilationEventBusAwareImpl : CompilationEventBusAware, KoinComponent {
    override val compilationEventBus: CompilationEventBus by inject()
}