package org.orbit.types.typeresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.ApiDefNode
import org.orbit.core.nodes.Node
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.types.components.Api
import org.orbit.types.components.Context
import org.orbit.types.components.Type
import org.orbit.util.Invocation

class ApiTypeResolver(override val node: ApiDefNode, override val binding: Binding) : TypeResolver<ApiDefNode, Api>,
    KoinComponent {
    override val invocation: Invocation by inject()

    constructor(pair: Pair<ApiDefNode, Binding>) : this(pair.first, pair.second)

    override fun resolve(environment: Environment, context: Context): Api {
        val path = node.getPath()
        val requiredTypes = node.requiredTypes
            .map(Node::getPath)
            .mapNotNull { context.getTypeByPath(it) as? Type }

        // TODO - Traits & Standard (i.e. non-required) types
        return Api(path.toString(OrbitMangler), requiredTypes)
    }
}