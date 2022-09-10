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

object PanicPathResolver : PathResolver<PanicNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: PanicNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
        input.expr.annotate(input.getGraphID(), Annotations.graphId)
        pathResolverUtil.resolve(input.expr, PathResolver.Pass.Initial, environment, graph)

        return PathResolver.Result.Success(OrbitMangler.unmangle("!"))
    }
}