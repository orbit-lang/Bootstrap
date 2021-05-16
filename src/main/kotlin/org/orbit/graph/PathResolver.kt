package org.orbit.graph

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.Phase
import org.orbit.core.nodes.Node

fun <N: Node> PathResolver<N>.resolveAll(nodes: List<N>, pass: PathResolver.Pass) {
	nodes.forEach { execute(PathResolver.InputType(it, pass)) }
}

fun <N: Node> N.toPathResolverInput(pass: PathResolver.Pass = PathResolver.Pass.Initial) : PathResolver.InputType<N> {
	return PathResolver.InputType(this, pass)
}

interface PathResolver<N: Node> : Phase<PathResolver.InputType<N>, PathResolver.Result>, KoinComponent {
	sealed class Pass {
		object Initial : Pass()
		data class Subsequent(val index: Int) : Pass()
		object Last : Pass()
	}

	class InputType<N: Node>(val node: N, val pass: Pass)

	sealed class Result {
		data class Success(val path: Path) : Result()
		data class Failure(val node: Node) : Result()

		val isSuccess: Boolean get() = this is Success
		val isFailure: Boolean get() = this is Failure

		fun asSuccess() : Result.Success {
			return this as Success
		}

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

	fun resolve(input: N, pass: Pass, environment: Environment, graph: Graph) : Result

	override fun execute(input: InputType<N>): Result {
		val environment = inject<Environment>().value
		val graph = inject<Graph>().value

//		environment.openScope(input.node)

		try {
			environment.mark(input.node)

			val result = resolve(input.node, input.pass, environment, graph)

			if (result.isFailure) {
				result.withFailure<Node> {
					when (input.pass) {
						is Pass.Subsequent -> invocation.reportError(GraphErrors.MissingDependency(input.node))
						else -> {}
					}
				}
			}

			return result
		} finally {
//			environment.closeScope()
		}
	}
}