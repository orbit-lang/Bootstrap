package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.StructuralPatternNode

object StructuralPatternInference : ITypeInference<StructuralPatternNode, ITypeEnvironment> {
    override fun infer(node: StructuralPatternNode, env: ITypeEnvironment): AnyType {
        val bindingTypes = TypeInferenceUtils.inferAll(node.bindings, env)

        return IType.Always
    }
}