package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.nodes.IdentifierNode
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.util.Invocation

class IdentifierExpressionPathResolver : IPathResolver<IdentifierNode> {
	override val invocation: Invocation by inject()

	override fun resolve(input: IdentifierNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
		return IPathResolver.Result.Success(Path.empty)
	}
}