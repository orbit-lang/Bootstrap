package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.backend.typesystem.intrinsics.OrbCoreTypes
import org.orbit.core.Path
import org.orbit.core.nodes.*
import org.orbit.core.nodes.Annotations
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphIDOrNull
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.graph.phase.CanonicalNameResolver
import org.orbit.util.Invocation

object TupleTypePathResolver : PathResolver<TupleTypeNode> {
	override val invocation: Invocation by inject()

	override fun resolve(input: TupleTypeNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
		TypeExpressionPathResolver.resolve(input.left, pass, environment, graph)
		TypeExpressionPathResolver.resolve(input.right, pass, environment, graph)

		return PathResolver.Result.Success(Path.empty)
	}
}

object StructTypePathResolver : PathResolver<StructTypeNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: StructTypeNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
		pathResolverUtil.resolveAll(input.members, pass, environment, graph)

		return PathResolver.Result.Success(OrbCoreTypes.tupleType.getPath())
	}
}

object TypeExpressionPathResolver : PathResolver<TypeExpressionNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: TypeExpressionNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result = when (input) {
		is TypeIdentifierNode -> {
			val binding = environment.getBinding(input.value, Binding.Kind.Union.entity, graph, input.getGraphIDOrNull())
				.unwrap(this, input.firstToken.position)

			input.annotateByKey(binding.path, Annotations.path)

            PathResolver.Result.Success(binding.path)
		}

		is MetaTypeNode -> MetaTypePathResolver.resolve(input, pass, environment, graph)
		is TypeIndexNode -> throw invocation.make<CanonicalNameResolver>("Self Index not allowed in this context", input)
		is ExpandNode -> ExpandPathResolver.resolve(input, pass, environment, graph)
		is MirrorNode -> MirrorPathResolver.resolve(input, pass, environment, graph)
		is InferNode -> PathResolver.Result.Success(Path("_"))
		is TupleTypeNode -> {
			resolve(input.left, pass, environment, graph)
			resolve(input.right, pass, environment, graph)

			PathResolver.Result.Success(OrbCoreTypes.tupleType.getPath())
		}
		is StructTypeNode -> {
			pathResolverUtil.resolveAll(input.members, pass, environment, graph)

			PathResolver.Result.Success(OrbCoreTypes.tupleType.getPath())
		}

		else -> TODO("???")
	}
}