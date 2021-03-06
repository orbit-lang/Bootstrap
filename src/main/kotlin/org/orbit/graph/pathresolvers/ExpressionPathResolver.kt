package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.nodes.CallNode
import org.orbit.core.nodes.ConstructorNode
import org.orbit.core.nodes.ExpressionNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

class ExpressionPathResolver : PathResolver<ExpressionNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: ExpressionNode, pass: PathResolver.Pass, environment: Environment, graph: Graph) : PathResolver.Result {
		return when (input) {
			is ConstructorNode -> pathResolverUtil.resolve(input, pass, environment, graph)
			is CallNode -> TODO("HERE")
			is TypeIdentifierNode -> pathResolverUtil.resolve(input, pass, environment, graph)
			else -> PathResolver.Result.Success(Path.empty)
		}
	}
}