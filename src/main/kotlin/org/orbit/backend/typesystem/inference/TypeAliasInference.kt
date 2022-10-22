package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Decl
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.utils.TypeSystemUtilsOLD
import org.orbit.core.getPath
import org.orbit.core.nodes.TypeAliasNode

object TypeAliasInference : ITypeInferenceOLD<TypeAliasNode> {
    override fun infer(node: TypeAliasNode, env: Env): AnyType {
        val type = TypeSystemUtilsOLD.infer(node.targetTypeIdentifier, env)
        val path = node.getPath()

        env.extendInPlace(Decl.TypeAlias(path, type))

        return type
    }
}