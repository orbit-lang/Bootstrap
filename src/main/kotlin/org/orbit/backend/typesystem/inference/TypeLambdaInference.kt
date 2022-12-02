package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.TypeLambdaNode

object TypeLambdaInference : ITypeInference<TypeLambdaNode, IMutableTypeEnvironment> {
    override fun infer(node: TypeLambdaNode, env: IMutableTypeEnvironment): AnyType {
        val nEnv = env.fork()

        val domain = node.domain.map {
            val type = IType.TypeVar(it.getTypeName())

            nEnv.add(type)

            type
        }

        val codomain = TypeInferenceUtils.infer(node.codomain, nEnv)

        return domain.arrowOf(codomain)
    }
}