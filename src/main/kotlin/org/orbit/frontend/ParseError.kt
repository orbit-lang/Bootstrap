package org.orbit.frontend

import org.orbit.core.SourcePosition
import org.orbit.util.OrbitError

//abstract class ParseError(
//	message: String, open val position: SourcePosition
//) : Exception("Parse error @ line: ${position.line}, char: ${position.character}\n\t$message")

abstract class ParseError(
	private val msg: String,
	open val position: SourcePosition)
	: OrbitError<Parser> {
	override val phaseClazz: Class<Parser>
		get() = Parser::class.java

	override val message: String
		get() = "$msg @ $position"
}