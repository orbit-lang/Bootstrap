package org.orbit.graph.pathresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.CheckNode
import org.orbit.frontend.extensions.annotate
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object CheckPathResolver : IPathResolver<CheckNode>, KoinComponent {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: CheckNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        input.left.annotate(input.getGraphID(), Annotations.graphId)
        input.right.annotate(input.getGraphID(), Annotations.graphId)

        pathResolverUtil.resolve(input.left, pass, environment, graph)

        return pathResolverUtil.resolve(input.right, pass, environment, graph)
    }
}