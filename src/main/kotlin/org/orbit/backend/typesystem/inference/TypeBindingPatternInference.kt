package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.TypeBindingPatternNode

object TypeBindingPatternInference : ITypeInference<TypeBindingPatternNode, ITypeEnvironment> {
    override fun infer(node: TypeBindingPatternNode, env: ITypeEnvironment): AnyType
        = TypeInferenceUtils.infer(node.typeIdentifier, env)
}