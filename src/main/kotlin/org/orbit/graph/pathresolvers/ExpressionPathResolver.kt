package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.nodes.MethodCallNode
import org.orbit.core.nodes.ConstructorInvocationNode
import org.orbit.core.nodes.IExpressionNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

class ExpressionPathResolver : IPathResolver<IExpressionNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: IExpressionNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph) : IPathResolver.Result {
		return when (input) {
			is ConstructorInvocationNode -> pathResolverUtil.resolve(input, pass, environment, graph)
			is MethodCallNode -> TODO("HERE")
			is TypeIdentifierNode -> pathResolverUtil.resolve(input, pass, environment, graph)
			else -> IPathResolver.Result.Success(Path.empty)
		}
	}
}