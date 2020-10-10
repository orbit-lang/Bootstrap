package org.orbit.core

import org.orbit.util.Invocation

class Compiler(override val invocation: Invocation, private val phaseLinker: PhaseLinker<SourceProvider, *, *, *>) :
    Phase<SourceProvider, Any> {
	override fun execute(input: SourceProvider) : Any {
		return phaseLinker.execute(input)
	}
}