package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.BinaryExpressionNode
import org.orbit.core.nodes.NodeAnnotationMap
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

class BinaryExpressionResolver : IPathResolver<BinaryExpressionNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()
	private val nodeAnnotationMap: NodeAnnotationMap by inject()

	override fun resolve(input: BinaryExpressionNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
		nodeAnnotationMap.annotate(input.left, input.getGraphID(), Annotations.graphId)
		nodeAnnotationMap.annotate(input.right, input.getGraphID(), Annotations.graphId)

        pathResolverUtil.resolve(input.left, pass, environment, graph)
        pathResolverUtil.resolve(input.right, pass, environment, graph)

		return IPathResolver.Result.Success(Path.empty)
	}
}