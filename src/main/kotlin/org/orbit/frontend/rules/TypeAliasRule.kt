package org.orbit.frontend.rules

import org.orbit.core.nodes.TypeAliasNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.frontend.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object TypeAliasRule : ParseRule<TypeAliasNode> {
    override fun parse(context: Parser): ParseRule.Result {
        return context.record {
            val start = context.expectOrNull(TokenTypes.Alias)
                ?: return@record ParseRule.Result.Failure.Rewind(emptyList())

            // TODO - Allow specialisation on left-hand side
            // EXAMPLE: `alias StringMap = Map<String, _>`
            val source = context.attempt(TypeIdentifierRule.Naked)
                ?: return@record ParseRule.Result.Failure.Abort

            context.expect(TokenTypes.Assignment)

            val target = context.attempt(TypeExpressionRule)
                ?: return@record ParseRule.Result.Failure.Abort

            return@record +TypeAliasNode(start, target.lastToken, source, target)

//            val start = context.expect(TokenTypes.Alias)
//            val sourceToken = context.expect(TokenTypes.TypeIdentifier)
//
//            // NOTE - We must rewind if there is no "=" token to allow TypeDefRule to pick this up
//            //  We also have to ensure TypeAliasRule is attempted BEFORE TypeDefRule to ensure we
//            //  don't introduce ambiguities to the grammar
//            context.expectOrNull(TokenTypes.Assignment)
//                ?: return@record ParseRule.Result.Failure.Rewind(listOf(start, sourceToken))
//
//            // NOTE - We have an ambiguity in the grammar here, similar to the on in TypeDefRule & TraitDefRule.
//            //  Namely, if the syntactic structure following a type alias is a method signature, the receiver "looks"
//            //  like a property list to the parser. We can resolve this by looking ahead.
//
//            // Unfortunately, this is t3e easiest way to resolve the ambiguity
//            val slice = context.peekAll(6)
//
//            if (slice.count() >= 6) {
//                if (slice[0].type == TokenTypes.TypeIdentifier) {
//                    if (slice[1].type == TokenTypes.LParen) {
//                        if (slice[2].type == TokenTypes.Identifier) {
//                            // Potentially an instance method def
//                            if (slice[3].type == TokenTypes.TypeIdentifier) {
//                                if (slice[4].type == TokenTypes.RParen) {
//                                    if (slice[5].type == TokenTypes.Identifier) {
//                                        // This is an instance method def
//                                        val targetType = context.attempt(TypeIdentifierRule.Naked)
//                                            ?: TODO("??")
//
//                                        return@record +TypeAliasNode(
//                                            start,
//                                            targetType.lastToken,
//                                            TypeIdentifierNode(sourceToken, sourceToken, sourceToken.text),
//                                            targetType
//                                        )
//                                    }
//                                }
//                            }
//                        } else if (slice[2].type == TokenTypes.TypeIdentifier) {
//                            // Potentially a type method def
//                            if (slice[3].type == TokenTypes.RParen) {
//                                if (slice[4].type == TokenTypes.Identifier) {
//                                    // This is a type method def
//                                    val targetType = context.attempt(TypeIdentifierRule.Naked)
//                                        ?: TODO("??")
//
//                                    return@record +TypeAliasNode(
//                                        start,
//                                        targetType.lastToken,
//                                        TypeIdentifierNode(sourceToken, sourceToken, sourceToken.text),
//                                        targetType
//                                    )
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//
//            val targetType = context.attempt(TypeExpressionRule)
//                ?: TODO("???")
//
//            return@record +TypeAliasNode(
//                start, targetType.lastToken,
//                TypeIdentifierNode(sourceToken, sourceToken, sourceToken.text), targetType
//            )
        }
    }
}