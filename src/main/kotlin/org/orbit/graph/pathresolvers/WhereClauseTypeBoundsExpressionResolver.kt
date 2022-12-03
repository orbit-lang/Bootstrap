package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.WhereClauseTypeBoundsExpressionNode
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.annotateByKey
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object WhereClauseTypeBoundsExpressionResolver : WhereClauseExpressionResolver<WhereClauseTypeBoundsExpressionNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: WhereClauseTypeBoundsExpressionNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        input.sourceTypeExpression.annotateByKey(input.getGraphID(), Annotations.graphId)
        input.targetTypeExpression.annotateByKey(input.getGraphID(), Annotations.graphId)

        val sourceTypePath = pathResolverUtil.resolve(input.sourceTypeExpression, pass, environment, graph)
        val targetTypePath = pathResolverUtil.resolve(input.targetTypeExpression, IPathResolver.Pass.Initial, environment, graph)

        input.sourceTypeExpression.annotateByKey(sourceTypePath.asSuccess().path, Annotations.path)
        input.targetTypeExpression.annotateByKey(targetTypePath.asSuccess().path, Annotations.path)

        return sourceTypePath
    }
}