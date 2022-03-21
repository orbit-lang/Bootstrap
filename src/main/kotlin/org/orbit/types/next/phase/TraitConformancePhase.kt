package org.orbit.types.next.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.TypeDefNode
import org.orbit.core.nodes.TypeExpressionNode
import org.orbit.types.next.components.ITrait
import org.orbit.types.next.components.Type
import org.orbit.types.next.inference.AnyInferenceContext
import org.orbit.util.Invocation

object TraitConformancePhase : TypePhase<TypeDefNode, Type>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun run(input: TypePhaseData<TypeDefNode>): Type {
        val type = input.inferenceUtil.inferAs<TypeDefNode, Type>(input.node)
        val traits = input.inferenceUtil.inferAllAs<TypeExpressionNode, ITrait>(input.node.traitConformances, AnyInferenceContext(TypeExpressionNode::class.java))

        traits.forEach { input.inferenceUtil.addConformance(type, it) }

        return type
    }
}