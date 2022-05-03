package org.orbit.types.next.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.getPath
import org.orbit.core.nodes.FamilyNode
import org.orbit.core.nodes.TypeDefNode
import org.orbit.types.next.components.TypeFamily
import org.orbit.types.next.inference.AnyInferenceContext
import org.orbit.util.Invocation

object FamilyPhase : EntityStubPhase<FamilyNode, TypeFamily<*>>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun run(input: TypePhaseData<FamilyNode>): TypeFamily<*> {
        val members = input.inferenceUtil.inferAll(input.node.memberNodes, AnyInferenceContext(TypeDefNode::class.java))

        return TypeFamily(input.node.getPath(), members)
    }
}