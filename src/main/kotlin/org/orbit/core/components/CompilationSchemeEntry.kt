package org.orbit.core.components

data class CompilationSchemeEntry(val uniqueIdentifier: String, val resultIdentifier: String) {
	companion object Intrinsics {
		val commentParser = CompilationSchemeEntry("CommentParser", "__source__")
		val lexer = CompilationSchemeEntry("Lexer", "CommentParser")
		val parser = CompilationSchemeEntry("Parser", "Lexer")
		val observers = CompilationSchemeEntry("Observers", "__source__")
		val canonicalNameResolver = CompilationSchemeEntry("NameResolverResult", "Parser")
		val typeChecker = CompilationSchemeEntry("TypeChecker", "NameResolverResult")
		val mainResolver = CompilationSchemeEntry("MainResolver", "Parser")
	}

	override fun equals(other: Any?): Boolean = when (other) {
		is CompilationSchemeEntry -> other.uniqueIdentifier == uniqueIdentifier
		else -> false
	}
}