package org.orbit.types.next.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.getPath
import org.orbit.core.nodes.EntityDefNode
import org.orbit.core.nodes.TypeDefNode
import org.orbit.types.next.components.Type
import org.orbit.types.next.inference.TypeReference
import org.orbit.util.Invocation

object TypeStubPhase : EntityStubPhase<TypeDefNode, Type>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun run(input: TypePhaseData<TypeDefNode>): Type = Type(input.node.getPath())
}
