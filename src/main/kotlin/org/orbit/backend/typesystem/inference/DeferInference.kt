package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.ISelfTypeEnvironment
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.SelfTypeEnvironment
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.DeferNode

object DeferInference : ITypeInference<DeferNode, ISelfTypeEnvironment> {
    override fun infer(node: DeferNode, env: ISelfTypeEnvironment): AnyType {
        val nEnv = when (val ret = node.returnValueIdentifier) {
            null -> env
            else -> {
                val self = when (val t = env.getSelfType()) {
                    is IType.Signature -> t.returns
                    else -> t
                }

                SelfTypeEnvironment(env, self).apply { bind(ret.identifier, self) }
            }
        }

        TypeInferenceUtils.infer(node.blockNode, nEnv)

        return IType.Always
    }
}