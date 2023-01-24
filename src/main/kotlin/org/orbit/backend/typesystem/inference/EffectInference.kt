package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IMutableTypeEnvironment
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.EffectNode

object EffectInference : ITypeInference<EffectNode, IMutableTypeEnvironment> {
    override fun infer(node: EffectNode, env: IMutableTypeEnvironment): AnyType {
        val parameters = TypeInferenceUtils.inferAll(node.parameters, env)
        val nEffect = IType.Effect(node.identifier.value, parameters)

        env.add(nEffect)

        return nEffect
    }
}