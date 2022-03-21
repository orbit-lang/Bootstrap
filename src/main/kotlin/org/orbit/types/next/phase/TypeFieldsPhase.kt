package org.orbit.types.next.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.PairNode
import org.orbit.core.nodes.TypeDefNode
import org.orbit.types.next.components.Field
import org.orbit.types.next.components.Type
import org.orbit.types.next.inference.AnyInferenceContext
import org.orbit.types.next.inference.TypeReference
import org.orbit.util.Invocation

object TypeFieldsPhase : TypePhase<TypeDefNode, Type>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun run(input: TypePhaseData<TypeDefNode>): Type {
        val typeStub = input.inferenceUtil.inferAs<TypeDefNode, Type>(input.node)
        val nInferenceUtil = input.inferenceUtil.derive(retainsTypeMap = true, retainsBindingScope = true, TypeReference(typeStub.fullyQualifiedName))
        val fields = nInferenceUtil.inferAllAs<PairNode, Field>(input.node.propertyPairs, AnyInferenceContext(PairNode::class.java))

        return Type(typeStub.fullyQualifiedName, fields)
    }
}