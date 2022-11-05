package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.ExtensionNode
import org.orbit.core.nodes.WhereClauseExpressionNode
import org.orbit.core.nodes.annotateByKey
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

interface WhereClauseExpressionResolver<N: WhereClauseExpressionNode> : IPathResolver<N>

class ExtensionPathResolver(private val parentPath: Path) : IPathResolver<ExtensionNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: ExtensionNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        val targetTypePath = pathResolverUtil.resolve(input.targetTypeNode, pass, environment, graph)
            .asSuccess()

        val graphID = graph.find(targetTypePath.path.toString(OrbitMangler))

        input.annotateByKey(graphID, Annotations.graphId)
        input.annotateByKey(targetTypePath.path, Annotations.path)

        input.context?.let {
            it.annotateByKey(graphID, Annotations.graphId)
            pathResolverUtil.resolve(it, pass, environment, graph)
        }

        input.bodyNodes.forEach {
            it.annotateByKey(graphID, Annotations.graphId)
            pathResolverUtil.resolve(it, pass, environment, graph)
        }

        return IPathResolver.Result.Success(targetTypePath.path)
    }
}