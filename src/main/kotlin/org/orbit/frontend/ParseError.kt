package org.orbit.frontend

import org.orbit.core.SourcePosition

abstract class ParseError(
	message: String, open val position: SourcePosition
) : Exception("Parse error @ line: ${position.line}, char: ${position.character}\n\t$message")