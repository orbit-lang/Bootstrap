package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.core.nodes.IdentifierNode
import org.orbit.util.Invocation

object IdentifierInference : ITypeInference<IdentifierNode, ITypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: IdentifierNode, env: ITypeEnvironment): AnyType
        = env.getBinding(node.identifier)?.type
            ?: throw invocation.make<TypeSystem>("`${node.identifier}` is not defined in the current context", node)
}