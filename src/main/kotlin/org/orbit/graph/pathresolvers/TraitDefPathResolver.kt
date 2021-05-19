package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.nodes.LValueTypeParameter
import org.orbit.core.nodes.TraitDefNode
import org.orbit.graph.components.*
import org.orbit.graph.extensions.annotate
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

class TraitDefPathResolver(
    private val parentPath: Path
) : PathResolver<TraitDefNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: TraitDefNode, pass: PathResolver.Pass, environment: Environment, graph: Graph) : PathResolver.Result {
		val path = parentPath + Path(input.typeIdentifierNode.value)

		input.annotate(path, Annotations.Path)
		environment.bind(Binding.Kind.Trait, input.typeIdentifierNode.value, path)

		// TODO - Trait conformances
		val parentGraphID = graph.find(parentPath.toString(OrbitMangler))
		val graphID = graph.insert(input.typeIdentifierNode.value)

		// TODO - This is probably overkill but ensures we have accounted for
		// all canonical representations of this trait's name.
		graph.link(parentGraphID, graphID)
		graph.alias(path.toString(OrbitMangler), graphID)

		environment.openScope(input)

		input.typeIdentifierNode.typeParametersNode.typeParameters.forEach {
			if (it is LValueTypeParameter) {
				environment.bind(Binding.Kind.Ephemeral, it.name.value, path + Path(it.name.value))
			}
		}

		pathResolverUtil.resolveAll(input.propertyPairs, pass, environment, graph)
		pathResolverUtil.resolveAll(input.signatures, pass, environment, graph)

		environment.closeScope()

		return PathResolver.Result.Success(path)
	}
}