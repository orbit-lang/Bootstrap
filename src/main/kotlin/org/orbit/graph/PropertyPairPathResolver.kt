package org.orbit.graph

import org.koin.core.component.inject
import org.orbit.core.nodes.PairNode
import org.orbit.util.Invocation

class PropertyPairPathResolver : PathResolver<PairNode> {
	override val invocation: Invocation by inject()

	override fun resolve(input: PairNode, pass: PathResolver.Pass, environment: Environment, graph: Graph) : PathResolver.Result {
		val typeNode = input.typeIdentifierNode
		val propertyTypeBindingResult = environment.getBinding(typeNode.value)
		val propertyTypeBinding = propertyTypeBindingResult
			.unwrap(this, input.typeIdentifierNode.firstToken.position)
		val propertyTypePath = propertyTypeBinding.path

		input.annotate(propertyTypePath, Annotations.Path)
		input.typeIdentifierNode.annotate(propertyTypePath, Annotations.Path)

		return PathResolver.Result.Success(propertyTypePath)
	}
}