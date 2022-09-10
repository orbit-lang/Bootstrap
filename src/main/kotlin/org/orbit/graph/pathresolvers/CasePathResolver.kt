package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.CaseNode
import org.orbit.frontend.extensions.annotate
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object CasePathResolver : PathResolver<CaseNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: CaseNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
        input.pattern.annotate(input.getGraphID(), Annotations.graphId)
        input.body.annotate(input.getGraphID(), Annotations.graphId)

        pathResolverUtil.resolve(input.pattern, PathResolver.Pass.Initial, environment, graph)
        pathResolverUtil.resolve(input.body, PathResolver.Pass.Initial, environment, graph)

        return PathResolver.Result.Success(Path.empty)
    }
}