package org.orbit.graph

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.nodes.LValueTypeParameter
import org.orbit.core.nodes.TraitDefNode
import org.orbit.util.Invocation

class TraitDefPathResolver(
    override val invocation: Invocation,
    override val environment: Environment,
	override val graph: Graph,
    private val parentPath: Path
) : PathResolver<TraitDefNode> {
	override fun resolve(input: TraitDefNode, pass: PathResolver.Pass) : PathResolver.Result {
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

		environment.bind(Binding.Kind.Self, "Self", path)

		input.typeIdentifierNode.typeParametersNode.typeParameters.forEach {
			if (it is LValueTypeParameter) {
				environment.bind(Binding.Kind.Ephemeral, it.name.value, path + Path(it.name.value))
			}
		}

		val propertyResolver = PropertyPairPathResolver(invocation, environment, graph)

		propertyResolver.resolveAll(input.propertyPairs, pass)

		val signatureResolver = MethodSignaturePathResolver(invocation, environment, graph)

		signatureResolver.resolveAll(input.signatures, pass)

		environment.closeScope()

		return PathResolver.Result.Success(path)
	}
}