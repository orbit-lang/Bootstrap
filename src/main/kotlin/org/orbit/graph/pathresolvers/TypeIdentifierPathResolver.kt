package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.components.Scope
import org.orbit.graph.extensions.annotate
import org.orbit.util.Invocation

class TypeIdentifierPathResolver : PathResolver<TypeIdentifierNode> {
	override val invocation: Invocation by inject()

	override fun resolve(
        input: TypeIdentifierNode,
        pass: PathResolver.Pass,
        environment: Environment,
        graph: Graph
	): PathResolver.Result {
		val binding = environment.getBinding(input.value)

		return when (binding) {
			is Scope.BindingSearchResult.Success -> {
				input.annotate(binding.result.path, Annotations.Path)
                PathResolver.Result.Success(binding.result.path)
			}

			else -> PathResolver.Result.Failure(input)
		}
	}
}