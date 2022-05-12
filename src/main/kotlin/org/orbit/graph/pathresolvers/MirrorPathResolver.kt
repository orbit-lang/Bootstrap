package org.orbit.graph.pathresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.MirrorNode
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object MirrorPathResolver : PathResolver<MirrorNode>, KoinComponent {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: MirrorNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result
        = pathResolverUtil.resolve(input.expressionNode, pass, environment, graph)
}