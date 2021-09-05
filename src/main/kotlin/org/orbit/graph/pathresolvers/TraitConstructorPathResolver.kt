package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.nodes.TraitConstructorNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.annotate
import org.orbit.util.Invocation
import org.orbit.util.partial
import org.orbit.util.unaryMinus

class TraitConstructorPathResolver(
	private val parentPath: Path
) : PathResolver<TraitConstructorNode> {
	override val invocation: Invocation by inject()

	override fun resolve(input: TraitConstructorNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
		val path = parentPath + Path(input.typeIdentifierNode.value)

		if (pass == PathResolver.Pass.Initial) {
			input.annotate(path, Annotations.Path)
			input.typeIdentifierNode.annotate(path, Annotations.Path)

			environment.bind(Binding.Kind.TraitConstructor, input.typeIdentifierNode.value, path)

			val parentGraphID = graph.find(parentPath.toString(OrbitMangler))
			val graphID = graph.insert(input.typeIdentifierNode.value)

			graph.link(parentGraphID, graphID)
		} else {
			environment.withScope { scope ->
				input.typeParameterNodes.forEach { t ->
					environment.bind(Binding.Kind.Type, t.value, path + Path(t.value))
				}

				val methodSignaturePathResolver = MethodSignaturePathResolver()

				input.signatureNodes.forEach(-partial(methodSignaturePathResolver::resolve, pass, environment, graph))
			}
		}

		return PathResolver.Result.Success(path)
	}
}