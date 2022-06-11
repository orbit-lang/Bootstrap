package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.ParameterNode
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object ParameterNodePathResolver : PathResolver<ParameterNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: ParameterNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
        val result = pathResolverUtil.resolve(input.typeNode, pass, environment, graph)

        input.defaultValue?.let { pathResolverUtil.resolve(it, pass, environment, graph) }

        return result
    }
}