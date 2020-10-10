package org.orbit.graph

import org.orbit.core.nodes.PairNode
import org.orbit.util.Invocation

class PropertyPairPathResolver(
    override val invocation: Invocation,
    override val environment: Environment,
    override val graph: Graph
) : PathResolver<PairNode> {
	override fun resolve(input: PairNode, pass: PathResolver.Pass): PathResolver.Result {
		val typeNode = input.typeIdentifierNode
		val propertyTypeBindingResult = environment.getBinding(typeNode.value)
		val propertyTypeBinding = propertyTypeBindingResult
			.unwrap(this, input.typeIdentifierNode.firstToken.position)
		val propertyTypePath = propertyTypeBinding.path

		input.typeIdentifierNode.annotate(propertyTypePath, Annotations.Path)

		return PathResolver.Result.Success(propertyTypePath)
	}
}