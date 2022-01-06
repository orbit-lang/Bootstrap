package org.orbit.graph.pathresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.CollectionLiteralNode
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.types.components.IntrinsicTypes
import org.orbit.util.Invocation

object CollectionLiteralPathResolver : PathResolver<CollectionLiteralNode>, KoinComponent {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: CollectionLiteralNode, pass: PathResolver.Pass, environment: Environment, graph: Graph) : PathResolver.Result {
        input.elements.forEach { pathResolverUtil.resolve(it, pass, environment, graph) }

        return PathResolver.Result.Success(IntrinsicTypes.Array.path)
    }
}