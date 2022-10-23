package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IMutableTypeEnvironment
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.getPath
import org.orbit.core.nodes.TypeAliasNode

object TypeAliasInference : ITypeInference<TypeAliasNode, IMutableTypeEnvironment> {
    override fun infer(node: TypeAliasNode, env: IMutableTypeEnvironment): AnyType {
        val type = TypeInferenceUtils.infer(node.targetTypeIdentifier, env)
        val path = node.getPath()

        env.add(IType.Alias(path, type))

        return type
    }
}