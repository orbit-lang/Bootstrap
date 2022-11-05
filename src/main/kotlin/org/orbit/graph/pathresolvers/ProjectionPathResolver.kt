package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.ProjectionNode
import org.orbit.core.nodes.annotateByKey
import org.orbit.frontend.extensions.annotate
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object ProjectionPathResolver : IPathResolver<ProjectionNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: ProjectionNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
		if (pass == IPathResolver.Pass.Initial) {
			val typeResult = TypeExpressionPathResolver.resolve(input.typeIdentifier, pass, environment, graph)
				.asSuccess()

			val graphID = graph.find(typeResult.path.toString(OrbitMangler))

			input.typeIdentifier.annotate(graphID, Annotations.graphId)
			input.traitIdentifier.annotate(graphID, Annotations.graphId)

			val traitResult = TypeExpressionPathResolver.resolve(input.traitIdentifier, pass, environment, graph)
				.asSuccess()

			input.typeIdentifier.annotate(typeResult.path, Annotations.path)
			input.traitIdentifier.annotate(traitResult.path, Annotations.path)

			input.annotate(typeResult.path, Annotations.path)

			if (input.context != null) {
				input.context.annotate(graphID, Annotations.graphId)
				pathResolverUtil.resolve(input.context, IPathResolver.Pass.Initial, environment, graph)
			}

			for (decl in input.body) {
				decl.annotate(graphID, Annotations.graphId)
				pathResolverUtil.resolve(decl, IPathResolver.Pass.Initial, environment, graph)
			}

			return typeResult
		}

		return IPathResolver.Result.Success(input.getPath())
	}
}