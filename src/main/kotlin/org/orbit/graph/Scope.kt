package org.orbit.graph

import org.json.JSONObject
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.SourcePosition
import org.orbit.core.nodes.Node
import org.orbit.serial.Serial
import org.orbit.util.Fatal
import java.util.*

data class ScopeIdentifier(private val uuid: UUID) : Serial {
	companion object {
		fun next() : ScopeIdentifier {
			return ScopeIdentifier(UUID.randomUUID())
		}
	}

	override fun describe(json: JSONObject) {
		json.put("scope.identifier", uuid.toString())
	}
}

class Scope(
	private val environment: Environment,
    val parentScope: Scope? = null,
    val identifier: ScopeIdentifier = ScopeIdentifier.next(),
    val bindings: MutableList<Binding> = mutableListOf(),
	private val imports: MutableSet<ScopeIdentifier> = mutableSetOf()
) : Serial {
	sealed class BindingSearchResult {
		private data class BindingNotFound(
			override val phaseClazz: Class<out PathResolver<*>>,
			override val sourcePosition: SourcePosition,
			private val simpleName: String
		) : Fatal<PathResolver<*>> {
			override val message: String
				get() = "Unknown binding: $simpleName"
		}

		private data class MultipleBindings(
			override val phaseClazz: Class<out PathResolver<*>>,
			override val sourcePosition: SourcePosition,
			private val candidates: List<Binding>
		) : Fatal<PathResolver<*>> {
			override val message: String
				get() = "Multiple candidates found for binding:\n\t" + candidates.joinToString("\n\t")
		}

		abstract operator fun plus(other: BindingSearchResult) : BindingSearchResult
		abstract fun unwrap(phase: PathResolver<*>, sourcePosition: SourcePosition) : Binding

		data class Success(val result: Binding) : BindingSearchResult() {
			override fun plus(other: BindingSearchResult): BindingSearchResult = when (other) {
				is Success -> Multiple(listOf(result, other.result))
				is None -> this
				is Multiple -> Multiple(other.results + result)
			}

			override fun unwrap(phase: PathResolver<*>, sourcePosition: SourcePosition): Binding {
				return result
			}
		}

		data class None(val simpleName: String) : BindingSearchResult() {
			override fun plus(other: BindingSearchResult): BindingSearchResult = when (other) {
				is None -> this
				is Success -> other
				is Multiple -> other
			}

			override fun unwrap(phase: PathResolver<*>, sourcePosition: SourcePosition): Binding {
				phase.invocation.reportError(BindingNotFound(phase::class.java, sourcePosition, simpleName))
				throw Exception("Unreachable")
			}
		}

		data class Multiple(val results: List<Binding>) : BindingSearchResult() {
			override fun plus(other: BindingSearchResult): BindingSearchResult = when (other) {
				is Multiple -> Multiple(results + other.results)
				is Success -> Multiple(results + other.result)
				is None -> this
			}

			override fun unwrap(phase: PathResolver<*>, sourcePosition: SourcePosition): Binding {
				phase.invocation.reportError(MultipleBindings(phase::class.java, sourcePosition, results))
				throw Exception("Unreachable")
			}
		}
	}

	val size: Int get() = bindings.size

	fun inject(other: Scope) {
		bindings += other.bindings
	}

	fun import(scopeIdentifier: ScopeIdentifier) {
		imports.add(scopeIdentifier)
	}

	fun importAll(scopeIdentifiers: List<ScopeIdentifier>) {
		imports.addAll(scopeIdentifiers)
	}

	fun bind(kind: Binding.Kind, simpleName: String, path: Path) : Binding {
		val binding = Binding(kind, simpleName, path)
		if (!bindings.contains(binding)) bindings.add(binding)

		return binding
	}

	fun get(simpleName: String, context: Binding.Kind?) : BindingSearchResult {
//		if (simpleName == "Self") {
//			val result = Binding(Binding.Kind.Self, "Self", Path("Self"))
//
//			return BindingSearchResult.Success(result)
//		}

		val candidates = bindings.filter { it.simpleName == simpleName || it.path.toString(OrbitMangler) == simpleName }
			.toMutableList()

		var result = when (candidates.size) {
			0 -> BindingSearchResult.None(simpleName)
			1 -> BindingSearchResult.Success(candidates[0])
			else -> BindingSearchResult.Multiple(candidates)
		}

		result = imports.fold(result) { acc, next ->
			val scope = environment.getScope(next)
			acc + scope.get(simpleName, context)
		}

		result += parentScope?.get(simpleName, context) ?: BindingSearchResult.None(simpleName)

		return when (candidates.size) {
			0, 1 -> result
			else -> {
				if (context == null) {
					return result
				}

				val refined = candidates.filter { it.kind == context }

				when (refined.size) {
					0 -> BindingSearchResult.None(simpleName)
					1 -> BindingSearchResult.Success(refined[0])
					else -> result
				}
			}
		}
	}

	fun get(path: Path, context: Binding.Kind? = null) : BindingSearchResult {
		val candidates = bindings.filter { it.path == path }

		return when (candidates.size) {
			0 -> BindingSearchResult.None(path.toString(OrbitMangler))
			1 -> BindingSearchResult.Success(candidates[0])
			else -> {
				if (context == null) {
					return BindingSearchResult.Multiple(candidates)
				}

				val refined = candidates.filter { it.kind == context }

				when (refined.size) {
					0 -> BindingSearchResult.None(path.toString(OrbitMangler))
					1 -> BindingSearchResult.Success(refined[0])
					else -> BindingSearchResult.Multiple(refined)
				}
			}
		}
	}

	override fun toString(): String {
		return "SCOPE($identifier) <- PARENT(${parentScope?.identifier})\n" + bindings.joinToString("\n") { "\t" + it.toString() }
	}

	override fun describe(json: JSONObject) {

	}
}

fun Node.getScopeIdentifier() : ScopeIdentifier {
	return getAnnotation<ScopeIdentifier>(Annotations.Scope)!!.value
}

fun Node.getScopeIdentifierOrNull() : ScopeIdentifier? {
	return getAnnotation<ScopeIdentifier>(Annotations.Scope)?.value
}