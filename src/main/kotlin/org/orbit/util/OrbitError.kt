package org.orbit.util

import org.orbit.core.Phase
import org.orbit.core.SourcePosition

interface OrbitError<out P: Phase<*, *>> {
	val phaseClazz: Class<out P>
	val message: String
	val sourcePosition: SourcePosition
}

interface Fatal<out P: Phase<*, *>> : OrbitError<P>
interface Phased<out P: Phase<*, *>> : OrbitError<P>