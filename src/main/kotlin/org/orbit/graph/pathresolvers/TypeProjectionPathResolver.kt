package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.TypeProjectionNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.annotate
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object TypeProjectionPathResolver : PathResolver<TypeProjectionNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: TypeProjectionNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
		val typeResult = TypeExpressionPathResolver.resolve(input.typeIdentifier, pass, environment, graph)
			.asSuccess()

		val traitResult = TypeExpressionPathResolver.resolve(input.traitIdentifier, pass, environment, graph)
			.asSuccess()

		input.typeIdentifier.annotate(typeResult.path, Annotations.Path)
		input.traitIdentifier.annotate(traitResult.path, Annotations.Path)

		input.annotate(typeResult.path, Annotations.Path)

		// TODO - Resolve where clauses
		input.whereNodes
			.forEach { pathResolverUtil.resolve(it.whereStatement, pass, environment, graph) }

		return typeResult
	}
}