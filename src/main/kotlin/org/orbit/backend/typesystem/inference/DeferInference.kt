package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.DeferNode

object DeferInference : ITypeInference<DeferNode, ISelfTypeEnvironment> {
    override fun infer(node: DeferNode, env: ISelfTypeEnvironment): AnyType {
        val nEnv = when (val ret = node.returnValueIdentifier) {
            null -> env
            else -> {
                val self = when (val t = env.getSelfType()) {
                    is Signature -> t.returns
                    else -> t
                }

                SelfTypeEnvironment(env, self).apply { bind(ret.identifier, self, ret.index) }
            }
        }

        TypeInferenceUtils.infer(node.blockNode, nEnv)

        return Always
    }
}