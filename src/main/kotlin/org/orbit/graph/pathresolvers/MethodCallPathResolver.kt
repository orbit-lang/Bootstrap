package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.EffectHandlerNode
import org.orbit.core.nodes.MethodCallNode
import org.orbit.core.nodes.annotateByKey
import org.orbit.frontend.extensions.annotate
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object EffectHandlerPathResolver : IPathResolver<EffectHandlerNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: EffectHandlerNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
		input.flowIdentifier.annotate(input.getGraphID(), Annotations.graphId)
		input.cases.forEach {
			it.annotate(input.getGraphID(), Annotations.graphId)
			pathResolverUtil.resolve(it, pass, environment, graph)
		}

		return IPathResolver.Result.Success(Path.empty)
	}
}

class MethodCallPathResolver : IPathResolver<MethodCallNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: MethodCallNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
		input.receiverExpression.annotate(input.getGraphID(), Annotations.graphId)
		val receiver = pathResolverUtil.resolve(input.receiverExpression, pass, environment, graph)

		input.arguments.forEach {
			it.annotateByKey(input.getGraphID(), Annotations.graphId)
			pathResolverUtil.resolve(it, pass, environment, graph)
		}

		input.effectHandler?.let {
			it.annotate(input.getGraphID(), Annotations.graphId)
			pathResolverUtil.resolve(it, pass, environment, graph)
		}

		return receiver
	}
}