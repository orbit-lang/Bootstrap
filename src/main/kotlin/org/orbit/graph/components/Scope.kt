package org.orbit.graph.components

import org.json.JSONObject
import org.orbit.core.*
import org.orbit.core.components.SourcePosition
import org.orbit.core.components.CompilationEvent
import org.orbit.core.components.CompilationEventBusAware
import org.orbit.core.components.CompilationEventBusAwareImpl
import org.orbit.graph.pathresolvers.PathResolver
import org.orbit.serial.Serial
import org.orbit.util.Fatal
import org.orbit.util.Monoid

class Scope(
	private val environment: Environment,
	val parentScope: Scope? = null,
	val identifier: ScopeIdentifier = ScopeIdentifier.next(),
	val bindings: MutableList<Binding> = mutableListOf(),
	private val imports: MutableSet<ScopeIdentifier> = mutableSetOf()
) : Serial, CompilationEventBusAware by CompilationEventBusAwareImpl {
	sealed class Events(override val identifier: String) : CompilationEvent {
		class BindingCreated(binding: Binding) : Events("Scope Binding Created: $binding")
	}

	sealed class BindingSearchResult : Monoid<BindingSearchResult> {
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

		abstract fun unwrap(phase: PathResolver<*>, sourcePosition: SourcePosition) : Binding

		data class Success(val result: Binding) : BindingSearchResult() {
			override fun unwrap(phase: PathResolver<*>, sourcePosition: SourcePosition): Binding {
				return result
			}

			override fun BindingSearchResult.combine(other: BindingSearchResult): BindingSearchResult = when (other) {
				is None -> this
				is Success -> Multiple(listOf(result, other.result))
				is Multiple -> Multiple(other.results + result)
			}
		}

		class None(val simpleName: String) : BindingSearchResult() {
			override fun unwrap(phase: PathResolver<*>, sourcePosition: SourcePosition): Binding {
				// TODO - Is simpleName a hard requirement here?
				phase.invocation.reportError(BindingNotFound(phase::class.java, sourcePosition, simpleName))
				throw Exception("Unreachable")
			}

			override fun BindingSearchResult.combine(other: BindingSearchResult): BindingSearchResult = when (other) {
				is None -> this
				else -> other
			}
		}

		data class Multiple(val results: List<Binding>) : BindingSearchResult() {
			override fun unwrap(phase: PathResolver<*>, sourcePosition: SourcePosition): Binding {
				if (results.size == 1) {
					// NOTE - Hack to get around circular dependencies
					return results.first()
				}

				phase.invocation.reportError(MultipleBindings(phase::class.java, sourcePosition, results))
				throw Exception("Unreachable")
			}

			override fun BindingSearchResult.combine(other: BindingSearchResult): BindingSearchResult = when (other) {
				is None -> this
				is Success -> Multiple(results + other.result)
				is Multiple -> Multiple(other.results + results)
			}
		}

		override fun BindingSearchResult.zero(): BindingSearchResult {
			return None("")
		}

		operator fun plus(other: BindingSearchResult) : BindingSearchResult = combine(other)
	}

	val size: Int get() = bindings.size

	fun getImportedScopes() : List<ScopeIdentifier> = imports.toList()

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
		// TODO - This is gross, but it is useful to be able to tell which scope a path belongs to.
		// The alternative would be to search through all known scopes for a match, which is quite expensive
		path.enclosingScope = this
		val binding = Binding(kind, simpleName, path)
		if (!bindings.contains(binding)) {
			bindings.add(binding)
			compilationEventBus.notify(Events.BindingCreated(binding))
		}

		return binding
	}

	fun unbind(kind: Binding.Kind, simpleName: String, path: Path) {
		bindings.remove(Binding(kind, simpleName, path))
	}

	fun get(simpleName: String, context: Binding.Kind?) : BindingSearchResult {
		val imported = imports.map { environment.getScope(it) }
			.flatMap { it.bindings }
		val all = (bindings + imported).distinct()
		val matches = all.filter {
			(it.simpleName == simpleName || it.path.toString(OrbitMangler) == simpleName)
				&& (context == null || it.kind == context)
		}

		return when (matches.size) {
			0 -> BindingSearchResult.None(simpleName)
			1 -> BindingSearchResult.Success(matches.first())
			else -> {
				if (context == null) {
					// If there is no expected Kind, there is no way for us to narrow down the search
					return BindingSearchResult.Multiple(matches)
				}

				val refined = matches.filter { it.kind == context }

				when (refined.size) {
					0 -> BindingSearchResult.None(simpleName)
					1 -> BindingSearchResult.Success(refined[0])
					else -> return BindingSearchResult.Multiple(matches)
				}
			}
		}
	}

	fun get(path: Path, context: Binding.Kind? = null) : BindingSearchResult {
		val candidates = bindings.filter { it.path == path }

		return when (candidates.size) {
			0 -> BindingSearchResult.None(path.toString(OrbitMangler))
			1 -> BindingSearchResult.Success(candidates.first())
			else -> {
				if (context == null) {
					return BindingSearchResult.Multiple(candidates)
				}

				val refined = candidates.filter { it.kind == context }

				when (refined.size) {
					0 -> BindingSearchResult.None(path.toString(OrbitMangler))
					1 -> BindingSearchResult.Success(refined.first())
					else -> BindingSearchResult.Multiple(refined)
				}
			}
		}
	}

	fun filter(where: (Binding) -> Boolean) : BindingSearchResult {
		return bindings.filter(where)
			.map { BindingSearchResult.Success(it) }
			.fold<BindingSearchResult, BindingSearchResult>(BindingSearchResult.None("")) { acc, next ->
				return@fold acc + next
			}
	}

	override fun toString(): String {
		return "SCOPE($identifier) <- PARENT(${parentScope?.identifier})\n" + bindings.joinToString("\n") { "\t" + it.toString() }
	}

	override fun describe(json: JSONObject) {}
}