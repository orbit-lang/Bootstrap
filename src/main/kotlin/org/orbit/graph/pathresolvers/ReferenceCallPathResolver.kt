package org.orbit.graph.pathresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.ReferenceCallNode
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.annotate
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object ReferenceCallPathResolver : PathResolver<ReferenceCallNode>, KoinComponent {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: ReferenceCallNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
        input.referenceNode.annotate(input.getGraphID(), Annotations.GraphID)
        val reference = pathResolverUtil.resolve(input.referenceNode, pass, environment, graph)

        input.parameterNodes.forEach {
            it.annotate(input.getGraphID(), Annotations.GraphID)
            pathResolverUtil.resolve(it, pass, environment, graph)
        }

        return reference
    }
}