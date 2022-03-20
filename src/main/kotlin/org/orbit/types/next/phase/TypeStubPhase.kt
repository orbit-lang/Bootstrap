package org.orbit.types.next.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.nodes.TypeDefNode
import org.orbit.types.next.components.Type
import org.orbit.util.Invocation

object TypeStubPhase : EntityStubPhase<TypeDefNode, Type>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun constructStub(path: Path): Type = Type(path)
}