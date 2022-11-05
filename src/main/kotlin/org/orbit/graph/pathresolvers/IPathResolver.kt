package org.orbit.graph.pathresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.nodes.INode
import org.orbit.core.phase.Phase
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.phase.GraphErrors

interface IPathResolver<N: INode> : Phase<IPathResolver.InputType<N>, IPathResolver.Result>, KoinComponent {
	sealed class Pass {
		object Initial : Pass()
		data class Subsequent(val index: Int) : Pass()
		object Last : Pass()
	}

	class InputType<N: INode>(val node: N, val pass: Pass)

	sealed class Result {
		data class Success(val path: Path) : Result()
		data class Failure(val node: INode) : Result()

		val isSuccess: Boolean get() = this is Success
		val isFailure: Boolean get() = this is Failure

		fun asSuccess() : Success {
			return this as Success
		}

		fun asSuccessOrNull() : Success? = this as? Success

		fun withSuccess(fn: (Path) -> Unit) {
			(this as? Success)?.let {
				fn(path)
			}
		}

		inline fun <reified N: INode> withFailure(fn: (N) -> Unit) {
			(this as? Failure)?.let {
				fn(node as N)
			}
		}
	}

	fun resolve(input: N, pass: Pass, environment: Environment, graph: Graph) : Result

	override fun execute(input: InputType<N>): Result {
		val environment = inject<Environment>().value
		val graph = inject<Graph>().value

		val result = resolve(input.node, input.pass, environment, graph)

		if (result.isFailure) {
			result.withFailure<INode> {
				when (input.pass) {
					is Pass.Subsequent -> invocation.reportError(GraphErrors.MissingDependency(input.node))
					else -> {}
				}
			}
		}

		return result
	}
}