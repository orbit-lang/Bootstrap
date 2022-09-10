package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.SelectNode
import org.orbit.frontend.extensions.annotate
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object SelectPathResolver : PathResolver<SelectNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: SelectNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
        input.condition.annotate(input.getGraphID(), Annotations.graphId)
        pathResolverUtil.resolve(input.condition, PathResolver.Pass.Initial, environment, graph)

        input.cases.forEach { it.annotate(input.getGraphID(), Annotations.graphId) }
        pathResolverUtil.resolveAll(input.cases, PathResolver.Pass.Initial, environment, graph)

        return PathResolver.Result.Success(Path.empty)
    }
}