package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.getPath
import org.orbit.core.nodes.TypeDefNode
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.annotateByKey
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
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

			input.annotateByKey(path, Annotations.Path)

			environment.bind(Binding.Kind.Type, input.typeIdentifierNode.value, path)

			val parentGraphID = graph.find(parentPath.toString(OrbitMangler))
			val graphID = graph.insert(input.typeIdentifierNode.value)

			graph.link(parentGraphID, graphID)

			input.annotateByKey(graphID, Annotations.GraphID)
			input.properties.forEach {
				it.annotateByKey(graphID, Annotations.GraphID)
				it.typeNode.annotateByKey(graphID, Annotations.GraphID)
				it.defaultValue?.annotateByKey(graphID, Annotations.GraphID)
			}

			input.traitConformances.forEach {
				it.annotateByKey(graphID, Annotations.GraphID)
			}

			path
		} else {
			val path = input.getPath()

			pathResolverUtil.resolveAll(input.traitConformances, pass, environment, graph)
			pathResolverUtil.resolveAll(input.properties, pass, environment, graph)

			path
		}

		return PathResolver.Result.Success(path)
	}
}

