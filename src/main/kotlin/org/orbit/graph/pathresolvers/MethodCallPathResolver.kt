package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.MethodCallNode
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.annotate
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

class MethodCallPathResolver : PathResolver<MethodCallNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: MethodCallNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
		input.receiverExpression.annotate(input.getGraphID(), Annotations.GraphID)
		val receiver = pathResolverUtil.resolve(input.receiverExpression, pass, environment, graph)

		input.parameterNodes.forEach {
			it.annotate(input.getGraphID(), Annotations.GraphID)
			pathResolverUtil.resolve(it, pass, environment, graph)
		}

		return receiver
	}
}