package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Decl
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.ContextNode

object ContextInference : ITypeInference<ContextNode> {
    override fun infer(node: ContextNode, env: Env): AnyType {
        val nEnv = env.extend(Decl.Clone(node.getPath().toString(OrbitMangler)))

        node.typeVariables.forEach {
            val name = it.getPath().toString(OrbitMangler)

            nEnv.extendInPlace(Decl.TypeVariable(name))
        }

        TypeSystemUtils.inferAll(node.body, nEnv)

        env.extendInPlace(Decl.Context(nEnv))

        return nEnv
    }
}