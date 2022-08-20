package org.orbit.precess.frontend.components

import org.orbit.core.components.TokenType
import org.orbit.core.components.TokenTypeProvider

object TokenTypes : TokenTypeProvider {
    object Delta : TokenType("Delta", "∆[a-zA-Z]+[a-zA-Z0-9]*", true, false, TokenType.Family.Id)
    object RefId : TokenType("Ref", "[a-z]+[a-zA-Z0-9_]*", true, false, TokenType.Family.Id)
    object TypeId : TokenType("Type", "[A-Z]+[a-zA-Z0-9_]*", true, false, TokenType.Family.Id)
    object Assign : TokenType("Assign", "\\=", true, false, TokenType.Family.Keyword)
    object FatArrow : TokenType("FatArrow", "\\=\\>", true, false, TokenType.Family.Keyword)
    object Arrow : TokenType("Arrow", "\\-\\>", true, false, TokenType.Family.Keyword)
    object Bind : TokenType("Bind", "\\:", true, false, TokenType.Family.Keyword)
    object Infer : TokenType("Infer", "\\?", true, false, TokenType.Family.Op)
    object Check : TokenType("Check", "\\!", true, false, TokenType.Family.Op)
    object LParen : TokenType("LParen", "\\(", true, false, TokenType.Family.Enclosing)
    object RParen : TokenType("RParen", "\\)", true, false, TokenType.Family.Enclosing)
    object LBrace : TokenType("LBrace", "\\{", true, false, TokenType.Family.Enclosing)
    object RBrace : TokenType("RBrace", "\\}", true, false, TokenType.Family.Enclosing)
    object Extend : TokenType("Extend", "\\+", true, false, TokenType.Family.Op)

    override fun getTokenTypes(): List<TokenType> = listOf(
        Delta, RefId, TypeId, FatArrow, Arrow, Assign, Bind, Infer, Check, LParen, RParen, LBrace, RBrace, Extend
    )
}