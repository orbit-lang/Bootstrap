package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.CollectionLiteralNode
import org.orbit.util.Invocation

object CollectionLiteralInference : ITypeInference<CollectionLiteralNode, ITypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: CollectionLiteralNode, env: ITypeEnvironment): AnyType {
        val elements = TypeInferenceUtils.inferAll(node.elements, env)

        // TODO - Read collection type from delegate (if specified)
        // TODO - Homogeneity

        val element = elements[0]

        for (pair in elements.withIndex()) {
            if (!TypeUtils.checkEq(env, pair.value, element)) {
                throw invocation.make<TypeSystem>("Raw collection literals (arrays) are homogeneous: found mismatched element Types ${pair.value} & $element", node.elements[pair.index])
            }
        }

        return IType.Array(element)
    }
}