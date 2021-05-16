package org.orbit.frontend.rules

import org.orbit.core.nodes.ProgramNode
import org.orbit.core.nodes.TopLevelDeclarationNode
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus

object ProgramRule : ParseRule<ProgramNode> {
	override fun parse(context: Parser) : ParseRule.Result {
		val start = context.peek()
		var next = context.peek()
		val declarationNodes = mutableListOf<TopLevelDeclarationNode>()
		
		while (context.hasMore) {
			val decl: TopLevelDeclarationNode = when (next.type) {
				// TODO - Create a ParserUtil to avoid direct instantiations of ParseRules
				TokenTypes.LParen -> MethodDefRule.execute(context).asSuccessOrNull<TopLevelDeclarationNode>()!!.node
				TokenTypes.Observe -> ObserverRule.execute(context).asSuccessOrNull<TopLevelDeclarationNode>()!!.node
//				TokenTypes.Api -> ApiDefRule.execute(context)
				TokenTypes.Annotation, TokenTypes.Module -> ModuleRule.execute(context).asSuccessOrNull<TopLevelDeclarationNode>()!!.node
				else -> throw context.invocation.make<Parser>("Unexpected decl at program-level: ${next.type}", next.position)
			}

			declarationNodes.add(decl)

			if (!context.hasMore) break
			
			next = context.peek()
		}
		
		return +ProgramNode(start, next, declarationNodes)
	}
}