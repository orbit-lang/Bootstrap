package org.orbit.frontend.components

import org.orbit.core.components.TokenType
import org.orbit.core.components.TokenTypeProvider

object TokenTypes : TokenTypeProvider {
    // Symbols
    object Colon : TokenType("Colon", "\\:", true, false, Family.Op)
    object Comma : TokenType("Comma", "\\,", true, false, Family.Op)
    object Dot : TokenType("Dot", "\\.", true, false, Family.Op)
    object LParen : TokenType("LParen", "\\(", true, false, Family.Enclosing)
    object RParen : TokenType("RParen", "\\)", true, false, Family.Enclosing)
    object LBracket : TokenType("LBracket", "\\[", true, false, Family.Enclosing)
    object RBracket : TokenType("RBracket", "\\]", true, false, Family.Enclosing)
    object LBrace : TokenType("LBrace", "\\{", true, false, Family.Enclosing)
    object RBrace : TokenType("RBrace", "\\}", true, false, Family.Enclosing)
    object LAngle : TokenType("LAngle", "\\<", true, false, Family.Enclosing)
    object RAngle : TokenType("RAngle", "\\>", true, false, Family.Enclosing)
    object Assignment : TokenType("Assignment", "\\=", true, false, Family.Op)
    object Operator : TokenType("Operator", "[\\+\\-\\*\\/\\^\\!\\?\\%\\&\\<\\>\\|]", true, false, Family.Op)
    object Annotation : TokenType("Annotation", "@", true, false, Family.Op)
    object Whitespace : TokenType("Whitespace", "[ \\t\\n\\r]", true, false, Family.White)

	// Comments
	object MultiLineComment : TokenType("MultiLineComment", "\\/\\*.**\\/", true, false, Family.Comment)
	
    // Keywords
    object Api : TokenType("API", "api", true, false, Family.Keyword)
    object Type : TokenType("Type", "type", true, false, Family.Keyword)
    object Trait : TokenType("Trait", "trait", true, false, Family.Keyword)
    object With : TokenType("With", "with", true, false, Family.Keyword)
    object Within : TokenType("Within", "within", true, false, Family.Keyword)
	object Return : TokenType("Return", "return", true, false, Family.Keyword)
    object Module : TokenType("Module", "module", true, false, Family.Keyword)
    object Define : TokenType("Define", "define", true, false, Family.Keyword)
    object Defer : TokenType("Defer", "defer", true, false, Family.Keyword)
    object Print : TokenType("Print", "print", true, false, Family.Keyword)
    object Required : TokenType("Required", "required", true, false, Family.Keyword)
    object Constructor : TokenType("Constructor", "constructor", true, false, Family.Keyword)
    object Projection : TokenType("Projection", "projection", true, false, Family.Keyword)
    object Extension : TokenType("Extension", "extension", true, false, Family.Keyword)
    object Alias : TokenType("Alias", "alias", true, false, Family.Keyword)
    // NOTE - This is kind of dirty, but if we want arbitrary keywords, we need a way to make the lexer
    // avoid recognising them in regular identifiers, e.g. a variable named "observeSomething"
    object Observe : TokenType("Observer", "observe ", true, false, Family.Keyword)
    object Where : TokenType("Where", "where", true, false, Family.Keyword)
    object Let : TokenType("Let", "let", true, false, Family.Keyword)
    object In : TokenType("In", "\\bin\\b", true, false, Family.Keyword)
    object Invoke : TokenType("Invoke", "invoke", true, false, Family.Keyword)

    // Literals
    object Int : TokenType("Int", "[0-9]+", true, false, Family.Num)
    object Real : TokenType("Real", "[0-9]+\\\\.[0-9]+", true, false, Family.Num)
    object Identifier : TokenType("Identifier", "[a-z_]+[a-zA-Z0-9_]*", true, false, Family.Id)
    object TypeIdentifier : TokenType("TypeIdentifier", "([A-Z]+[a-zA-Z0-9_]*)(::[A-Z]+[a-zA-Z0-9_]*)*(::\\*)?", true, false, Family.Id)
	object Symbol : TokenType("Symbol", "\\:[a-zA-Z_]+[a-zA-Z0-9_]*", false, false, Family.Id)
	object EOS : TokenType("", "", true, false, Family.White)

    override fun getTokenTypes() : List<TokenType> {
        // NOTE - Keywords MUST be listed before the Identifier token type
        // Generally, the order of this list matters!
        return listOf(
            Int, Real, Api, Module, Define, Defer, Observe, Where, Print,
            Required, Projection, Extension, Constructor, Alias,
            Let, Invoke, In,
            TypeIdentifier,
            Colon, Comma, Dot, Assignment, Annotation, Whitespace,
            LParen, RParen, LBracket, RBracket, LBrace, RBrace, LAngle, RAngle,
            Type, Trait, Within, With, Return,
            Operator, Identifier
        )
    }
}