package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.TypeAliasNode
import org.orbit.core.nodes.annotateByKey
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

class TypeAliasPathResolver(private val parentPath: Path) : PathResolver<TypeAliasNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: TypeAliasNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
		val sourcePath = parentPath + Path(input.sourceTypeIdentifier.value)
		val graphID = graph.insert(sourcePath.toString(OrbitMangler))

		input.annotateByKey(graphID, Annotations.GraphID)
		input.targetTypeIdentifier.annotateByKey(graphID, Annotations.GraphID)

		TypeExpressionPathResolver.execute(PathResolver.InputType(input.targetTypeIdentifier, pass))

		val targetBinding = pathResolverUtil.resolve(input.targetTypeIdentifier, pass, environment, graph)
			.asSuccess()

		input.annotateByKey(sourcePath, Annotations.Path)
		input.sourceTypeIdentifier.annotateByKey(sourcePath, Annotations.Path)
		input.targetTypeIdentifier.annotateByKey(targetBinding.path, Annotations.Path)

		environment.bind(Binding.Kind.TypeAlias, input.sourceTypeIdentifier.value, sourcePath)

		return PathResolver.Result.Success(sourcePath)
	}
}