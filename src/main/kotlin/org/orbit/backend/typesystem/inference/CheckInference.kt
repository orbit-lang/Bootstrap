package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnnotatedTypeEnvironment
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.CheckNode

object CheckInference : ITypeInference<CheckNode, ITypeEnvironment> {
    override fun infer(node: CheckNode, env: ITypeEnvironment): AnyType {
        val typeAnnotation = (env as? AnnotatedTypeEnvironment)?.typeAnnotation ?: IType.Unit
        val lType = TypeInferenceUtils.infer(node.left, env)
        val rType = TypeInferenceUtils.infer(node.right, env)
        val eq = TypeUtils.checkEq(env, lType, rType)

        println("Type Equality for $lType == $rType -- $eq")

        return typeAnnotation
    }
}