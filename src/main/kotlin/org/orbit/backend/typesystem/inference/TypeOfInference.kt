package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnnotatedTypeEnvironment
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.TypeOfNode

object TypeOfInference : ITypeInference<TypeOfNode, ITypeEnvironment> {
    override fun infer(node: TypeOfNode, env: ITypeEnvironment): AnyType {
        val typeAnnotation = (env as? AnnotatedTypeEnvironment)?.typeAnnotation ?: IType.Unit
        val type = TypeInferenceUtils.infer(node.expressionNode, env)

        println(type)

        // `typeOf` expressions are Type "neutral"
        return typeAnnotation
    }
}