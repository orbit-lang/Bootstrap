package org.orbit.graph

import org.orbit.core.Path
import org.orbit.core.Phase
import org.orbit.core.nodes.Node

fun <N: Node> PathResolver<N>.resolveAll(nodes: List<N>, pass: PathResolver.Pass) {
	nodes.forEach { execute(PathResolver.InputType(it, pass)) }
}

interface PathResolver<N: Node> : Phase<PathResolver.InputType<N>, PathResolver.Result> {
	enum class Pass {
		First, Second, Last
	}

	class InputType<N: Node>(val node: N, val pass: Pass)

	sealed class Result {
		data class Success(val path: Path) : Result()
		data class Failure(val node: Node) : Result()

		val isSuccess: Boolean get() = this is Success
		val isFailure: Boolean get() = this is Failure

		fun withSuccess(fn: (Path) -> Unit) {
			(this as? Success)?.let {
				fn(path)
			}
		}

		inline fun <reified N: Node> withFailure(fn: (N) -> Unit) {
			(this as? Failure)?.let {
				fn(node as N)
			}
		}
	}

	val environment: Environment
	val graph: Graph

	fun resolve(input: N, pass: Pass) : PathResolver.Result

	override fun execute(input: InputType<N>): PathResolver.Result {
		environment.openScope(input.node)
		try {
			environment.mark(input.node)
			val result = resolve(input.node, input.pass)

			if (result.isFailure) {
				result.withFailure<Node> {
					when (input.pass) {
						Pass.Second -> invocation.reportError(GraphErrors.MissingDependency(input.node))
						else -> {}
					}
				}
			}

			return result
		} finally {
			environment.closeScope()
		}
	}
}