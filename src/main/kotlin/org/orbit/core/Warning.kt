package org.orbit.core

abstract class Warning(
	open val message: String,
	open val position: SourcePosition
)