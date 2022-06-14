package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.MethodReferenceNode
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object MethodReferencePathResolver : PathResolver<MethodReferenceNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: MethodReferenceNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
        input.typeExpressionNode.annotate(input.getGraphID(), Annotations.GraphID)

        return pathResolverUtil.resolve(input.typeExpressionNode, pass, environment, graph)
    }
}