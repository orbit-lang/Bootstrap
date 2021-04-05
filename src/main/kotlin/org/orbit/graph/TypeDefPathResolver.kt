package org.orbit.graph

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.getPath
import org.orbit.core.nodes.TypeDefNode
import org.orbit.util.Invocation

class TypeDefPathResolver(
    override val invocation: Invocation,
    override val environment: Environment,
	override val graph: Graph,
    private val parentPath: Path
) : PathResolver<TypeDefNode> {
	override fun resolve(input: TypeDefNode, pass: PathResolver.Pass) : PathResolver.Result {
		val path: Path = if (pass == PathResolver.Pass.Initial) {
			val path = parentPath + Path(input.typeIdentifierNode.value)

			input.annotate(path, Annotations.Path)
			environment.bind(Binding.Kind.Type, input.typeIdentifierNode.value, path)

			val parentGraphID = graph.find(parentPath.toString(OrbitMangler))
			val graphID = graph.insert(input.typeIdentifierNode.value)

			graph.link(parentGraphID, graphID)
			path
		} else {
			val path = input.getPath()
			val propertyResolver = PropertyPairPathResolver(invocation, environment, graph)

			propertyResolver.resolveAll(input.propertyPairs, pass)
			path
		}

		return PathResolver.Result.Success(path)
	}
}