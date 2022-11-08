package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.components.getTypeOrNull
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.ContextOfNode

object ContextOfInference : ITypeInference<ContextOfNode, ITypeEnvironment> {
    override fun infer(node: ContextOfNode, env: ITypeEnvironment): AnyType {
        val type = TypeInferenceUtils.infer(node.expressionNode, env)
        val decl = env.getTypeOrNull(type.getCanonicalName())
            ?: env.getAllTypes().firstOrNull { it.component.flatten(it.component, env).getCanonicalName() == type.getCanonicalName() }
            ?: return IType.Always

        println(decl.context)

        return IType.Always
    }
}