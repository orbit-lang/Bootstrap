package org.orbit.graph.pathresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.TypeSynthesisNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.annotate
import org.orbit.graph.extensions.getGraphIDOrNull
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object TypeSynthesisPathResolver : PathResolver<TypeSynthesisNode>, KoinComponent {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(
        input: TypeSynthesisNode,
        pass: PathResolver.Pass,
        environment: Environment,
        graph: Graph
    ): PathResolver.Result {
        // TODO - Generalised compile-time functions
        val parentGraphID = input.getGraphIDOrNull()
        if (parentGraphID != null) {
            input.targetNode.annotate(parentGraphID, Annotations.GraphID)
        }

        return pathResolverUtil.resolve(input.targetNode, pass, environment, graph)
    }
}