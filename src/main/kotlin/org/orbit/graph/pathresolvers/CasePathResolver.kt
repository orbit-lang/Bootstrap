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

object CasePathResolver : IPathResolver<CaseNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: CaseNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        input.pattern.annotate(input.getGraphID(), Annotations.graphId)
        input.body.annotate(input.getGraphID(), Annotations.graphId)

        pathResolverUtil.resolve(input.pattern, IPathResolver.Pass.Initial, environment, graph)
        pathResolverUtil.resolve(input.body, IPathResolver.Pass.Initial, environment, graph)

        return IPathResolver.Result.Success(Path.empty)
    }
}