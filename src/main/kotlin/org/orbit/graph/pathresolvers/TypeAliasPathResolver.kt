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

class TypeAliasPathResolver(private val parentPath: Path) : IPathResolver<TypeAliasNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: TypeAliasNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
		val sourcePath = parentPath + Path(input.sourceTypeIdentifier.value)
		val graphID = graph.insert(sourcePath.toString(OrbitMangler))

		input.annotateByKey(graphID, Annotations.graphId)
		input.targetType.annotateByKey(graphID, Annotations.graphId)

		TypeExpressionPathResolver.execute(IPathResolver.InputType(input.targetType, pass))

		val targetBinding = pathResolverUtil.resolve(input.targetType, pass, environment, graph)
			.asSuccess()

		input.annotateByKey(sourcePath, Annotations.path)
		input.sourceTypeIdentifier.annotateByKey(sourcePath, Annotations.path)
		input.targetType.annotateByKey(targetBinding.path, Annotations.path)

		environment.bind(Binding.Kind.TypeAlias, input.sourceTypeIdentifier.value, sourcePath)

		return IPathResolver.Result.Success(sourcePath)
	}
}