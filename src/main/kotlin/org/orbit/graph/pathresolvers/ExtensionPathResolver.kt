package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.nodes.ExtensionNode
import org.orbit.core.nodes.WhereClauseExpressionNode
import org.orbit.core.nodes.Annotations
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

interface WhereClauseExpressionResolver<N: WhereClauseExpressionNode> : PathResolver<N>

class ExtensionPathResolver(private val parentPath: Path) : PathResolver<ExtensionNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: ExtensionNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
        val targetTypePath = pathResolverUtil.resolve(input.targetTypeNode, pass, environment, graph)
            .asSuccess()

        val graphID = graph.find(targetTypePath.path.toString(OrbitMangler))

        input.annotate(graphID, Annotations.GraphID)
        input.annotate(targetTypePath.path, Annotations.Path)

        input.context?.let {
            it.annotate(graphID, Annotations.GraphID)
            pathResolverUtil.resolve(it, pass, environment, graph)
        }

        input.methodDefNodes.forEach {
            it.annotate(graphID, Annotations.GraphID)
            pathResolverUtil.resolve(it, pass, environment, graph)
        }

        return PathResolver.Result.Success(targetTypePath.path)
    }
}