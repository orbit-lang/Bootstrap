package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.ContextNode
import org.orbit.precess.backend.components.Decl
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType

object ContextInference : ITypeInference<ContextNode> {
    override fun infer(node: ContextNode, env: Env): IType<*> {
        val nEnv = env.extend(Decl.Clone(node.getPath().toString(OrbitMangler)))

        node.typeVariables.forEach {
            val name = it.getPath().toString(OrbitMangler)

            nEnv.extendInPlace(Decl.Type(IType.Type(name), emptyMap()))
        }

        TypeSystemUtils.inferAll(node.body, nEnv)

        env.extendInPlace(Decl.Context(nEnv))

        return nEnv
    }
}