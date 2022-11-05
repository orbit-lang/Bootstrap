package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.getPath
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.TraitDefNode
import org.orbit.core.nodes.annotateByKey
import org.orbit.graph.components.*
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

class TraitDefPathResolver(
    private val parentPath: Path
) : IPathResolver<TraitDefNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: TraitDefNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph) : IPathResolver.Result {
		val path = if (pass == IPathResolver.Pass.Initial) {
			val path = parentPath + Path(input.typeIdentifierNode.value)

			input.annotateByKey(path, Annotations.path)
			environment.bind(Binding.Kind.Trait, input.typeIdentifierNode.value, path)

			// TODO - Trait conformances
			val parentGraphID = graph.find(parentPath.toString(OrbitMangler))
			val graphID = graph.insert(input.typeIdentifierNode.value)

			// TODO - This is probably overkill but ensures we have accounted for
			// all canonical representations of this trait's name.
			graph.link(parentGraphID, graphID)
			graph.alias(path.toString(OrbitMangler), graphID)

			input.typeIdentifierNode.typeParametersNode.typeParameters.forEach {
				environment.bind(Binding.Kind.Ephemeral, it.name.value, path + Path(it.name.value))
			}

			input.properties.forEach {
				it.annotateByKey(graphID, Annotations.graphId)
				it.typeNode.annotateByKey(graphID, Annotations.graphId)
				it.defaultValue?.annotateByKey(graphID, Annotations.graphId)
			}

			input.signatures.forEach { it.annotateByKey(graphID, Annotations.graphId) }

			path
		} else {
			val path = input.getPath()

			pathResolverUtil.resolveAll(input.traitConformances, pass, environment, graph)
			pathResolverUtil.resolveAll(input.signatures, pass, environment, graph)
			pathResolverUtil.resolveAll(input.properties, pass, environment, graph)

			path
		}

		return IPathResolver.Result.Success(path)
	}
}