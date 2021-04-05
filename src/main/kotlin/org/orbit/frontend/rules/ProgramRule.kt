package org.orbit.frontend.rules

import org.orbit.frontend.ParseRule
import org.orbit.frontend.Parser
import org.orbit.core.nodes.*
import org.orbit.frontend.rules.*
import org.orbit.frontend.TokenTypes

object ProgramRule : ParseRule<ProgramNode> {
	override fun parse(context: Parser) : ProgramNode {
		val start = context.peek()
		var next = context.peek()
		val declarationNodes = mutableListOf<TopLevelDeclarationNode>()
		
		while (context.hasMore) {
			val decl: TopLevelDeclarationNode = when (next.type) {
				TokenTypes.LParen -> MethodDefRule.execute(context)
				TokenTypes.Observe -> ObserverRule.execute(context)
//				TokenTypes.Api -> ApiDefRule.execute(context)
				TokenTypes.Annotation, TokenTypes.Module -> ModuleRule.execute(context)
				else -> throw context.invocation.make<Parser>("Unexpected decl at program-level: ${next.type}", next.position)
			}

			declarationNodes.add(decl)

			if (!context.hasMore) break
			
			next = context.peek()
		}
		
		return ProgramNode(start, next, declarationNodes)
	}
}