package org.orbit.graph.pathresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.CompoundAttributeExpressionNode
import org.orbit.frontend.extensions.annotate
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object CompoundAttributeExpressionPathResolver : IPathResolver<CompoundAttributeExpressionNode>, KoinComponent {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: CompoundAttributeExpressionNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        input.leftExpression.annotate(input.getGraphID(), Annotations.graphId)
        input.rightExpression.annotate(input.getGraphID(), Annotations.graphId)

        pathResolverUtil.resolve(input.leftExpression, pass, environment, graph)
        pathResolverUtil.resolve(input.rightExpression, pass, environment, graph)

        return IPathResolver.Result.Success(Path.empty)
    }
}