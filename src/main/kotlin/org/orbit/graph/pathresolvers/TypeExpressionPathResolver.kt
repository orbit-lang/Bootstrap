package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.CollectionTypeLiteralNode
import org.orbit.core.nodes.MetaTypeNode
import org.orbit.core.nodes.TypeExpressionNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.annotate
import org.orbit.graph.extensions.getGraphIDOrNull
import org.orbit.util.Invocation

object TypeExpressionPathResolver : PathResolver<TypeExpressionNode> {
	override val invocation: Invocation by inject()

	override fun resolve(input: TypeExpressionNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result = when (input) {
		is TypeIdentifierNode -> {
			val binding = environment.getBinding(input.value, Binding.Kind.Union.entityOrConstructorOrParameter, graph, input.getGraphIDOrNull())
				.unwrap(this, input.firstToken.position)

			input.annotate(binding.path, Annotations.Path)

            PathResolver.Result.Success(binding.path)
		}

		is MetaTypeNode ->
			MetaTypePathResolver.resolve(input, pass, environment, graph)

		//is CollectionTypeLiteralNode ->

		else -> TODO("???")
	}
}