package org.orbit.frontend.rules

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.EntityConstructorNode
import org.orbit.core.nodes.TraitConstructorNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.frontend.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.util.Invocation

object TraitConstructorRule : ParseRule<EntityConstructorNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Trait)
        var next = context.peek()

        if (next.type != TokenTypes.Constructor) return ParseRule.Result.Failure.Rewind(listOf(start))

        context.consume()

        val traitIdentifier = context.attempt(TypeIdentifierRule.Naked)
            ?: throw invocation.make<Parser>("Expected type name identifier after `trait constructor`", context.peek())

        next = context.peek()

        if (next.type != TokenTypes.LAngle) {
            // TODO - Can trait constructors have case constructors?
            throw invocation.make<Parser>("Expected type parameter list after `trait constructor ${traitIdentifier.value}`", next)
        }

        context.consume()
        next = context.peek()

        val typeParameters = mutableListOf<TypeIdentifierNode>()
        while (next.type != TokenTypes.RAngle) {
            val typeParameter = context.attempt(TypeIdentifierRule.Naked)
                ?: throw invocation.make<Parser>("", context.peek())

            typeParameters.add(typeParameter)

            next = context.peek()

            if (next.type == TokenTypes.Comma) {
                context.consume()
                next = context.peek()
            }
        }

        var end = context.expect(TokenTypes.RAngle)

        next = context.peek()

        if (next.type != TokenTypes.LBrace) return +TraitConstructorNode(start, end, traitIdentifier, typeParameters)

        val signatureNodes = context.attempt(
            DelimitedRule(
                TokenTypes.LBrace,
                TokenTypes.RBrace,
                MethodSignatureRule(false)
            )
        )
            ?.nodes
            ?: throw invocation.make<Parser>("Only method signatures are allowed in the body of a trait or trait constructor definition", context.peek())

        end = signatureNodes.lastOrNull()?.lastToken ?: end

        return +TraitConstructorNode(start, end, traitIdentifier, typeParameters, signatureNodes)
    }
}