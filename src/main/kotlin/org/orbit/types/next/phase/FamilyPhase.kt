package org.orbit.types.next.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.getPath
import org.orbit.core.nodes.FamilyNode
import org.orbit.core.nodes.TypeDefNode
import org.orbit.types.next.components.Alias
import org.orbit.types.next.components.TypeFamily
import org.orbit.types.next.components.getPath
import org.orbit.types.next.inference.AnyInferenceContext
import org.orbit.util.Invocation

object FamilyPhase : EntityStubPhase<FamilyNode, TypeFamily<*>>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun run(input: TypePhaseData<FamilyNode>): TypeFamily<*> {
        val family = TypeFamily(input.node.getPath(), emptyList())

        input.inferenceUtil.declare(family)

        val members = input.inferenceUtil.inferAll(input.node.memberNodes, AnyInferenceContext(TypeDefNode::class.java))

        members.forEach {
            val path = Path(family.getPath(OrbitMangler).last()) + it.getPath(OrbitMangler).last()
            val alias = Alias(path, it)

            input.inferenceUtil.declare(alias)
        }

        val nFamily = TypeFamily(input.node.getPath(), members)

        input.inferenceUtil.declare(nFamily)

        return family
    }
}