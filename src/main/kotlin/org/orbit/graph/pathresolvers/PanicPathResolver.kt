package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.PanicNode
import org.orbit.frontend.extensions.annotate
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object PanicPathResolver : IPathResolver<PanicNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: PanicNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        input.expr.annotate(input.getGraphID(), Annotations.graphId)
        pathResolverUtil.resolve(input.expr, IPathResolver.Pass.Initial, environment, graph)

        return IPathResolver.Result.Success(OrbitMangler.unmangle("!"))
    }
}