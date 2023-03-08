package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.ForNode
import org.orbit.frontend.extensions.annotate
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object ForPathResolver : IPathResolver<ForNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: ForNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        input.iterable.annotate(input.getGraphID(), Annotations.graphId)
        input.body.annotate(input.getGraphID(), Annotations.graphId)

        pathResolverUtil.resolve(input.iterable, pass, environment, graph)

        return pathResolverUtil.resolve(input.body, pass, environment, graph)
    }
}