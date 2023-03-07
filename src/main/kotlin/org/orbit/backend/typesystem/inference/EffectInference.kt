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
        val domain = TypeInferenceUtils.inferAll(node.lambda.domain, env)
        val codomain = TypeInferenceUtils.infer(node.lambda.codomain, env)
        val nEffect = IType.Effect(node.identifier.getPath(), domain, codomain)

        env.add(nEffect)

        return nEffect
    }
}