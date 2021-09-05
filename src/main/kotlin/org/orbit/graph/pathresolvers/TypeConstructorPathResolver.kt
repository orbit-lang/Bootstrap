package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.nodes.TypeConstructorNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.annotate
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation
import org.orbit.util.dispose
import org.orbit.util.partial

class TypeConstructorPathResolver(
	private val parentPath: Path
) : PathResolver<TypeConstructorNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: TypeConstructorNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
		val path = parentPath + Path(input.typeIdentifierNode.value)

		if (pass == PathResolver.Pass.Initial) {
			//environment.withScope {
				//input.annotate(it.identifier, Annotations.Scope, true)
				input.annotate(path, Annotations.Path)
				input.typeIdentifierNode.annotate(path, Annotations.Path)

				environment.bind(Binding.Kind.TypeConstructor, input.typeIdentifierNode.value, path)

				val parentGraphID = graph.find(parentPath.toString(OrbitMangler))
				val graphID = graph.insert(input.typeIdentifierNode.value)

				graph.link(parentGraphID, graphID)

				for (typeParameter in input.typeParameterNodes) {
					val nPath = path + typeParameter.value

					typeParameter.annotate(nPath, Annotations.Path)
					// TODO - Recursively resolve nested type parameters
					environment.bind(Binding.Kind.Type, typeParameter.value, nPath)
				}
			//}
		} else {
			//environment.withScope(input) {
				// TODO - Once we have complex type parameters, they need to be resolved
				input.properties.forEach(dispose(partial(pathResolverUtil::resolve, pass, environment, graph)))
			//}
		}

		return PathResolver.Result.Success(path)
	}
}