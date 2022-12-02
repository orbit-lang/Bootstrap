package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.TypeLambdaInvocationNode
import org.orbit.frontend.extensions.annotate
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object TypeLambdaInvocationPathResolver : IPathResolver<TypeLambdaInvocationNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: TypeLambdaInvocationNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        input.typeIdentifierNode.annotate(input.getGraphID(), Annotations.graphId)

        val result = pathResolverUtil.resolve(input.typeIdentifierNode, pass, environment, graph)

        input.arguments.forEach {
            it.annotate(input.getGraphID(), Annotations.graphId)
            pathResolverUtil.resolve(it, pass, environment, graph)
        }

        return result
    }
}