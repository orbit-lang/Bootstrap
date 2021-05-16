package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.UnaryExpressionNode
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

class UnaryExpressionResolver : PathResolver<UnaryExpressionNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(
        input: UnaryExpressionNode,
        pass: PathResolver.Pass,
        environment: Environment,
        graph: Graph
	): PathResolver.Result {
		return pathResolverUtil.resolve(input.operand, pass, environment, graph)
	}
}