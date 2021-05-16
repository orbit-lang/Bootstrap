package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.ConstructorNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.components.Scope
import org.orbit.graph.extensions.annotate
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

class ConstructorPathResolver : PathResolver<ConstructorNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: ConstructorNode, pass: PathResolver.Pass, environment: Environment, graph: Graph) : PathResolver.Result {
		val binding = environment.getBinding(input.typeIdentifierNode.value)

		return when (binding) {
			is Scope.BindingSearchResult.Success -> {
				input.typeIdentifierNode.annotate(binding.result.path, Annotations.Path)

				// Resolver parameters
				input.parameterNodes.forEach { pathResolverUtil.resolve(it, pass, environment, graph) }

                PathResolver.Result.Success(binding.result.path)
			}

			else -> TODO("@MethodDefPathResolver:95")
		}
	}
}