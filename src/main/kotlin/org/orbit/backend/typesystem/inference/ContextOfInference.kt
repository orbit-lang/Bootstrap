package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.inference.evidence.asSuccessOrNull
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.ContextOfNode

object ContextOfInference : ITypeInference<ContextOfNode> {
    override fun infer(node: ContextOfNode, env: Env): AnyType {
        val evidence = TypeSystemUtils.gatherEvidence(node.typeExpressionNode)

        evidence.asSuccessOrNull()?.let { println(it) }

        return IType.Always
    }
}