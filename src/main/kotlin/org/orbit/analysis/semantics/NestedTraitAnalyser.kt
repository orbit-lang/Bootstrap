package org.orbit.analysis.semantics

import org.orbit.analysis.NodeAnalyser
import org.orbit.analysis.Analysis
import org.orbit.core.nodes.*

/**
	Parser allows TraitDefs within TypeDefs to simplify TypeDefRule.
	However, this is has no meaning in Orbit's semantics, so we find
	those cases now and report error.
*/
object NestedTraitAnalyser : NodeAnalyser<TypeDefNode>(TypeDefNode::class.java) {
	override fun analyse(node: TypeDefNode) : List<Analysis> {
		return node.body.body.mapNotNull {
			val clazz = NestedTraitAnalyser::class.java.simpleName
		
			when (it) {
				is TraitDefNode -> Analysis(clazz, Analysis.Level.Error,
					"Types cannot declare nested Traits", it.firstToken, it.lastToken)
				
				else -> null
			}
		}
	}
}