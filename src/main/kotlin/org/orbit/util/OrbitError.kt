package org.orbit.util

import org.orbit.core.Phase

interface OrbitError<out P: Phase<*, *>> {
	val phaseClazz: Class<out P>
	val message: String
}