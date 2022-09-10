package org.orbit.graph.pathresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.MethodDelegateNode
import org.orbit.frontend.extensions.annotate
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object MethodDelegatePathResolver : PathResolver<MethodDelegateNode>, KoinComponent {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: MethodDelegateNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
        input.delegate.annotate(input.getGraphID(), Annotations.graphId)
        pathResolverUtil.resolve(input.delegate, PathResolver.Pass.Initial, environment, graph)

        return PathResolver.Result.Success(Path.empty)
    }
}