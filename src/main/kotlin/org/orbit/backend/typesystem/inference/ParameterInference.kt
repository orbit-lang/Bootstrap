package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.ParameterNode

object ParameterInference : ITypeInference<ParameterNode, ITypeEnvironment> {
    override fun infer(node: ParameterNode, env: ITypeEnvironment): AnyType
        = TypeInferenceUtils.infer(node.typeNode, env)
}