package org.orbit.precess.frontend.rules

//object TypeLookupRule : ParseRule<TypeLookupNode> {
//    override fun parse(context: Parser): ParseRule.Result {
//        val type = context.attempt(AnyTypeExpressionRule)
//            ?: return ParseRule.Result.Failure.Abort
//
//        return +TypeLookupNode(type.firstToken, type.lastToken, type)
//    }
//}