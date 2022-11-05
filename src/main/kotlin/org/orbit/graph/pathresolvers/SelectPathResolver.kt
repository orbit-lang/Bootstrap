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

object SelectPathResolver : IPathResolver<SelectNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: SelectNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        input.condition.annotate(input.getGraphID(), Annotations.graphId)
        pathResolverUtil.resolve(input.condition, IPathResolver.Pass.Initial, environment, graph)

        input.cases.forEach { it.annotate(input.getGraphID(), Annotations.graphId) }
        pathResolverUtil.resolveAll(input.cases, IPathResolver.Pass.Initial, environment, graph)

        return IPathResolver.Result.Success(Path.empty)
    }
}