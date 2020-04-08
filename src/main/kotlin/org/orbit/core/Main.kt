package org.orbit.core

import org.orbit.frontend.TokenTypes
import org.orbit.frontend.Lexer
import org.orbit.frontend.FileSourceProvider
import org.orbit.frontend.Parser
import org.orbit.frontend.ParseError
import org.orbit.frontend.rules.ProgramRule
import org.orbit.core.nodes.*
import org.orbit.graph.CanonicalNameResolver
import org.orbit.core.OrbitMangler
import org.orbit.frontend.CommentParser
import org.orbit.analysis.semantics.*
import org.orbit.analysis.*
import org.orbit.analysis.types.IntLiteralAnalyser
import org.orbit.util.*

class Main {
    companion object {
        @JvmStatic fun main(args: Array<String>) {
        	try {
	            val orbit = Orbit()

	            orbit.main(args)

				// TODO - Platform should be derived from System.getProperty("os.name") or similar
				// TODO - Support non *nix platforms
				val invocation = Invocation(Unix)

	            var sourceProvider: SourceProvider = FileSourceProvider(orbit.source)
				val commentParser = CommentParser(invocation)
				val commentParseResult = commentParser.execute(sourceProvider)

				sourceProvider = commentParseResult.sourceProvider

	            val lexer = Lexer(invocation, TokenTypes)
	            val parser = Parser(invocation, ProgramRule)
				val canonicalNameResolver = CanonicalNameResolver(invocation)

//				val linker = PhaseLinker(invocation,
//					commentParser,
//					Lexer.AdapterPhase(invocation) as ReifiedPhase<Any, Any>,
//					lexer as ReifiedPhase<Any, Any>,
//					Parser.AdapterPhase(invocation) as ReifiedPhase<Any, Any>,
//					finalPhase = parser)
//
//				val result = linker.execute(sourceProvider)

	            val lexerResult = lexer.execute(sourceProvider)
	            val result = parser.execute(Parser.InputType(lexerResult.tokens))

	            result.warnings.forEach { println(it) }

				val environment = CanonicalNameResolver(invocation).execute(result.ast as ProgramNode)

	            //print(ProgramNode.JsonSerialiser.serialise(resuelt.ast as ProgramNode).toString(4))

	            //val types = result.ast
	            //	.search(TypeDefNode::class.java)

				//val traits = result.ast
				//	.search(TraitDefNode::class.java)

	            //val methods = result.ast
	            //	.search(MethodDefNode::class.java)

	            //val bounded = result.ast
	            //	.search(BoundedTypeParameterNode::class.java)

	            //val dependent = result.ast
	            //	.search(DependentTypeParameterNode::class.java)
	            	//.mapNotNull {
	            	//	val path = it.getAnnotationByKey<Path>("path")?.value ?: return
					//	OrbitMangler.mangle(path)
	            	//}

	            //val typeIds = result.ast
	            //	.search(TypeIdentifierNode::class.java)

				//val exprs = result.ast
				//	.search(LiteralNode::class.java)

				//exprs.forEach { println(it) }
				//println("TYPES: ${types.size}")
				//println("TRAITS: ${traits.size}")
				//println("METHODS: ${methods.size}")

				//types.forEach { println(it) }
				//traits.forEach { println(it) }
				//methods.forEach { println(it) }
				//bounded.forEach { println(it) }
				//dependent.forEach { println(it) }
				//typeIds.forEach { println(it) }

				val printer = Printer(Unix)

				val semanticAnalyser = Analyser(invocation, "Semantics",
					NestedTraitAnalyser(invocation),
					UnreachableReturnAnalyser(invocation),
					RedundantReturnAnalyser(invocation))

				val semanticAnalysisReport = semanticAnalyser.execute(result.ast)

				println(semanticAnalysisReport.toString(printer))

				val typeAnalyser = Analyser(invocation, "Types",
					IntLiteralAnalyser(invocation)
				)

				val typeAnalysisReport = typeAnalyser.execute(result.ast)

				println(typeAnalysisReport.toString(printer))
			} catch (ex: Exception) {
				println(ex.message)
				throw ex
			}
        }
    }
}