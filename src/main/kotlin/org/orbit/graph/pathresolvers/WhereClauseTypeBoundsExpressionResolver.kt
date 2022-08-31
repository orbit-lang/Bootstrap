package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.WhereClauseTypeBoundsExpressionNode
import org.orbit.core.nodes.Annotations
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.annotateByKey
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object WhereClauseTypeBoundsExpressionResolver : WhereClauseExpressionResolver<WhereClauseTypeBoundsExpressionNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: WhereClauseTypeBoundsExpressionNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
        input.sourceTypeExpression.annotateByKey(input.getGraphID(), Annotations.GraphID)
        input.targetTypeExpression.annotateByKey(input.getGraphID(), Annotations.GraphID)

        val sourceTypePath = pathResolverUtil.resolve(input.sourceTypeExpression, pass, environment, graph)
        val targetTypePath = pathResolverUtil.resolve(input.targetTypeExpression, pass, environment, graph)

        input.sourceTypeExpression.annotateByKey(sourceTypePath.asSuccess().path, Annotations.Path)
        input.targetTypeExpression.annotateByKey(targetTypePath.asSuccess().path, Annotations.Path)

        return sourceTypePath
    }
}