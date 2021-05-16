package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.AssignmentStatementNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.annotate
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

class AssignmentPathResolver : PathResolver<AssignmentStatementNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: AssignmentStatementNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
		val valuePath = pathResolverUtil.resolve(input.value, pass, environment, graph)

		if (valuePath is PathResolver.Result.Success) {
			input.annotate(valuePath.path, Annotations.Path)
		}

		return valuePath
	}
}