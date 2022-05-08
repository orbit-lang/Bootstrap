package org.orbit.graph.pathresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.TypeOfNode
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object TypeOfPathResolver : PathResolver<TypeOfNode>, KoinComponent {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: TypeOfNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
        return pathResolverUtil.resolve(input.expressionNode, pass, environment, graph)
    }
}