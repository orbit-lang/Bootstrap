package org.orbit.graph

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.nodes.*
import org.orbit.types.IntrinsicTypes
import org.orbit.util.Invocation
import org.orbit.util.partial

class MethodDefPathResolver : PathResolver<MethodDefNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: MethodDefNode, pass: PathResolver.Pass, environment: Environment, graph: Graph) : PathResolver.Result {
		val signatureResult = pathResolverUtil.resolve(input.signature, pass)

		signatureResult.withSuccess {
			input.annotate(it, Annotations.Path)
		}

		pathResolverUtil.resolve(input.body, PathResolver.Pass.Initial)

		return signatureResult
	}
}

class BlockPathResolver : PathResolver<BlockNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: BlockNode, pass: PathResolver.Pass, environment: Environment, graph: Graph) : PathResolver.Result {
		return environment.withNewScope(input) {
			val expressionResolver = ExpressionPathResolver()

			// TODO - Non-linear routes through a block, e.g. conditionals, controls etc
			var result: PathResolver.Result =
				PathResolver.Result.Success(OrbitMangler.unmangle(IntrinsicTypes.Unit.type.name))
			for (node in input.body) {
				when (node) {
					is ReturnStatementNode -> result =
						pathResolverUtil.resolve(node.valueNode.expressionNode, pass)

					is AssignmentStatementNode -> pathResolverUtil.resolve(node, pass)

					else -> throw invocation.make<CanonicalNameResolver>("Unsupported statement in block", node)
				}
			}

			result
		}
	}
}

class AssignmentPathResolver : PathResolver<AssignmentStatementNode> {
	override val invocation: Invocation by inject()
	private val util: PathResolverUtil by inject()

	override fun resolve(input: AssignmentStatementNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
		val valuePath = util.resolve(input.value, pass)

		if (valuePath is PathResolver.Result.Success) {
			input.annotate(valuePath.path, Annotations.Path)
		}

		return valuePath
	}
}

class InstanceMethodCallPathResolver : PathResolver<InstanceMethodCallNode> {
	override val invocation: Invocation by inject()

	override fun resolve(
		input: InstanceMethodCallNode,
		pass: PathResolver.Pass,
		environment: Environment,
		graph: Graph
	): PathResolver.Result {
		return PathResolver.Result.Success(Path.empty)
	}
}

class ExpressionPathResolver : PathResolver<ExpressionNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: ExpressionNode, pass: PathResolver.Pass, environment: Environment, graph: Graph) : PathResolver.Result {
		return when (input) {
			is ConstructorNode -> pathResolverUtil.resolve(input, pass)
			is InstanceMethodCallNode -> TODO("HERE")
			is TypeIdentifierNode -> pathResolverUtil.resolve(input, pass)
			else -> PathResolver.Result.Success(Path.empty)
		}
	}
}

class RValuePathResolver : PathResolver<RValueNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(
		input: RValueNode,
		pass: PathResolver.Pass,
		environment: Environment,
		graph: Graph
	): PathResolver.Result {
		return pathResolverUtil.resolve(input.expressionNode, pass)
	}
}

class TypeIdentifierPathResolver : PathResolver<TypeIdentifierNode> {
	override val invocation: Invocation by inject()

	override fun resolve(
		input: TypeIdentifierNode,
		pass: PathResolver.Pass,
		environment: Environment,
		graph: Graph
	): PathResolver.Result {
		val binding = environment.getBinding(input.value)

		return when (binding) {
			is Scope.BindingSearchResult.Success -> {
				input.annotate(binding.result.path, Annotations.Path)
				PathResolver.Result.Success(binding.result.path)
			}

			else -> PathResolver.Result.Failure(input)
		}
	}
}

class ConstructorPathResolver : PathResolver<ConstructorNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: ConstructorNode, pass: PathResolver.Pass, environment: Environment, graph: Graph) : PathResolver.Result {
		val binding = environment.getBinding(input.typeIdentifierNode.value)

		return when (binding) {
			is Scope.BindingSearchResult.Success -> {
				input.typeIdentifierNode.annotate(binding.result.path, Annotations.Path)

				// Resolver parameters
				input.parameterNodes.forEach { pathResolverUtil.resolve(it, pass) }

				PathResolver.Result.Success(binding.result.path)
			}

			else -> TODO("@MethodDefPathResolver:95")
		}
	}
}

class PathResolverUtil : KoinComponent {
	private val invocation: Invocation by inject()
	private val pathResolvers = mutableMapOf<Class<out Node>, PathResolver<*>>()

	fun <N: Node> registerPathResolver(pathResolver: PathResolver<N>, nodeType: Class<N>) {
		pathResolvers[nodeType] = pathResolver
	}

	fun <N: Node> resolve(node: N, pass: PathResolver.Pass) : PathResolver.Result {
		val resolver = pathResolvers[node::class.java] as? PathResolver<N>
			?: throw invocation.make<CanonicalNameResolver>("Cannot resolve path for Node ${node::class.java}", node)

		return resolver.execute(PathResolver.InputType(node, pass))
	}

	fun <N: Node> resolveAll(nodes: List<N>, pass: PathResolver.Pass) : List<PathResolver.Result> {
		return nodes.map(partial(::resolve, pass))
	}
}