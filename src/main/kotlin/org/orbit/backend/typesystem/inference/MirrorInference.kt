package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.MirrorNode

object MirrorInference : ITypeInference<MirrorNode, ITypeEnvironment> {
    override fun infer(node: MirrorNode, env: ITypeEnvironment): AnyType {
        return TypeInferenceUtils.infer(node.expressionNode, env)
    }
}