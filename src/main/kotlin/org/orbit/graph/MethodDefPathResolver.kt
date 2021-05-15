package org.orbit.graph

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.nodes.*
import org.orbit.frontend.PrefixPhaseAnnotatedParseRule
import org.orbit.frontend.rules.PhaseAnnotationNode
import org.orbit.types.Entity
import org.orbit.types.IntrinsicTypes
import org.orbit.util.Invocation
import org.orbit.util.partial

class MethodDefPathResolver : PathResolver<MethodDefNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: MethodDefNode, pass: PathResolver.Pass, environment: Environment, graph: Graph) : PathResolver.Result {
		val signatureResult = pathResolverUtil.resolve(input.signature, pass, environment, graph)

		signatureResult.withSuccess {
			input.annotate(it, Annotations.Path)
		}

		pathResolverUtil.resolve(input.body, PathResolver.Pass.Initial, environment, graph)

		return signatureResult
	}
}

class BlockPathResolver : PathResolver<BlockNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: BlockNode, pass: PathResolver.Pass, environment: Environment, graph: Graph) : PathResolver.Result {
		return environment.withNewScope(input) {
			// TODO - Non-linear routes through a block, e.g. conditionals, controls etc
			var result: PathResolver.Result =
				PathResolver.Result.Success(OrbitMangler.unmangle(IntrinsicTypes.Unit.type.name))
			for (node in input.body) {
				when (node) {
					is PrintNode ->
						pathResolverUtil.resolve(node, pass, environment, graph)

					is ReturnStatementNode -> result =
						pathResolverUtil.resolve(node.valueNode.expressionNode, pass, environment, graph)

					is AssignmentStatementNode -> pathResolverUtil.resolve(node, pass, environment, graph)

					else -> throw invocation.make<CanonicalNameResolver>("Unsupported statement in block", node)
				}
			}

			result
		}
	}
}

class PrintPathResolver : PathResolver<PrintNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(
		input: PrintNode,
		pass: PathResolver.Pass,
		environment: Environment,
		graph: Graph
	): PathResolver.Result {
		return pathResolverUtil.resolve(input.expressionNode, pass, environment, graph)
	}
}

class AssignmentPathResolver : PathResolver<AssignmentStatementNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: AssignmentStatementNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
		val valuePath = pathResolverUtil.resolve(input.value, pass, environment, graph)

		if (valuePath is PathResolver.Result.Success) {
			input.annotate(valuePath.path, Annotations.Path)
		}

		return valuePath
	}
}

class CallPathResolver : PathResolver<CallNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(
		input: CallNode,
		pass: PathResolver.Pass,
		environment: Environment,
		graph: Graph
	): PathResolver.Result {
		return pathResolverUtil.resolve(input.receiverExpression, pass, environment, graph)
	}
}

class ExpressionPathResolver : PathResolver<ExpressionNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: ExpressionNode, pass: PathResolver.Pass, environment: Environment, graph: Graph) : PathResolver.Result {
		return when (input) {
			is ConstructorNode -> pathResolverUtil.resolve(input, pass, environment, graph)
			is CallNode -> TODO("HERE")
			is TypeIdentifierNode -> pathResolverUtil.resolve(input, pass, environment, graph)
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
		return pathResolverUtil.resolve(input.expressionNode, pass, environment, graph)
	}
}

abstract class LiteralPathResolver<N: LiteralNode<*>>(private val path: Path) : PathResolver<N> {
	override val invocation: Invocation by inject()

	override fun resolve(
		input: N,
		pass: PathResolver.Pass,
		environment: Environment,
		graph: Graph
	): PathResolver.Result {
		input.annotate(path, Annotations.Path)

		return PathResolver.Result.Success(path)
	}
}

object IntLiteralPathResolver : LiteralPathResolver<IntLiteralNode>(IntrinsicTypes.Int.path)
object SymbolLiteralPathResolver : LiteralPathResolver<SymbolLiteralNode>(IntrinsicTypes.Symbol.path)

class IdentifierExpressionPathResolver : PathResolver<IdentifierNode> {
	override val invocation: Invocation by inject()

	override fun resolve(
		input: IdentifierNode,
		pass: PathResolver.Pass,
		environment: Environment,
		graph: Graph
	): PathResolver.Result {
		return PathResolver.Result.Success(Path.empty)
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
				input.parameterNodes.forEach { pathResolverUtil.resolve(it, pass, environment, graph) }

				PathResolver.Result.Success(binding.result.path)
			}

			else -> TODO("@MethodDefPathResolver:95")
		}
	}
}

class UnaryExpressionResolver : PathResolver<UnaryExpressionNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(
		input: UnaryExpressionNode,
		pass: PathResolver.Pass,
		environment: Environment,
		graph: Graph
	): PathResolver.Result {
		return pathResolverUtil.resolve(input.operand, pass, environment, graph)
	}
}

class BinaryExpressionResolver : PathResolver<BinaryExpressionNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(
		input: BinaryExpressionNode,
		pass: PathResolver.Pass,
		environment: Environment,
		graph: Graph
	): PathResolver.Result {
		runBlocking {
			launch {
				pathResolverUtil.resolve(input.left, pass, environment, graph)
			}

			launch {
				pathResolverUtil.resolve(input.right, pass, environment, graph)
			}
		}

		// There is no path associated with runtime values
		// TODO - If this is an expression on types, there may be a path result here
		return PathResolver.Result.Success(Path.empty)
	}
}

class AnnotationResolver<N: Node>(private val annotationNode: PhaseAnnotationNode, clazz: Class<N>) : PathResolver<N> {
	override val invocation: Invocation by inject()

	override fun resolve(
		input: N,
		pass: PathResolver.Pass,
		environment: Environment,
		graph: Graph
	) : PathResolver.Result {
		// Phase annotations map back to real types
		val typeIdentifier = annotationNode.annotationIdentifierNode.value
		val binding = environment.getBinding(typeIdentifier, Binding.Kind.Type)
			.unwrap(this, input.firstToken.position)

		// TODO - Annotation parameters

		annotationNode.annotate(binding.path, Annotations.Path)

		return PathResolver.Result.Success(binding.path)
	}
}

class PathResolverUtil : KoinComponent {
	private val invocation: Invocation by inject()
	private val pathResolvers = mutableMapOf<Class<out Node>, PathResolver<*>>()

	fun <N: Node> registerPathResolver(pathResolver: PathResolver<N>, nodeType: Class<N>) {
		pathResolvers[nodeType] = pathResolver
	}

	fun <N: Node> resolve(node: N, pass: PathResolver.Pass, environment: Environment, graph: Graph) : PathResolver.Result {
		val resolver = pathResolvers[node::class.java] as? PathResolver<N>
			?: throw invocation.make<CanonicalNameResolver>("Cannot resolve path for Node ${node::class.java}", node)

		if (node is AnnotatedNode && pass == node.annotationPass) {
			node.phaseAnnotationNodes.forEach { AnnotationResolver(it, Node::class.java).resolve(it, pass, environment, graph) }
		}

		return resolver.execute(PathResolver.InputType(node, pass))
	}

	fun <N: Node> resolveAll(nodes: List<N>, pass: PathResolver.Pass, environment: Environment, graph: Graph) : List<PathResolver.Result> {
		return nodes.map(partial(::resolve, pass, environment, graph))
	}
}