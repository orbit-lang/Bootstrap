package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.PairNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.annotate
import org.orbit.util.Invocation

class PropertyPairPathResolver : PathResolver<PairNode> {
	override val invocation: Invocation by inject()

	override fun resolve(input: PairNode, pass: PathResolver.Pass, environment: Environment, graph: Graph) : PathResolver.Result {
		val typeNode = input.typeExpressionNode
		val propertyTypeBindingResult = environment.getBinding(typeNode.value, Binding.Kind.Union.entityMethodOrConstructor)
		val propertyTypeBinding = propertyTypeBindingResult
			.unwrap(this, input.typeExpressionNode.firstToken.position)
		val propertyTypePath = propertyTypeBinding.path

		input.annotate(propertyTypePath, Annotations.Path)
		input.typeExpressionNode.annotate(propertyTypePath, Annotations.Path)

		return PathResolver.Result.Success(propertyTypePath)
	}
}