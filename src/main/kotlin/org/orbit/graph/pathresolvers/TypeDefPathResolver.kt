package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.getPath
import org.orbit.core.nodes.TypeAliasNode
import org.orbit.core.nodes.TypeConstructorNode
import org.orbit.core.nodes.TypeDefNode
import org.orbit.graph.components.*
import org.orbit.graph.extensions.annotate
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

class TypeConstructorPathResolver(
	private val parentPath: Path
) : PathResolver<TypeConstructorNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(
		input: TypeConstructorNode,
		pass: PathResolver.Pass,
		environment: Environment,
		graph: Graph
	): PathResolver.Result {
		val path = parentPath + Path(input.typeIdentifierNode.value)

		if (pass == PathResolver.Pass.Initial) {
			input.annotate(path, Annotations.Path)
			input.typeIdentifierNode.annotate(path, Annotations.Path)

			environment.bind(Binding.Kind.TypeConstructor, input.typeIdentifierNode.value, path)

			val parentGraphID = graph.find(parentPath.toString(OrbitMangler))
			val graphID = graph.insert(input.typeIdentifierNode.value)

			graph.link(parentGraphID, graphID)
		} else {
			// TODO - Once we have complex type parameters, they need to be resolved
		}

		return PathResolver.Result.Success(path)
	}
}

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

class TypeAliasPathResolver(private val parentPath: Path) : PathResolver<TypeAliasNode> {
	override val invocation: Invocation by inject()

	override fun resolve(
		input: TypeAliasNode,
		pass: PathResolver.Pass,
		environment: Environment,
		graph: Graph
	): PathResolver.Result {
		val sourcePath = parentPath + Path(input.sourceTypeIdentifier.value)
		val targetBinding = environment.getBinding(input.targetTypeIdentifier.value, Binding.Kind.Type)
			.unwrap(this, input.targetTypeIdentifier.firstToken.position)

		val targetVertexID = graph.find(targetBinding)

		graph.alias(sourcePath.toString(OrbitMangler), targetVertexID)

		input.annotate(sourcePath, Annotations.Path)
		input.sourceTypeIdentifier.annotate(sourcePath, Annotations.Path)
		input.targetTypeIdentifier.annotate(targetBinding.path, Annotations.Path)

		environment.bind(Binding.Kind.TypeAlias, input.sourceTypeIdentifier.value, sourcePath)

		return PathResolver.Result.Success(sourcePath)
	}
}