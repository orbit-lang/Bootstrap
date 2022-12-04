package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.TypeLambdaConstraintNode

object TypeLambdaConstraintInference : ITypeInference<TypeLambdaConstraintNode, ITypeEnvironment> {
    override fun infer(node: TypeLambdaConstraintNode, env: ITypeEnvironment): AnyType {
        return TypeInferenceUtils.infer(node.invocation, env)
    }
}