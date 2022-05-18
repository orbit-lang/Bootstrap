package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.*
import org.orbit.core.nodes.Annotations
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphIDOrNull
import org.orbit.graph.phase.CanonicalNameResolver
import org.orbit.util.Invocation

object TypeExpressionPathResolver : PathResolver<TypeExpressionNode> {
	override val invocation: Invocation by inject()

	override fun resolve(input: TypeExpressionNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result = when (input) {
		is TypeIdentifierNode -> {
			val binding = environment.getBinding(input.value, Binding.Kind.Union.entityOrConstructorOrParameter, graph, input.getGraphIDOrNull())
				.unwrap(this, input.firstToken.position)

			input.annotate(binding.path, Annotations.Path)

            PathResolver.Result.Success(binding.path)
		}

		is MetaTypeNode ->
			MetaTypePathResolver.resolve(input, pass, environment, graph)

		is TypeSynthesisNode -> TypeSynthesisPathResolver.resolve(input, pass, environment, graph)

		is TypeIndexNode -> throw invocation.make<CanonicalNameResolver>("Self Index not allowed in this context", input)

		is ExpandNode -> ExpandPathResolver.resolve(input, pass, environment, graph)

		is MirrorNode -> MirrorPathResolver.resolve(input, pass, environment, graph)

		else -> TODO("???")
	}
}