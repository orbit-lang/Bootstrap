package org.orbit.core

import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.phase.Phase
import org.orbit.core.phase.PhaseLinker
import org.orbit.core.phase.ReifiedPhase
import org.orbit.util.Invocation

inline fun <reified O: Any> Invocation.getResult(key: CompilationSchemeEntry) : O {
	return getResults<O>(key.uniqueIdentifier).first()
}