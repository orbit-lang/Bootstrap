package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.utils.TypeSystemUtilsOLD
import org.orbit.core.nodes.TupleTypeNode

object TupleTypeInference : ITypeInferenceOLD<TupleTypeNode> {
    override fun infer(node: TupleTypeNode, env: Env): AnyType {
        val left = TypeSystemUtilsOLD.infer(node.left, env)
        val right = TypeSystemUtilsOLD.infer(node.right, env)

        return IType.Tuple(left, right)
    }
}