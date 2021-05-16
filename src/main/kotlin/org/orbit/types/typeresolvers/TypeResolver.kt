package org.orbit.types.typeresolvers

import org.orbit.graph.Binding
import org.orbit.graph.Environment
import org.orbit.types.components.Context
import org.orbit.types.components.TypeProtocol

interface TypeResolver {
    fun resolve(environment: Environment, context: Context, binding: Binding) : TypeProtocol
}