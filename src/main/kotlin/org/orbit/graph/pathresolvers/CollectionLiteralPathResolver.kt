package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.CollectionLiteralNode
import org.orbit.frontend.extensions.annotate
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object CollectionLiteralPathResolver : IPathResolver<CollectionLiteralNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: CollectionLiteralNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        input.elements.forEach {
            it.annotate(input.getGraphID(), Annotations.graphId)
            pathResolverUtil.resolve(it, pass, environment, graph)
        }

        return IPathResolver.Result.Success(Path.array)
    }
}