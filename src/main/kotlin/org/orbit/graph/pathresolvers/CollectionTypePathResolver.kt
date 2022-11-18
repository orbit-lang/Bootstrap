package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.CollectionTypeNode
import org.orbit.frontend.extensions.annotate
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object CollectionTypePathResolver : IPathResolver<CollectionTypeNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: CollectionTypeNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        //input.elementType.annotate(input.getGraphID(), Annotations.graphId)

        return pathResolverUtil.resolve(input.elementType, pass, environment, graph)
    }
}