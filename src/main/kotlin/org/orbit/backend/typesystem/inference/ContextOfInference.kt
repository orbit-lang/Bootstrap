package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.ContextOfNode

object ContextOfInference : ITypeInference<ContextOfNode, ITypeEnvironment> {
    override fun infer(node: ContextOfNode, env: ITypeEnvironment): AnyType {
        val type = TypeInferenceUtils.infer(node.expressionNode, env)
        val decl = env.getTypeOrNull(type.getCanonicalName())
            ?: env.getAllTypes().lastOrNull { TypeUtils.checkEq(env, it.component, type) }
            ?: return Always

        if (decl.context.isComplete()) {
            println(decl.context)

            return Always
        }

        if (env.getCurrentContext().isComplete() && decl.context.getCanonicalName() == env.getCurrentContext().getCanonicalName()) {
            println(env.getCurrentContext())

            return Always
        }

        println(decl.context)

        return Always
    }
}