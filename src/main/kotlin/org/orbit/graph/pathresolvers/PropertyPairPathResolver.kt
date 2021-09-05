package org.orbit.graph.pathresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.PairNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.annotate
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

class PropertyPairPathResolver : PathResolver<PairNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: PairNode, pass: PathResolver.Pass, environment: Environment, graph: Graph) : PathResolver.Result {
		val typeNode = input.typeExpressionNode
		val result = pathResolverUtil.resolve(typeNode, pass, environment, graph)
			.asSuccess()

		input.annotate(result.path, Annotations.Path)
		input.typeExpressionNode.annotate(result.path, Annotations.Path)

		return result
	}
}