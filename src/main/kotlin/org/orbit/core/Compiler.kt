package org.orbit.core

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.util.Invocation

class Compiler(override val invocation: Invocation, private val phaseLinker: PhaseLinker<SourceProvider, *, *, *>) :
    Phase<SourceProvider, Any> {
	override fun execute(input: SourceProvider) : Any {
		return phaseLinker.execute(input)
	}
}

typealias AnyPhase = ReifiedPhase<Any, Any>

data class CompilationSchemeEntry(val uniqueIdentifier: String, val resultIdentifier: String) {
	companion object Intrinsics {
		val commentParser = CompilationSchemeEntry("CommentParser", "__source__")
		val lexer = CompilationSchemeEntry("Lexer", "CommentParser")
		val parser = CompilationSchemeEntry("Parser", "Lexer")
		val observers = CompilationSchemeEntry("Observers", "__source__")
		val canonicalNameResolver = CompilationSchemeEntry("CanonicalNameResolver", "Parser")
		val typeChecker = CompilationSchemeEntry("TypeChecker", "CanonicalNameResolver")
		val mainResolver = CompilationSchemeEntry("MainResolver", "Parser")
	}

	override fun equals(other: Any?): Boolean = when (other) {
		is CompilationSchemeEntry -> other.uniqueIdentifier == uniqueIdentifier
		else -> false
	}
}

inline fun <reified O: Any> Invocation.storeResult(key: CompilationSchemeEntry, value: O) {
	storeResult(key.uniqueIdentifier, value)
}

inline fun <reified O: Any> Invocation.mergeResult(key: CompilationSchemeEntry, result: O, where: (CompilationSchemeEntry) -> Boolean) {
	mergeResult(key.uniqueIdentifier, result) { it == key.uniqueIdentifier }
}

inline fun <reified O: Any> Invocation.getResult(key: CompilationSchemeEntry) : O {
	return getResults<O>(key.uniqueIdentifier).first()
}

open class CompilationScheme(entries: List<CompilationSchemeEntry>) : MutableListIterator<CompilationSchemeEntry> {
	private val _entries = entries.toMutableList()

	companion object Intrinsics : CompilationScheme(listOf(
		CompilationSchemeEntry.commentParser,
		CompilationSchemeEntry.lexer,
		CompilationSchemeEntry.parser,
		CompilationSchemeEntry.canonicalNameResolver,
		CompilationSchemeEntry.observers,
		CompilationSchemeEntry.typeChecker,
		CompilationSchemeEntry.mainResolver
	))

	override fun next(): CompilationSchemeEntry {
		return _entries.removeFirst()
	}

	override fun hasPrevious(): Boolean {
		return false
	}

	override fun nextIndex(): Int {
		return 0
	}

	override fun previous(): CompilationSchemeEntry {
		TODO("Not yet implemented")
	}

	override fun previousIndex(): Int {
		TODO("Not yet implemented")
	}

	override fun add(element: CompilationSchemeEntry) {
		_entries.add(0, element)
	}

	override fun hasNext(): Boolean = _entries.isNotEmpty()

	override fun remove() {
		_entries.removeFirst()
	}

	override fun set(element: CompilationSchemeEntry) {
		TODO("Not yet implemented")
	}
}

class CompilerGenerator(private val invocation: Invocation, phases: Map<String, AnyPhase> = emptyMap()) : KoinComponent {
	private val phases = phases.toMutableMap()
	private val eventBus: CompilationEventBus by inject()

	operator fun set(key: String, value: ReifiedPhase<*, *>) {
		phases[key] = value as AnyPhase
	}

	operator fun set(key: CompilationSchemeEntry, value: ReifiedPhase<*, *>) {
		set(key.uniqueIdentifier, value)
	}

	fun run(scheme: CompilationScheme) {
		for (entry in scheme) {
			val phase = phases[entry.uniqueIdentifier] ?: throw RuntimeException("Unknown compilation phase '${entry.uniqueIdentifier}'")
			val resultPhase = phases[entry.resultIdentifier] ?: throw RuntimeException("Cannot find compilation input result for phase '${entry.uniqueIdentifier}'")

			eventBus.notify(PhaseLifecycle.Event(PhaseLifecycle.Init, entry.uniqueIdentifier))

			var input = invocation.phaseResults[entry.resultIdentifier]
				?: throw RuntimeException("FATAL")

			input = phase.inputType.safeCast(input)
				?: (phase as? AdaptablePhase)?.getAdapterSafe(resultPhase.outputType)?.bridge(input)
				?: throw RuntimeException("FATAL")

			eventBus.notify(PhaseLifecycle.Event(PhaseLifecycle.Before, entry.uniqueIdentifier))

			phase.execute(input)

			eventBus.notify(PhaseLifecycle.Event(PhaseLifecycle.After, entry.uniqueIdentifier))
		}
	}
}

class DummyPhase(override val invocation: Invocation, private val result: Any) : ReifiedPhase<Unit, Any> {
	override val inputType: Class<Unit> = Unit::class.java
	override val outputType: Class<Any> = Any::class.java

	override fun execute(input: Unit): Any {
		return result
	}
}