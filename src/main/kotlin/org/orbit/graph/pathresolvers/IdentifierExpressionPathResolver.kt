package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.nodes.IdentifierNode
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.util.Invocation

class IdentifierExpressionPathResolver : PathResolver<IdentifierNode> {
	override val invocation: Invocation by inject()

	override fun resolve(
        input: IdentifierNode,
        pass: PathResolver.Pass,
        environment: Environment,
        graph: Graph
	): PathResolver.Result {
		return PathResolver.Result.Success(Path.empty)
	}
}