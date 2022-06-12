package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.RValueNode
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.annotate
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

class RValuePathResolver : PathResolver<RValueNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: RValueNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
		input.expressionNode.annotate(input.getGraphID(), Annotations.GraphID)
		return pathResolverUtil.resolve(input.expressionNode, pass, environment, graph)
	}
}