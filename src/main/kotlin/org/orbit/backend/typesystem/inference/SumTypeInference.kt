package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.SumTypeNode

object SumTypeInference : ITypeInference<SumTypeNode, ITypeEnvironment> {
    override fun infer(node: SumTypeNode, env: ITypeEnvironment): AnyType {
        val lType = TypeInferenceUtils.infer(node.left, env)
        val rType = TypeInferenceUtils.infer(node.right, env)

        return IType.Sum(lType, rType)
    }
}