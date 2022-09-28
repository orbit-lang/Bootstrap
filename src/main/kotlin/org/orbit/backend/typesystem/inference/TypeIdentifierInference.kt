package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.Expr
import org.orbit.precess.backend.components.IType
import org.orbit.util.Invocation

object TypeIdentifierInference : ITypeInference<TypeIdentifierNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: TypeIdentifierNode, env: Env): AnyType {
        val path = node.getPath()

        return env.getElement(path.toString(OrbitMangler))
            ?: throw invocation.make<TypeSystem>("Undefined Type `${path.toString(OrbitMangler)}`", node)
    }
}