package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.getPath
import org.orbit.core.nodes.TypeDefNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.annotate
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

class TypeDefPathResolver(
	private val parentPath: Path
) : PathResolver<TypeDefNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: TypeDefNode, pass: PathResolver.Pass, environment: Environment, graph: Graph) : PathResolver.Result {
		val path = if (pass == PathResolver.Pass.Initial) {
			val path = parentPath + Path(input.typeIdentifierNode.value)

			input.annotate(path, Annotations.Path)

			val kind = when (input.isRequired) {
				true -> Binding.Kind.RequiredType
				false -> Binding.Kind.Type
			}

			environment.bind(kind, input.typeIdentifierNode.value, path)

			val parentGraphID = graph.find(parentPath.toString(OrbitMangler))
			val graphID = graph.insert(input.typeIdentifierNode.value)

			graph.link(parentGraphID, graphID)

			input.annotate(graphID, Annotations.GraphID)
			input.propertyPairs.forEach {
				it.annotate(graphID, Annotations.GraphID)
				it.typeExpressionNode.annotate(graphID, Annotations.GraphID)
			}

			input.traitConformances.forEach {
				it.annotate(graphID, Annotations.GraphID)
			}

			path
		} else {
			val path = input.getPath()

			pathResolverUtil.resolveAll(input.traitConformances, pass, environment, graph)
			pathResolverUtil.resolveAll(input.propertyPairs, pass, environment, graph)

			path
		}

		return PathResolver.Result.Success(path)
	}
}

