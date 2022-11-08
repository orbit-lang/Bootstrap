package org.orbit.core.components

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
    object LExpand : TokenType("LExpand", "\\$\\{", true, false, Family.Enclosing)
    object Assignment : TokenType("Assignment", "\\=", true, false, Family.Op)
    object Dollar : TokenType("Dollar", "\\$[0-9]+", true, false, Family.Op)
    object OperatorSymbol : TokenType("OperatorSymbol", "((?:`[^`]+`)|(?:\\.{2,})|(?:[\\+\\-\\*\\/\\^\\!\\?\\%\\&\\<\\>\\|]+))", true, false, Family.Op)
    object Annotation : TokenType("Annotation", "@", true, false, Family.Op)
    object Whitespace : TokenType("Whitespace", "[ \\t\\n\\r]", true, false, Family.White)
	
    // Keywords
    object Api : TokenType("API", "\\bapi\\b", true, false, Family.Keyword)
    object Check : TokenType("Check", "\\bcheck\\b", true, false, Family.Keyword)
    object Type : TokenType("Type", "\\btype\\b", true, false, Family.Keyword + Family.Kind)
    object Trait : TokenType("Trait", "\\btrait\\b", true, false, Family.Keyword + Family.Kind)
    object Family : TokenType("Family", "\\bfamily\\b", true, false, Family.Keyword + Family.Kind)
    object With : TokenType("With", "\\bwith\\b", true, false, Family.Keyword)
    object Within : TokenType("Within", "\\bwithin\\b", true, false, Family.Keyword)
    object Return : TokenType("Return", "\\breturn\\b", true, false, Family.Keyword)
    object Module : TokenType("Module", "\\bmodule\\b", true, false, Family.Keyword)
    object Define : TokenType("Define", "\\bdefine\\b", true, false, Family.Keyword)
    object Defer : TokenType("Defer", "\\bdefer\\b", true, false, Family.Keyword)
    object Print : TokenType("Print", "\\bprint\\b", true, false, Family.Keyword)
    object Required : TokenType("Required", "\\brequired\\b", true, false, Family.Keyword)
    object Constructor : TokenType("Constructor", "\\bconstructor\\b", true, false, Family.Keyword)
    object Projection : TokenType("Projection", "\\bprojection\\b", true, false, Family.Keyword)
    object Extension : TokenType("Extension", "\\bextension\\b", true, false, Family.Keyword)
    object Alias : TokenType("Alias", "\\balias\\b", true, false, Family.Keyword)
    object Observe : TokenType("Observer", "\\bobserve\\b", true, false, Family.Keyword)
    object Where : TokenType("Where", "\\bwhere\\b", true, false, Family.Keyword)
    object Let : TokenType("Let", "\\blet\\b", true, false, Family.Keyword)
    object In : TokenType("In", "\\bin\\b", true, false, Family.Keyword)
    object Invoke : TokenType("Invoke", "\\binvoke\\b", true, false, Family.Keyword)
    object Of : TokenType("Of", "\\bof\\b", true, false, Family.Keyword)
    object TypeOf : TokenType("TypeOf", "\\btypeOf\\b", true, false, Family.Keyword)
    object ContextOf : TokenType("ContextOf", "\\bcontextOf\\b", true, false, Family.Keyword)
    object Expand : TokenType("Expand", "\\bexpand\\b", true, false, Family.Keyword)
    object Mirror : TokenType("Mirror", "\\bmirror\\b", true, false, Family.Keyword)
    object Context : TokenType("Context", "\\bcontext\\b", true, false, Family.Keyword)
    object By : TokenType("By", "\\bby\\b", true, false, Family.Keyword)
    object To : TokenType("To", "\\bto\\b", true, false, Family.Keyword)
    object Fixity : TokenType("Fixity", "\\b(in|pre|post)fix\\b", true, false, Family.Keyword)
    object Operator : TokenType("Operator", "\\boperator\\b", true, false, Family.Keyword)
    object Call : TokenType("Call", "call", true, false, Family.Keyword)
    object Else : TokenType("Else", "else", true, false, Family.Keyword)
    object Case : TokenType("Case", "case", true, false, Family.Keyword)
    object Select : TokenType("Select", "select", true, false, Family.Keyword)
    object As : TokenType("As", "as", true, false, Family.Keyword)
    object Bool : TokenType("Bool", "(true|false)", true, false, Family.Keyword)
    object Panic : TokenType("Panic", "panic", true, false, Family.Keyword)

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
            Int, Real, Context, Api, Module, Define, Defer, Observe, Where, Print,
            Required, Projection, Extension, Constructor, Alias, Operator,
            Synthesise,
            Call,
            Dollar,
            Fixity, Let, Invoke, In, Of, By, To, Else, Case, Select, As,
            Bool, Panic,
            TypeIdentifier,
            Colon, Comma, Assignment, Annotation, Whitespace,
            LParen, RParen, LBracket, RBracket, LBrace, RBrace, LAngle, RAngle, LExpand,
            Expand, Mirror, TypeOf, Check, ContextOf, Type, Trait, Within, With, Return, Family,
            OperatorSymbol, Identifier, Dot
        )
    }
}
