package org.orbit.graph.pathresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.TypeOfNode
import org.orbit.core.nodes.annotateByKey
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object TypeOfPathResolver : IPathResolver<TypeOfNode>, KoinComponent {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: TypeOfNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        input.expressionNode.annotateByKey(input.getGraphID(), Annotations.graphId)
        return pathResolverUtil.resolve(input.expressionNode, pass, environment, graph)
    }
}