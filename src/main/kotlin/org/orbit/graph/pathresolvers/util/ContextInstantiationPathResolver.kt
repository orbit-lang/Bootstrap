package org.orbit.graph.pathresolvers.util

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.ContextInstantiationNode
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.PathResolver
import org.orbit.util.Invocation

object ContextInstantiationPathResolver : PathResolver<ContextInstantiationNode>, KoinComponent {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: ContextInstantiationNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
        input.contextIdentifierNode.annotate(input.getGraphID(), Annotations.GraphID)
        input.typeVariables.forEach { it.annotate(input.getGraphID(), Annotations.GraphID) }

        val result = pathResolverUtil.resolve(input.contextIdentifierNode, pass, environment, graph)

        pathResolverUtil.resolveAll(input.typeVariables, pass, environment, graph)

        return result
    }
}