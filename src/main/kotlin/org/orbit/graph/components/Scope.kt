package org.orbit.graph.components

import org.json.JSONObject
import org.orbit.core.*
import org.orbit.core.components.SourcePosition
import org.orbit.core.components.CompilationEvent
import org.orbit.core.components.CompilationEventBusAware
import org.orbit.core.components.CompilationEventBusAwareImpl
import org.orbit.core.phase.Phase
import org.orbit.graph.pathresolvers.PathResolver
import org.orbit.serial.Serial
import org.orbit.util.Fatal
import org.orbit.util.Monoid
import org.orbit.util.endsWith
import org.orbit.util.partial
import java.io.Serializable

class Scope(
	@Transient private val environment: Environment,
	val parentScope: Scope? = null,
	val identifier: ScopeIdentifier = ScopeIdentifier.next(),
	val bindings: MutableList<Binding> = mutableListOf(),
	private val imports: MutableSet<ScopeIdentifier> = mutableSetOf()
) : Serial, Serializable, CompilationEventBusAware by CompilationEventBusAwareImpl {
	sealed class Events(override val identifier: String) : CompilationEvent {
		class BindingCreated(binding: Binding) : Events("Scope Binding Created: $binding")
	}

	sealed class BindingSearchResult : Monoid<BindingSearchResult> {
		private data class BindingNotFound(
			override val phaseClazz: Class<out Phase<*, *>>,
			override val sourcePosition: SourcePosition,
			private val simpleName: String,
			private val contextualKind: Binding.Kind? = null
		) : Fatal<Phase<*, *>> {
			override val message: String
				get() = when (contextualKind) {
					null -> "Unknown binding '$simpleName'"
					else -> "Unknown binding '$simpleName' for context kind:\n\t\t${contextualKind.getName()}"
				}
		}

		private data class MultipleBindings(
			override val phaseClazz: Class<out Phase<*, *>>,
			override val sourcePosition: SourcePosition,
			private val candidates: List<Binding>
		) : Fatal<Phase<*, *>> {
			override val message: String
				get() = "Multiple candidates found for binding:\n\t" + candidates.joinToString("\n\t")
		}

		abstract fun unwrap(phase: Phase<*, *>, sourcePosition: SourcePosition) : Binding

		data class Success(val result: Binding) : BindingSearchResult() {
			override fun unwrap(phase: Phase<*, *>, sourcePosition: SourcePosition): Binding {
				return result
			}

			override fun BindingSearchResult.combine(other: BindingSearchResult): BindingSearchResult = when (other) {
				is None -> this
				is Success -> Multiple(listOf(result, other.result))
				is Multiple -> Multiple(other.results + result)
			}
		}

		class None(val simpleName: String, val contextualKind: Binding.Kind? = null) : BindingSearchResult() {
			override fun unwrap(phase: Phase<*, *>, sourcePosition: SourcePosition): Binding {
				// TODO - Is simpleName a hard requirement here?
				phase.invocation.reportError(BindingNotFound(phase::class.java, sourcePosition, simpleName, contextualKind))
				throw Exception("Unreachable")
			}

			override fun BindingSearchResult.combine(other: BindingSearchResult): BindingSearchResult = when (other) {
				is None -> this
				else -> other
			}
		}

		data class Multiple(val results: List<Binding>) : BindingSearchResult() {
			override fun unwrap(phase: Phase<*, *>, sourcePosition: SourcePosition): Binding {
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

	fun get2(name: String, context: Binding.Kind? = null) : BindingSearchResult {
		if (name == "Self") return BindingSearchResult.Success(Binding.Self)

		// Collect together all visible bindings
		val imported = imports.map { environment.getScope(it) }
			.flatMap { it.bindings }
		val all = (bindings + imported).distinct()

		val path = OrbitMangler.unmangle(name)

		// Get all bindings whose Path ends with name
		var matches = all.filter(partial(Binding::matches, name))

		if (matches.isEmpty()) return BindingSearchResult.None(name)
		if (matches.count() == 1) {
			val result = matches.first()
			if (context != null && !context.same(result.kind)) {
				return BindingSearchResult.None(name, context)
			}

			return BindingSearchResult.Success(matches.first())
		}

		// If we have multiple results, then we can start filtering on context.
		// Unless context is null, in which case we're screwed!
		if (context == null) return BindingSearchResult.Multiple(matches)

		matches = matches.filter { context.same(it.kind) }

		if (matches.isEmpty()) return BindingSearchResult.None(name)
		if (matches.count() == 1) return BindingSearchResult.Success(matches.first())

		val finalAttempt = matches.filter { it.path == path }

		if (finalAttempt.count() == 1) return BindingSearchResult.Success(finalAttempt.first())

		return BindingSearchResult.Multiple(matches)
	}

	fun get(simpleName: String, context: Binding.Kind?) : BindingSearchResult {
		if (simpleName == "Self") {
			return BindingSearchResult.Success(Binding.Self)
		}

		val imported = imports.map { environment.getScope(it) }
			.flatMap { it.bindings }
		val all = (bindings + imported).distinct()
			.toMutableList()

		val partialMatches = all.filter {
			it.simpleName == simpleName
				|| it.path.toString(OrbitMangler) == simpleName
		}

		val matches = partialMatches.filter {
			// For Kind.Union to work, the comparison order matters here!
			context == null || context.same(it.kind)
		}

		return when (matches.size) {
			0 -> BindingSearchResult.None(simpleName)
			1 -> BindingSearchResult.Success(matches.first())
			else -> {
				if (context == null) {
					// If there is no expected Kind, there is no way for us to narrow down the search
					// We can try to resolve this conflict automatically by giving priority
					// to a binding if it exists in the current scope
					// TODO - This doesn't work, but the idea is good
					for (binding in matches) {
						if (bindings.contains(binding)) {
							// TODO - We should probably raise a warning about potential conflicts here
							return BindingSearchResult.Success(binding)
						}
					}

					return BindingSearchResult.Multiple(matches)
				}

				val refined = matches.filter { it.kind == context }

				when (refined.size) {
					0 -> BindingSearchResult.None(simpleName)
					1 -> BindingSearchResult.Success(refined[0])
					else -> {
						// We can try to resolve this conflict automatically by giving priority
						// to a binding if it exists in the current scope
						// TODO - This doesn't work, but the idea is good
						for (binding in matches) {
							if (bindings.contains(binding)) {
								// TODO - We should probably raise a warning about potential conflicts here
								return BindingSearchResult.Success(binding)
							}
						}

						return BindingSearchResult.Multiple(matches)
					}
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

//	fun getChildren(graph: Graph) {
//		val rootVertex = graph.findVertex()
//	}

	override fun toString(): String {
		return "SCOPE($identifier) <- PARENT(${parentScope?.identifier})\n" + bindings.joinToString("\n") { "\t" + it.toString() }
	}

	override fun describe(json: JSONObject) {}
}