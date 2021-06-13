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
				// TODO - For now, there's no reason to allow method defs at the top-level
				TokenTypes.Api -> ApiDefRule.execute(context).asSuccessOrNull<TopLevelDeclarationNode>()!!.node
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