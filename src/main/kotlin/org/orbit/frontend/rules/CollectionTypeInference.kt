package org.orbit.frontend.rules

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.inference.ITypeInference
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.CollectionTypeNode

object CollectionTypeInference : ITypeInference<CollectionTypeNode, ITypeEnvironment> {
    override fun infer(node: CollectionTypeNode, env: ITypeEnvironment): AnyType {
        val elementType = TypeInferenceUtils.infer(node.elementType, env)

        return IType.Array(elementType)
    }
}