package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.CauseNode
import org.orbit.frontend.extensions.annotate
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object CausePathResolver : IPathResolver<CauseNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: CauseNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        input.invocationNode.effectIdentifier.annotate(input.getGraphID(), Annotations.graphId)
        val result = pathResolverUtil.resolve(input.invocationNode.effectIdentifier, pass, environment, graph)

        input.invocationNode.args.forEach {
            it.annotate(input.getGraphID(), Annotations.graphId)
            pathResolverUtil.resolve(it, pass, environment, graph)
        }

        return result
    }
}