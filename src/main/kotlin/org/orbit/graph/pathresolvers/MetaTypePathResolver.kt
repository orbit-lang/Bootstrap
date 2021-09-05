package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.MetaTypeNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.annotate
import org.orbit.util.Invocation

object MetaTypePathResolver : PathResolver<MetaTypeNode> {
	override val invocation: Invocation by inject()

	override fun resolve(input: MetaTypeNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
		val typeConstructorResult = TypeExpressionPathResolver.resolve(
            input.typeConstructorIdentifier,
            pass, environment, graph
        ).asSuccess()

		input.typeParameters.forEach { TypeExpressionPathResolver.resolve(it, pass, environment, graph) }

		input.annotate(typeConstructorResult.path, Annotations.Path)
		input.typeConstructorIdentifier.annotate(typeConstructorResult.path, Annotations.Path)

		return PathResolver.Result.Success(typeConstructorResult.path)
	}
}