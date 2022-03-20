package org.orbit.types.next.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.PairNode
import org.orbit.core.nodes.TraitDefNode
import org.orbit.types.next.components.Field
import org.orbit.types.next.components.FieldContract
import org.orbit.types.next.components.Trait
import org.orbit.util.Invocation

object TraitContractsPhase : TypePhase<TraitDefNode, Trait>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun run(input: TypePhaseData<TraitDefNode>): Trait {
        val trait = input.inferenceUtil.inferAs<TraitDefNode, Trait>(input.node)
        val fields = input.inferenceUtil.inferAllAs<PairNode, Field>(input.node.propertyPairs)
        val fieldContracts = fields.map { FieldContract(trait, it) }

        // TODO - Signatures
        return Trait(trait.fullyQualifiedName, fieldContracts)
    }
}