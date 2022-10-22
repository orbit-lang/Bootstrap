package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.utils.TypeSystemUtilsOLD
import org.orbit.core.nodes.TupleLiteralNode

object TupleLiteralInference : ITypeInferenceOLD<TupleLiteralNode> {
    override fun infer(node: TupleLiteralNode, env: Env): AnyType {
        val left = TypeSystemUtilsOLD.infer(node.value.first, env)
        val right = TypeSystemUtilsOLD.infer(node.value.second, env)

        return IType.Tuple(left, right)
    }
}