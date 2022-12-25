package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IMutableTypeEnvironment
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.fork
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.ITypeEffectExpressionNode
import org.orbit.core.nodes.ProjectionEffectNode
import org.orbit.core.nodes.TypeEffectNode

private object ProjectionEffectInference : ITypeInference<ProjectionEffectNode, IMutableTypeEnvironment> {
    override fun infer(node: ProjectionEffectNode, env: IMutableTypeEnvironment): AnyType {
        val type = TypeInferenceUtils.infer(node.type, env)
        val trait = TypeInferenceUtils.infer(node.trait, env)

        return IType.ProjectionEffect(type, trait)
    }
}

private object AnyTypeEffectInference : ITypeInference<ITypeEffectExpressionNode, IMutableTypeEnvironment> {
    override fun infer(node: ITypeEffectExpressionNode, env: IMutableTypeEnvironment): AnyType = when (node) {
        is ProjectionEffectNode -> ProjectionEffectInference.infer(node, env)
        else -> TODO("Unsupported Type Effect: $node")
    }
}

object TypeEffectInference : ITypeInference<TypeEffectNode, IMutableTypeEnvironment> {
    override fun infer(node: TypeEffectNode, env: IMutableTypeEnvironment): AnyType {
        val typeVariables = node.parameters.map { IType.TypeVar(it.getTypeName()) }
        val nEnv = env.fork()

        typeVariables.forEach { nEnv.add(it) }

        val body = AnyTypeEffectInference.infer(node.body, nEnv) as IType.ITypeEffect

        return IType.TypeEffect(node.identifier.getTypeName(), typeVariables, listOf(body))
    }
}