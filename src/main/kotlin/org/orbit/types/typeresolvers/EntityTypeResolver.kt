package org.orbit.types.typeresolvers

import org.orbit.core.nodes.EntityDefNode
import org.orbit.core.nodes.Node
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.types.components.Context
import org.orbit.types.components.Entity
import org.orbit.types.components.TypeProtocol

interface TypeResolver<N: Node, T: TypeProtocol> {
    val node: N
    val binding: Binding

    fun resolve(environment: Environment, context: Context) : T
}

interface EntityTypeResolver<E: EntityDefNode, T: Entity> : TypeResolver<E, T> {
    override val node: E
    override val binding: Binding
}