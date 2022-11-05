package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.PairNode
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.annotateByKey
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

class PropertyPairPathResolver : IPathResolver<PairNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: PairNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph) : IPathResolver.Result {
		val typeNode = input.typeExpressionNode
		val result = pathResolverUtil.resolve(typeNode, pass, environment, graph)
			.asSuccess()

		input.annotateByKey(result.path, Annotations.path)
		input.typeExpressionNode.annotateByKey(result.path, Annotations.path)

		return result
	}
}