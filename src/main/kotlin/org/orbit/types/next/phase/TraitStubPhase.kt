package org.orbit.types.next.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.getPath
import org.orbit.core.nodes.EntityDefNode
import org.orbit.core.nodes.TraitDefNode
import org.orbit.types.next.components.Trait
import org.orbit.util.Invocation

object TraitStubPhase : EntityStubPhase<TraitDefNode, Trait>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun run(input: TypePhaseData<TraitDefNode>): Trait = Trait(input.node.getPath())
}