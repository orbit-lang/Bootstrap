package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.LambdaTypeNode
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object LambdaTypePathResolver : IPathResolver<LambdaTypeNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: LambdaTypeNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        pathResolverUtil.resolveAll(input.domain, pass, environment, graph)

        return pathResolverUtil.resolve(input.codomain, pass, environment, graph)
    }
}