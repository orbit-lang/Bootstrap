package org.orbit.graph.pathresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.WhereClauseByExpressionNode
import org.orbit.core.nodes.annotateByKey
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object WhereClauseByExpressionPathResolver : PathResolver<WhereClauseByExpressionNode>, KoinComponent {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: WhereClauseByExpressionNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
        input.lambdaExpression.annotateByKey(input.getGraphID(), Annotations.graphId)

        return pathResolverUtil.resolve(input.lambdaExpression, pass, environment, graph)
    }
}