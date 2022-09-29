package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.util.Invocation

object TypeIdentifierInference : ITypeInference<TypeIdentifierNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: TypeIdentifierNode, env: Env): AnyType {
        if (node.isDiscard) return IType.Always

        val path = node.getPath()

        return env.getElement(path.toString(OrbitMangler))
            ?: throw invocation.make<TypeSystem>("Undefined Type `${path.toString(OrbitMangler)}`", node)
    }
}