package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.StructuralPatternNode

object StructuralPatternInference : ITypeInference<StructuralPatternNode> {
    override fun infer(node: StructuralPatternNode, env: Env): AnyType {
        val bindingTypes = TypeSystemUtils.inferAll(node.bindings, env)

        return IType.Always
    }
}