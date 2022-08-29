package org.orbit.precess.frontend.components

import org.orbit.core.components.TokenType
import org.orbit.core.components.TokenTypeProvider

object TokenTypes : TokenTypeProvider {
    object Delta : TokenType("Delta", "∆", true, false, TokenType.Family.Keyword)
    object RefId : TokenType("Ref", "[a-z]+[a-zA-Z0-9_]*", true, false, TokenType.Family.Id)
    object TypeId : TokenType("Type", "(\\?|[A-Z]+[a-zA-Z0-9_]*)", true, false, TokenType.Family.Id)
    object Box : TokenType("Box", "box", true, false, TokenType.Family.Keyword)
    object Check : TokenType("Check", "check", true, false, TokenType.Family.Keyword)
    object Infer : TokenType("Infer", "infer", true, false, TokenType.Family.Keyword)
    object Dump : TokenType("Dump", "dump", true, false, TokenType.Family.Keyword)
    object Exists : TokenType("Exists", "exists", true, false, TokenType.Family.Keyword)
    object In : TokenType("In", "in", true, false, TokenType.Family.Keyword)
    object As : TokenType("As", "as", true, false, TokenType.Family.Keyword)
    object Run : TokenType("Run", "run", true, false, TokenType.Family.Keyword)
    object Safe : TokenType("Safe", "safe", true, false, TokenType.Family.Keyword)
    object SummonValue : TokenType("SummonValue", "summonValue", true, false, TokenType.Family.Keyword)
    object Unbox : TokenType("Unbox", "unbox", true, false, TokenType.Family.Keyword)
    object Assign : TokenType("Assign", "\\=", true, false, TokenType.Family.Keyword)
    object FatArrow : TokenType("FatArrow", "\\=\\>", true, false, TokenType.Family.Keyword)
    object Arrow : TokenType("Arrow", "\\-\\>", true, false, TokenType.Family.Keyword)
    object Bind : TokenType("Bind", "\\:", true, false, TokenType.Family.Keyword)
    object LParen : TokenType("LParen", "\\(", true, false, TokenType.Family.Enclosing)
    object RParen : TokenType("RParen", "\\)", true, false, TokenType.Family.Enclosing)
    object LBrace : TokenType("LBrace", "\\{", true, false, TokenType.Family.Enclosing)
    object RBrace : TokenType("RBrace", "\\}", true, false, TokenType.Family.Enclosing)
    object ContextOperator : TokenType("ContextOp", "[\\+|\\-]", true, false, TokenType.Family.Op)
    object And : TokenType("And", "\\&", true, false, TokenType.Family.Op)
    object Dot : TokenType("Dot", "\\.", true, false, TokenType.Family.Op)
    object Comma : TokenType("Comma", "\\,", true, false, TokenType.Family.Op)
    object TypeAttribute : TokenType("TypeAttr", "[\\!\\?]", true, false, TokenType.Family.Op)
    object TypeOperator : TokenType("TypeOp", "[∏∑]", true, false, TokenType.Family.Op)

    override fun getTokenTypes(): List<TokenType> = listOf(
        Box, Check, Infer, In, As, Run, Dump, Exists, Safe, SummonValue, Unbox,
        Delta, RefId, TypeId, Check, FatArrow, Arrow, Assign,
        Bind, LParen, RParen, LBrace, RBrace, ContextOperator, Dot,
        Comma, And, TypeAttribute, TypeOperator
    )
}