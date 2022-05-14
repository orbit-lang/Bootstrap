package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.graph.components.*
import org.orbit.graph.extensions.annotate
import org.orbit.graph.phase.CanonicalNameResolver
import org.orbit.util.Invocation

class TypeIdentifierPathResolver(private val kind: Binding.Kind? = null) : PathResolver<TypeIdentifierNode> {
	override val invocation: Invocation by inject()

	override fun resolve(input: TypeIdentifierNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
		return TypeExpressionPathResolver.execute(PathResolver.InputType(input, pass))
	}
}