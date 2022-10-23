package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.TupleTypeNode

object TupleTypeInference : ITypeInference<TupleTypeNode, ITypeEnvironment> {
    override fun infer(node: TupleTypeNode, env: ITypeEnvironment): AnyType {
        val lType = TypeInferenceUtils.infer(node.left, env)
        val rType = TypeInferenceUtils.infer(node.right, env)

        return IType.Tuple(lType, rType)
    }
}