package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.graph.components.*
import org.orbit.util.Invocation

class TypeIdentifierPathResolver(private val kind: Binding.Kind? = null) : IPathResolver<TypeIdentifierNode> {
	override val invocation: Invocation by inject()

	override fun resolve(input: TypeIdentifierNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
		return TypeExpressionPathResolver.execute(IPathResolver.InputType(input, pass))
	}
}