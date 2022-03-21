
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
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation
import org.orbit.util.dispose
import org.orbit.util.partial
import org.orbit.util.unaryMinus

class TraitConstructorPathResolver(private val parentPath: Path) : PathResolver<TraitConstructorNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: TraitConstructorNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
		val path = parentPath + Path(input.typeIdentifierNode.value)

		if (pass == PathResolver.Pass.Initial) {
			input.annotate(path, Annotations.Path)
			input.typeIdentifierNode.annotate(path, Annotations.Path)

			environment.bind(Binding.Kind.TraitConstructor, input.typeIdentifierNode.value, path)

			val parentGraphID = graph.find(parentPath.toString(OrbitMangler))
			val graphID = graph.insert(input.typeIdentifierNode.value)

			graph.link(parentGraphID, graphID)

			input.annotate(graphID, Annotations.GraphID)

			for (typeParameter in input.typeParameterNodes) {
				val nPath = path + typeParameter.value

				typeParameter.annotate(nPath, Annotations.Path)
				typeParameter.annotate(graphID, Annotations.GraphID)

				val vertexID = graph.insert(typeParameter.value)

				graph.link(graphID, vertexID)

				//environment.bind(Binding.Kind.TypeParameter, typeParameter.value, nPath, vertexID)
			}
		} else {
			val parentGraphID = input.getGraphID()

			input.typeParameterNodes.forEach { t ->
				//environment.bind(Binding.Kind.TypeParameter, t.value, path + Path(t.value))
			}

			val methodSignaturePathResolver = MethodSignaturePathResolver()

			input.properties.forEach { it.typeExpressionNode.annotate(parentGraphID, Annotations.GraphID) }

			input.signatureNodes.forEach { it.annotate(parentGraphID, Annotations.GraphID) }
			input.signatureNodes.forEach(-partial(methodSignaturePathResolver::resolve, pass, environment, graph))

			input.properties.forEach(dispose(partial(pathResolverUtil::resolve, pass, environment, graph)))
			input.clauses.forEach(dispose(partial(TypeConstraintWhereClausePathResolver::resolve, pass, environment, graph)))
		}

		return PathResolver.Result.Success(path)
	}
}