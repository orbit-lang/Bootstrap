package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.nodes.MetaTypeNode
import org.orbit.core.nodes.TypeConstructorNode
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

// TODO - Unify this with TypeConstructorPathResolver; they do the same thing
class TypeConstructorPathResolver(
	private val parentPath: Path
) : PathResolver<TypeConstructorNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: TypeConstructorNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
		val path = parentPath + Path(input.typeIdentifierNode.value)

		if (pass == PathResolver.Pass.Initial) {
			input.annotate(path, Annotations.Path)
			input.typeIdentifierNode.annotate(path, Annotations.Path)

			environment.bind(Binding.Kind.TypeConstructor, input.typeIdentifierNode.value, path)

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

				environment.bind(Binding.Kind.TypeParameter, typeParameter.value, nPath, vertexID)
			}
		} else {
			val parentGraphID = input.getGraphID()

			input.traitConformance.forEach {
				it.annotate(parentGraphID, Annotations.GraphID, true)

				when (it) {
					is MetaTypeNode -> {
						it.typeConstructorIdentifier.annotate(parentGraphID, Annotations.GraphID, true)
						it.typeParameters.forEach { tp -> tp.annotate(parentGraphID, Annotations.GraphID, true) }
					}
				}
			}

			input.traitConformance.forEach(dispose(partial(pathResolverUtil::resolve, pass, environment, graph)))

			// This is a really disgusting hack to allow for multiple type parameters with the same name
			// TODO - Type Parameters should be uniquely mangled somehow
			input.properties.forEach { it.typeExpressionNode.annotate(parentGraphID, Annotations.GraphID) }
			input.properties.forEach(dispose(partial(pathResolverUtil::resolve, pass, environment, graph)))
			input.clauses.forEach(dispose(partial(TypeConstraintWhereClausePathResolver::resolve, pass, environment, graph)))
		}

		return PathResolver.Result.Success(path)
	}
}