package org.orbit.frontend

import org.orbit.core.TokenType
import org.orbit.core.TokenTypeProvider

object TokenTypes : TokenTypeProvider {
    // Symbols
    object Colon : TokenType("Colon", "\\:", true, false)
    object Comma : TokenType("Comma", "\\,", true, false)
    object Dot : TokenType("Dot", "\\.", true, false)
    object LParen : TokenType("LParen", "\\(", true, false)
    object RParen : TokenType("RParen", "\\)", true, false)
    object LBracket : TokenType("LBracket", "\\[", true, false)
    object RBracket : TokenType("RBracket", "\\]", true, false)
    object LBrace : TokenType("LBrace", "\\{", true, false)
    object RBrace : TokenType("RBrace", "\\}", true, false)
    object LAngle : TokenType("LAngle", "\\<", true, false)
    object RAngle : TokenType("RAngle", "\\>", true, false)
    object Assignment : TokenType("Assignment", "\\=", true, false)
    object Operator : TokenType("Operator", "[\\+\\-\\*\\/\\^\\!\\?\\%\\&\\<\\>\\|]+", true, false)
    object Annotation : TokenType("Annotation", "@", true, false)
    object Whitespace : TokenType("Whitespace", "[ \\t\\n\\r]", true, false)

	// Comments
	object MultiLineComment : TokenType("MultiLineComment", "\\/\\*.**\\/", true, false)
	
    // Keywords
    object Api : TokenType("API", "api", true, false)
    object Type : TokenType("Type", "type", true, false)
    object Trait : TokenType("Trait", "trait", true, false)
    object With : TokenType("With", "with", true, false)
    object Within : TokenType("Within", "within", true, false)
	object Return : TokenType("Return", "return", true, false)

    // Literals
    object Int : TokenType("Int", "[0-9]+", true, false)
    object Real : TokenType("Real", "[0-9]+\\\\.[0-9]+", true, false)
    object Identifier : TokenType("Identifier", "[a-z_]+[a-zA-Z0-9_]*", true, false)
    object TypeIdentifier : TokenType("TypeIdentifier", "([A-Z]+[a-zA-Z0-9_]*)(::[A-Z]+[a-zA-Z0-9_]*)*", true, false)
	
    override fun getTokenTypes() : List<TokenType> {
        // NOTE - Keywords MUST be listed before the Identifier token type
        // Generally, the order of this list matters!
        return listOf(
            Int, Real, Api, TypeIdentifier,
            Colon, Comma, Dot, Assignment, Annotation, Whitespace,
            LParen, RParen, LBracket, RBracket, LBrace, RBrace, LAngle, RAngle,
            Type, Trait, Within, With, Return,
            Operator, Identifier
        )
    }
}