package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.PrintNode
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

class PrintPathResolver : PathResolver<PrintNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(
        input: PrintNode,
        pass: PathResolver.Pass,
        environment: Environment,
        graph: Graph
	): PathResolver.Result {
		return pathResolverUtil.resolve(input.expressionNode, pass, environment, graph)
	}
}