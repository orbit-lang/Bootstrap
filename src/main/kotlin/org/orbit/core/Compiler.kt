package org.orbit.core

import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.phase.Phase
import org.orbit.core.phase.PhaseLinker
import org.orbit.core.phase.ReifiedPhase
import org.orbit.util.Invocation

class Compiler(override val invocation: Invocation, private val phaseLinker: PhaseLinker<SourceProvider, *, *, *>) :
	Phase<SourceProvider, Any> {
	override fun execute(input: SourceProvider) : Any {
		return phaseLinker.execute(input)
	}
}

typealias AnyPhase = ReifiedPhase<Any, Any>

inline fun <reified O: Any> Invocation.storeResult(key: CompilationSchemeEntry, value: O) {
	storeResult(key.uniqueIdentifier, value)
}

inline fun <reified O: Any> Invocation.mergeResult(key: CompilationSchemeEntry, result: O, where: (CompilationSchemeEntry) -> Boolean) {
	mergeResult(key.uniqueIdentifier, result) { it == key.uniqueIdentifier }
}

inline fun <reified O: Any> Invocation.getResult(key: CompilationSchemeEntry) : O {
	return getResults<O>(key.uniqueIdentifier).first()
}

class DummyPhase(override val invocation: Invocation, private val result: Any) : ReifiedPhase<Unit, Any> {
	override val inputType: Class<Unit> = Unit::class.java
	override val outputType: Class<Any> = Any::class.java

	override fun execute(input: Unit): Any {
		return result
	}
}