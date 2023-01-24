package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IMutableTypeEnvironment
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.getPath
import org.orbit.core.nodes.EffectNode
import org.orbit.core.nodes.ParameterNode

object EffectInference : ITypeInference<EffectNode, IMutableTypeEnvironment> {
    override fun infer(node: EffectNode, env: IMutableTypeEnvironment): AnyType {
        val parameters = TypeInferenceUtils.inferAllAs<ParameterNode, IType.Property>(node.parameters, env)
        val nEffect = IType.Effect(node.identifier.getPath(), parameters.map { Pair(it.id, it.type) })

        env.add(nEffect)

        return nEffect
    }
}