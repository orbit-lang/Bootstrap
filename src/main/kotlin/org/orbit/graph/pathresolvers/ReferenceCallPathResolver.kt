package org.orbit.graph.pathresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.ReferenceCallNode
import org.orbit.core.nodes.annotateByKey
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object ReferenceCallPathResolver : IPathResolver<ReferenceCallNode>, KoinComponent {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: ReferenceCallNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        input.referenceNode.annotateByKey(input.getGraphID(), Annotations.graphId)
        val reference = pathResolverUtil.resolve(input.referenceNode, pass, environment, graph)

        input.arguments.forEach {
            it.annotateByKey(input.getGraphID(), Annotations.graphId)
            pathResolverUtil.resolve(it, pass, environment, graph)
        }

        return reference
    }
}