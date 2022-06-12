package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.BinaryExpressionNode
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

class BinaryExpressionResolver : PathResolver<BinaryExpressionNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: BinaryExpressionNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
        input.left.annotate(input.getGraphID(), Annotations.GraphID)
        input.right.annotate(input.getGraphID(), Annotations.GraphID)

        pathResolverUtil.resolve(input.left, pass, environment, graph)
        pathResolverUtil.resolve(input.right, pass, environment, graph)

		return PathResolver.Result.Success(Path.empty)
	}
}