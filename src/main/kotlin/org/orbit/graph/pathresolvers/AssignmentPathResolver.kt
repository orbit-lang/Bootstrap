package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.AssignmentStatementNode
import org.orbit.core.nodes.annotateByKey
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

class AssignmentPathResolver : IPathResolver<AssignmentStatementNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: AssignmentStatementNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
		val graphID = input.getGraphID()

		input.value.annotateByKey(graphID, Annotations.graphId)
		val valuePath = pathResolverUtil.resolve(input.value, pass, environment, graph)

		input.context?.let {
			it.annotateByKey(graphID, Annotations.graphId)
			pathResolverUtil.resolve(it, pass, environment, graph)
		}

		if (input.typeAnnotationNode != null) {
			val typeAnnotationPath = pathResolverUtil.resolve(input.typeAnnotationNode, pass, environment, graph)
				.asSuccess()

			input.typeAnnotationNode.annotateByKey(typeAnnotationPath.path, Annotations.path)
		}

		if (valuePath is IPathResolver.Result.Success) {
			input.annotateByKey(valuePath.path, Annotations.path)
		}

		return valuePath
	}
}