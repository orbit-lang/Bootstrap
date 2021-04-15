package org.orbit.graph

import org.orbit.core.Path
import org.orbit.core.nodes.*
import org.orbit.util.Invocation

class MethodDefPathResolver(
    override val invocation: Invocation,
    override val environment: Environment,
	override val graph: Graph
) : PathResolver<MethodDefNode> {
	override fun resolve(input: MethodDefNode, pass: PathResolver.Pass) : PathResolver.Result {
		val signatureResolver = MethodSignaturePathResolver(invocation, environment, graph)
		val signatureResult = signatureResolver.execute(PathResolver.InputType(input.signature, pass))

		signatureResult.withSuccess {
			input.annotate(it, Annotations.Path)
		}

		val blockResolver = BlockPathResolver(invocation, environment, graph)

		blockResolver.execute(PathResolver.InputType(input.body, PathResolver.Pass.Initial))

		return signatureResult
	}
}

class BlockPathResolver(
	override val invocation: Invocation,
	override val environment: Environment,
	override val graph: Graph
) : PathResolver<BlockNode> {
	override fun resolve(input: BlockNode, pass: PathResolver.Pass) : PathResolver.Result {
		val expressionResolver = ExpressionPathResolver(invocation, environment, graph)

		var result: PathResolver.Result = PathResolver.Result.Failure(input)
		for (node in input.body) {
			result = when (node) {
				is ReturnStatementNode -> expressionResolver.execute(PathResolver.InputType(node.valueNode.expressionNode, pass))
				else -> throw RuntimeException("TODO")
			}
		}

		return result
	}
}

class ExpressionPathResolver(
	override val invocation: Invocation,
	override val environment: Environment,
	override val graph: Graph
) : PathResolver<ExpressionNode> {
	override fun resolve(input: ExpressionNode, pass: PathResolver.Pass) : PathResolver.Result {
		return when (input) {
			is ConstructorNode -> ConstructorPathResolver(invocation, environment, graph).execute(input.toPathResolverInput())
			else -> PathResolver.Result.Success(Path())
		}
	}
}

class ConstructorPathResolver(
	override val invocation: Invocation,
	override val environment: Environment,
	override val graph: Graph
) : PathResolver<ConstructorNode> {
	override fun resolve(input: ConstructorNode, pass: PathResolver.Pass) : PathResolver.Result {
		val binding = environment.getBinding(input.typeIdentifierNode.value)

		return when (binding) {
			is Scope.BindingSearchResult.Success -> {
				input.typeIdentifierNode.annotate(binding.result.path, Annotations.Path)

				PathResolver.Result.Success(binding.result.path)
			}

			else -> throw RuntimeException("TODO")
		}
	}
}