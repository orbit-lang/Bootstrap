package org.orbit.core.components

object TokenTypes : TokenTypeProvider {
    // Symbols
    object Colon : TokenType("Colon", "\\:", true, false, Family.Op)
    object Comma : TokenType("Comma", "\\,", true, false, Family.Op)
    object Dot : TokenType("Dot", "\\.", true, false, Family.Op)
    object Assignment : TokenType("Assignment", "\\=", true, false, Family.Op)
    object Dollar : TokenType("Dollar", "\\$[0-9]+", true, false, Family.Op)
    object OperatorSymbol : TokenType("OperatorSymbol", "((?:`[^`]+`)|(?:\\.{2,})|(?:[\\+\\-\\*\\/\\^\\!\\?\\%\\&\\<\\>\\|\\•]+))", true, false, Family.Op)
    object Annotation : TokenType("Annotation", "@", true, false, Family.Op)
    object LExpand : TokenType("LExpand", "\\$\\{", true, false, Family.Op)

    // Enclosing
    object LParen : TokenType("LParen", "(", true, false, Family.Enclosing)
    object RParen : TokenType("RParen", ")", true, false, Family.Enclosing)
    object LBracket : TokenType("LBracket", "[", true, false, Family.Enclosing)
    object RBracket : TokenType("RBracket", "]", true, false, Family.Enclosing)
    object LBrace : TokenType("LBrace", "{", true, false, Family.Enclosing)
    object RBrace : TokenType("RBrace", "}", true, false, Family.Enclosing)
    object LAngle : TokenType("LAngle", "<", true, false, Family.Enclosing)
    object RAngle : TokenType("RAngle", ">", true, false, Family.Enclosing)

    object Whitespace : TokenType("Whitespace", "[ \\t\\n\\r]", true, false, Family.White)
	
    // Keywords
    object Api : TokenType("API", "api", true, false, Family.Keyword)
    object Attribute : TokenType("Attribute", "attribute", true, false, Family.Keyword)
    object Check : TokenType("Check", "check", true, false, Family.Keyword)
    object Cause : TokenType("Cause", "cause", true, false, Family.Keyword)
    object Effect : TokenType("Effect", "effect", true, false, Family.Keyword)
    object For : TokenType("For", "for", true, false, Family.Keyword)
    object TypeEffect : TokenType("TypeEffect", "\\btype effect\\b", false, false, Family.Keyword)
    object Type : TokenType("Type", "type", true, false, Family.Keyword + Family.Kind)
    object Trait : TokenType("Trait", "trait", true, false, Family.Keyword + Family.Kind)
    object Family : TokenType("Family", "family", true, false, Family.Keyword + Family.Kind)
    object With : TokenType("With", "with", true, false, Family.Keyword)
    object Within : TokenType("Within", "within", true, false, Family.Keyword)
    object Return : TokenType("Return", "return", true, false, Family.Keyword)
    object Module : TokenType("Module", "module", true, false, Family.Keyword)
    object Define : TokenType("Define", "define", true, false, Family.Keyword)
    object Defer : TokenType("Defer", "defer", true, false, Family.Keyword)
    object Fun : TokenType("Fun", "fun", true, false, Family.Keyword)
    object Print : TokenType("Print", "print", true, false, Family.Keyword)
    object Required : TokenType("Required", "required", true, false, Family.Keyword)
    object Constructor : TokenType("Constructor", "constructor", true, false, Family.Keyword)
    object Projection : TokenType("Projection", "projection", true, false, Family.Keyword)
    object Extension : TokenType("Extension", "extension", true, false, Family.Keyword)
    object Alias : TokenType("Alias", "alias", true, false, Family.Keyword)
    object Observe : TokenType("Observer", "observe", true, false, Family.Keyword)
    object Where : TokenType("Where", "where", true, false, Family.Keyword)
    object Let : TokenType("Let", "let", true, false, Family.Keyword)
    object In : TokenType("In", "in", true, false, Family.Keyword)
    object Of : TokenType("Of", "of", true, false, Family.Keyword)
    object TypeOf : TokenType("TypeOf", "typeOf", true, false, Family.Keyword)
    object ContextOf : TokenType("ContextOf", "contextOf", true, false, Family.Keyword)
    object RefOf : TokenType("RefOf", "refOf", true, false, Family.Keyword)
    object Expand : TokenType("Expand", "expand", true, false, Family.Keyword)
    object Mirror : TokenType("Mirror", "mirror", true, false, Family.Keyword)
    object Context : TokenType("Context", "context", true, false, Family.Keyword)
    object By : TokenType("By", "by", true, false, Family.Keyword)
    object To : TokenType("To", "to", true, false, Family.Keyword)
    object Operator : TokenType("Operator", "operator", true, false, Family.Keyword)
    object Else : TokenType("Else", "else", true, false, Family.Keyword)
    object Case : TokenType("Case", "case", true, false, Family.Keyword)
    object Select : TokenType("Select", "select", true, false, Family.Keyword)
    object As : TokenType("As", "as", true, false, Family.Keyword)
    object True : TokenType("True", "true", true, false, Family.Keyword)
    object False : TokenType("False", "false", true, false, Family.Keyword)
    object Panic : TokenType("Panic", "panic", true, false, Family.Keyword)
    object Prefix : TokenType("Prefix", "prefix", true, false, Family.Keyword)
    object Infix : TokenType("Infix", "infix", true, false, Family.Keyword)
    object Postfix : TokenType("Postfix", "postfix", true, false, Family.Keyword)
    object Variadic : TokenType("Variadic", "variadic", true, false, Family.Keyword)
    object Query : TokenType("Query", "query", true, false, Family.Keyword)

    // Compile-time functions
    object Synthesise : TokenType("Synthesise", "\\bsynthesise\\b", true, false, Family.CompileTime)

    // Literals
    object Int : TokenType("Int", "[0-9]+", true, false, Family.Num)
    object Real : TokenType("Real", "[0-9]+\\\\.[0-9]+", true, false, Family.Num)
    object Identifier : TokenType("Identifier", "[a-z_]+[a-zA-Z0-9_]*", true, false, Family.Id)
    object TypeIdentifier : TokenType("TypeIdentifier", "(_|([A-Z]+[a-zA-Z0-9_]*)(::[A-Z]+[a-zA-Z0-9_]*)*(::\\*)?)", true, false, Family.Id)
    object Symbol : TokenType("Symbol", "\\:[a-zA-Z_]+[a-zA-Z0-9_]*", false, false, Family.Id)
    object EOS : TokenType("", "", true, false, Family.White)

    override fun getTokenTypes() : List<TokenType> {
        // NOTE - Keywords MUST be listed before the Identifier token type
        // Generally, the order of this list matters!
        return listOf(
            Real, Int, Dot, Attribute, Context, Api, Module, Define, Defer, Cause, Effect, For, TypeEffect, Fun, Observe, Where, Print,
            Required, Projection, Extension, Constructor, Alias, Operator, Variadic, Query,
            Synthesise,
            Dollar,
            Prefix, Infix, Postfix, Let, In, Of, By, To, Else, Case, Select, As,
            True, False, Panic,
            Colon, Comma, Assignment, Annotation, Whitespace,
            LParen, RParen, LBracket, RBracket, LBrace, RBrace, LAngle, RAngle, LExpand,
            Expand, Mirror, TypeOf, RefOf, Check, ContextOf, Type, Trait, Within, With, Return, Family,
            OperatorSymbol, TypeIdentifier, Identifier
        )
    }
}
