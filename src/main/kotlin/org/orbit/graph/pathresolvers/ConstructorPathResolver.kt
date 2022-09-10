package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.Scope
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.ConstructorInvocationNode
import org.orbit.core.nodes.annotateByKey
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

class ConstructorPathResolver : PathResolver<ConstructorInvocationNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: ConstructorInvocationNode, pass: PathResolver.Pass, environment: Environment, graph: Graph) : PathResolver.Result {
		input.typeExpressionNode.annotateByKey(input.getGraphID(), Annotations.graphId)
		TypeExpressionPathResolver.resolve(input.typeExpressionNode, pass, environment, graph)

		val binding = environment.getBinding(input.typeExpressionNode.value, Binding.Kind.Union.entityOrConstructor)

		return when (binding) {
			is Scope.BindingSearchResult.Success -> {
				input.typeExpressionNode.annotateByKey(binding.result.path, Annotations.path)
				input.annotateByKey(binding.result.path, Annotations.path)
				// Resolver parameters
				input.parameterNodes.forEach {
					it.annotateByKey(input.getGraphID(), Annotations.graphId)
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