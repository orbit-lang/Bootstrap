package org.orbit.types.typeresolvers

import org.orbit.core.nodes.EntityDefNode
import org.orbit.core.nodes.Node
import org.orbit.core.phase.Phase
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.types.components.Context
import org.orbit.types.components.Entity
import org.orbit.types.components.TypeProtocol

interface TypeResolver<N: Node, T: TypeProtocol> : Phase<TypeResolver.Input, T> {
    data class Input(val environment: Environment, val context: Context)

    val node: N
    val binding: Binding

    fun resolve(environment: Environment, context: Context) : T

    override fun execute(input: Input): T {
        return resolve(input.environment, input.context)
    }
}

interface EntityTypeResolver<E: EntityDefNode, T: Entity> : TypeResolver<E, T> {
    override val node: E
    override val binding: Binding
}