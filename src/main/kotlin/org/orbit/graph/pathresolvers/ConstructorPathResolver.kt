package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.Scope
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.ConstructorNode
import org.orbit.graph.components.*
import org.orbit.graph.extensions.annotateByKey
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

class ConstructorPathResolver : PathResolver<ConstructorNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: ConstructorNode, pass: PathResolver.Pass, environment: Environment, graph: Graph) : PathResolver.Result {
		input.typeExpressionNode.annotateByKey(input.getGraphID(), Annotations.GraphID)
		TypeExpressionPathResolver.resolve(input.typeExpressionNode, pass, environment, graph)

		val binding = environment.getBinding(input.typeExpressionNode.value, Binding.Kind.Union.entityOrConstructor)

		return when (binding) {
			is Scope.BindingSearchResult.Success -> {
				input.typeExpressionNode.annotateByKey(binding.result.path, Annotations.Path)
				input.annotateByKey(binding.result.path, Annotations.Path)
				// Resolver parameters
				input.parameterNodes.forEach {
					it.annotateByKey(input.getGraphID(), Annotations.GraphID)
					pathResolverUtil.resolve(it, pass, environment, graph)
				}

                PathResolver.Result.Success(binding.result.path)
			}

			else -> {
				binding.unwrap(this, input.firstToken.position)
				PathResolver.Result.Failure(input)
			}
		}
	}
}