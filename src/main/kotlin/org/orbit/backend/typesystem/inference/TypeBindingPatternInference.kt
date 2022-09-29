package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.TypeBindingPatternNode

object TypeBindingPatternInference : ITypeInference<TypeBindingPatternNode> {
    override fun infer(node: TypeBindingPatternNode, env: Env): AnyType
        = TypeSystemUtils.infer(node.typeIdentifier, env)
}