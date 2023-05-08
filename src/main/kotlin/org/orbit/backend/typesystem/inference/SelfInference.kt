package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.ISelfTypeEnvironment
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.core.nodes.SelfNode
import org.orbit.util.Invocation

object SelfInference : ITypeInference<SelfNode, ITypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: SelfNode, env: ITypeEnvironment): AnyType = when (env) {
        is ISelfTypeEnvironment -> env.getSelfType()
        else -> throw invocation.make<TypeSystem>("Cannot infer Self Type in the current context")
    }
}