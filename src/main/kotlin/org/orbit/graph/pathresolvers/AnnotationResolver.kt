package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.Node
import org.orbit.frontend.rules.PhaseAnnotationNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.annotate
import org.orbit.util.Invocation

class AnnotationResolver<N: Node>(private val annotationNode: PhaseAnnotationNode, clazz: Class<N>) : PathResolver<N> {
	override val invocation: Invocation by inject()

	override fun resolve(
        input: N,
        pass: PathResolver.Pass,
        environment: Environment,
        graph: Graph
	) : PathResolver.Result {
		// Phase annotations map back to real types
		val typeIdentifier = annotationNode.annotationIdentifierNode.value
		val binding = environment.getBinding(typeIdentifier, Binding.Kind.Type)
			.unwrap(this, input.firstToken.position)

		// TODO - Annotation parameters

		annotationNode.annotate(binding.path, Annotations.Path)

		return PathResolver.Result.Success(binding.path)
	}
}