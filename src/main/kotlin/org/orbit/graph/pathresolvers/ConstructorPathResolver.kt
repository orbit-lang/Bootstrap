package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.Scope
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.ConstructorInvocationNode
import org.orbit.core.nodes.annotateByKey
import org.orbit.frontend.extensions.annotate
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

class ConstructorPathResolver : IPathResolver<ConstructorInvocationNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: ConstructorInvocationNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph) : IPathResolver.Result {
		input.typeExpressionNode.annotate(input.getGraphID(), Annotations.graphId)
		TypeExpressionPathResolver.resolve(input.typeExpressionNode, pass, environment, graph)

		val binding = environment.getBinding(input.typeExpressionNode.value, Binding.Kind.Union.entity)

		return when (binding) {
			is Scope.BindingSearchResult.Success -> {
				input.typeExpressionNode.annotateByKey(binding.result.path, Annotations.path)
				input.annotateByKey(binding.result.path, Annotations.path)
				// Resolver parameters
				input.parameterNodes.forEach {
					it.annotateByKey(input.getGraphID(), Annotations.graphId)
					pathResolverUtil.resolve(it, pass, environment, graph)
				}

                IPathResolver.Result.Success(binding.result.path)
			}

			else -> {
				binding.unwrap(this, input.firstToken.position)
				IPathResolver.Result.Failure(input)
			}
		}
	}
}