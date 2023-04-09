package org.orbit.frontend.rules

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.TypeParametersNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.util.Invocation

object TypeParametersRule : ParseRule<TypeParametersNode>, KoinComponent {
	private val invocation: Invocation by inject()

	override fun parse(context: Parser): ParseRule.Result {
		val delimitedRule = DelimitedRule(TokenTypes.LAngle, TokenTypes.RAngle, TypeIdentifierRule.Naked)
		val typeParameters = context.attempt(delimitedRule)
			?: throw invocation.make<Parser>("", context.peek())

		return +TypeParametersNode(typeParameters.firstToken, typeParameters.lastToken, typeParameters.nodes)
	}
}
