package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.utils.TypeSystemUtilsOLD
import org.orbit.core.nodes.TypeOfNode

object TypeOfInference : ITypeInferenceOLD<TypeOfNode> {
    override fun infer(node: TypeOfNode, env: Env): AnyType {
        val typeAnnotation = TypeSystemUtilsOLD.popTypeAnnotation() ?: IType.Unit
        val type = TypeSystemUtilsOLD.infer(node.expressionNode, env)

        println(type)

        // `typeOf` expressions are Type "neutral"
        return typeAnnotation
    }
}