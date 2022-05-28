package org.orbit.types.next.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.core.nodes.PairNode
import org.orbit.core.nodes.TraitDefNode
import org.orbit.types.next.components.*
import org.orbit.types.next.inference.AnyInferenceContext
import org.orbit.util.Invocation

object TraitContractsPhase : TypePhase<TraitDefNode, Trait>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun run(input: TypePhaseData<TraitDefNode>): Trait {
        val trait = input.inferenceUtil.inferAs<TraitDefNode, Trait>(input.node)
        val nInferenceUtil = input.inferenceUtil.derive(self = trait)
        val fields = nInferenceUtil.inferAllAs<PairNode, Field>(input.node.propertyPairs, AnyInferenceContext(PairNode::class.java))
        val fieldContracts = fields.map { FieldContract(trait, it) }
        val signatures = nInferenceUtil.inferAllAs<MethodSignatureNode, ISignature>(input.node.signatures, AnyInferenceContext(MethodSignatureNode::class.java))
        val signatureContracts = signatures.map { SignatureContract(trait, it) }

        // TODO - Signatures
        return Trait(trait.fullyQualifiedName, fieldContracts + signatureContracts)
    }
}