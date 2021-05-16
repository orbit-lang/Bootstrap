package org.orbit.graph.pathresolvers

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.nodes.BinaryExpressionNode
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

class BinaryExpressionResolver : PathResolver<BinaryExpressionNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(
        input: BinaryExpressionNode,
        pass: PathResolver.Pass,
        environment: Environment,
        graph: Graph
	): PathResolver.Result {
        runBlocking {
            launch {
                pathResolverUtil.resolve(input.left, pass, environment, graph)
            }

            launch {
                pathResolverUtil.resolve(input.right, pass, environment, graph)
            }
        }

		// There is no path associated with runtime values
		// TODO - If this is an expression on types, there may be a path result here
		return PathResolver.Result.Success(Path.empty)
	}
}