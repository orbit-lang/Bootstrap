package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.backend.typesystem.intrinsics.OrbCoreTypes
import org.orbit.core.Path
import org.orbit.core.nodes.*
import org.orbit.core.nodes.Annotations
import org.orbit.frontend.extensions.annotate
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.extensions.getGraphIDOrNull
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.graph.phase.CanonicalNameResolver
import org.orbit.util.Invocation

object TupleTypePathResolver : IPathResolver<TupleTypeNode> {
	override val invocation: Invocation by inject()

	override fun resolve(input: TupleTypeNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
		TypeExpressionPathResolver.resolve(input.left, pass, environment, graph)
		TypeExpressionPathResolver.resolve(input.right, pass, environment, graph)

		return IPathResolver.Result.Success(Path.empty)
	}
}

object StructTypePathResolver : IPathResolver<StructTypeNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: StructTypeNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
		input.members.forEach { it.annotate(input.getGraphID(), Annotations.graphId) }
		pathResolverUtil.resolveAll(input.members, pass, environment, graph)

		return IPathResolver.Result.Success(OrbCoreTypes.tupleType.getPath())
	}
}

object TypeExpressionPathResolver : IPathResolver<TypeExpressionNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: TypeExpressionNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result = when (input) {
		is TypeIdentifierNode -> {
			val binding = environment.getBinding(input.value, Binding.Kind.Union.entity, graph, input.getGraphIDOrNull())
				.unwrap(this, input.firstToken.position)

			input.annotateByKey(binding.path, Annotations.path)

            IPathResolver.Result.Success(binding.path)
		}

		is MetaTypeNode -> MetaTypePathResolver.resolve(input, pass, environment, graph)
		is TypeIndexNode -> throw invocation.make<CanonicalNameResolver>("Self Index not allowed in this context", input)
		is ExpandNode -> ExpandPathResolver.resolve(input, pass, environment, graph)
		is MirrorNode -> MirrorPathResolver.resolve(input, pass, environment, graph)
		is InferNode -> IPathResolver.Result.Success(Path("_"))
		is TupleTypeNode -> {
			resolve(input.left, pass, environment, graph)
			resolve(input.right, pass, environment, graph)

			IPathResolver.Result.Success(OrbCoreTypes.tupleType.getPath())
		}

		is StructTypeNode -> {
			input.members.forEach { it.annotate(input.getGraphID(), Annotations.graphId) }
			pathResolverUtil.resolveAll(input.members, pass, environment, graph)

			IPathResolver.Result.Success(OrbCoreTypes.tupleType.getPath())
		}

		is CollectionTypeNode -> {
			pathResolverUtil.resolve(input, pass, environment, graph)
		}

		is LambdaTypeNode -> {
			input.domain.forEach { it.annotate(input.getGraphID(), Annotations.graphId) }
			input.codomain.annotate(input.getGraphID(), Annotations.graphId)
			pathResolverUtil.resolve(input, pass, environment, graph)
		}

		is TypeLambdaNode -> {
			input.domain.forEach { it.annotate(input.getGraphID(), Annotations.graphId) }
			input.codomain.annotate(input.getGraphID(), Annotations.graphId)
			pathResolverUtil.resolve(input, pass, environment, graph)
		}

		is TypeLambdaInvocationNode -> {
			input.typeIdentifierNode.annotate(input.getGraphID(), Annotations.graphId)
			input.arguments.forEach { it.annotate(input.getGraphID(), Annotations.graphId) }

			pathResolverUtil.resolve(input, pass, environment, graph)
		}

		else -> TODO("Cannot resolve Path for unsupported Type Expression: $input")
	}
}