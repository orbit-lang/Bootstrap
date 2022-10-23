package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.TupleLiteralNode

object TupleLiteralInference : ITypeInference<TupleLiteralNode, ITypeEnvironment> {
    override fun infer(node: TupleLiteralNode, env: ITypeEnvironment): AnyType {
        val lType = TypeInferenceUtils.infer(node.value.first, env)
        val rType = TypeInferenceUtils.infer(node.value.second, env)

        return IType.Tuple(lType, rType)
    }
}