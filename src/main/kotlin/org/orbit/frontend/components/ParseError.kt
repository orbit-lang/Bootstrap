package org.orbit.frontend.components

import org.orbit.core.components.SourcePosition
import org.orbit.frontend.phase.Parser
import org.orbit.util.OrbitError

abstract class ParseError(
	private val msg: String,
	open val position: SourcePosition
)
	: Throwable(), OrbitError<Parser> {
	override val phaseClazz: Class<Parser>
		get() = Parser::class.java

	override val message: String
		get() = "$msg @ $position"
}