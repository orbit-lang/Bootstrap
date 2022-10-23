package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.StructTypeNode

object StructTypeInference : ITypeInference<StructTypeNode, ITypeEnvironment> {
    override fun infer(node: StructTypeNode, env: ITypeEnvironment): AnyType {
        val members = node.members.map {
            val type = TypeInferenceUtils.infer(it.typeExpressionNode, env)

            Pair(it.identifierNode.identifier, type)
        }

        return IType.Struct(members)
    }
}