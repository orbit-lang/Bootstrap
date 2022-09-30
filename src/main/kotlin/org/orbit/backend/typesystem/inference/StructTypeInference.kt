package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.StructTypeNode

object StructTypeInference : ITypeInference<StructTypeNode> {
    override fun infer(node: StructTypeNode, env: Env): AnyType {
        val members = node.members.map {
            val type = TypeSystemUtils.infer(it.typeExpressionNode, env)

            Pair(it.identifierNode.identifier, type)
        }

        return IType.Struct(members)
    }
}