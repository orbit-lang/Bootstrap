package org.orbit.types.next.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.ContextAwareNode
import org.orbit.core.nodes.ContextExpressionNode
import org.orbit.types.next.components.ContextInstantiation
import org.orbit.types.next.components.TypeComponent
import org.orbit.util.Invocation

object ContextAwarePhase : TypePhase<ContextAwareNode, TypeComponent>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun run(input: TypePhaseData<ContextAwareNode>): TypeComponent {
        val type = input.inferenceUtil.infer(input.node)
        val contextNode = input.node.context ?: return type
        val nInferenceUtil = input.inferenceUtil.derive(self = type)
        val context = nInferenceUtil.inferAs<ContextExpressionNode, ContextInstantiation>(contextNode)

        input.inferenceUtil.addContext(type, context)

        return type
    }
}