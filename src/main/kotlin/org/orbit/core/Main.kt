package org.orbit.core

import org.orbit.frontend.TokenTypes
import org.orbit.frontend.Lexer
import org.orbit.frontend.FileSourceProvider
import org.orbit.frontend.Parser
import org.orbit.frontend.rules.ProgramRule
import org.orbit.core.nodes.*
import org.orbit.graph.CanonicalNameResolver
import org.orbit.core.OrbitMangler
import org.orbit.frontend.CommentParser
import org.orbit.analysis.semantics.*
import org.orbit.analysis.*
import org.orbit.util.*

class Main {
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            if (args.isEmpty()) throw Exception("usage: orbit <source_files>")

            var sourceProvider: SourceProvider = FileSourceProvider(args[0])
			val commentParseResult = CommentParser.execute(sourceProvider)

			sourceProvider = commentParseResult.first
            
            val lexer = Lexer(TokenTypes)
            val parser = Parser(ProgramRule)

            val tokens = lexer.execute(sourceProvider)
            val result = parser.execute(tokens)

            result.warnings.forEach { println(it) }

			val environment = CanonicalNameResolver.execute(result.ast as ProgramNode)
			
            //print(ProgramNode.JsonSerialiser.serialise(resuelt.ast as ProgramNode).toString(4))
			
            val types = result.ast
            	.search(TypeDefNode::class.java)

			val traits = result.ast
				.search(TraitDefNode::class.java)
			
            val methods = result.ast
            	.search(MethodDefNode::class.java)

            val bounded = result.ast
            	.search(BoundedTypeParameterNode::class.java)

            val dependent = result.ast
            	.search(DependentTypeParameterNode::class.java)
            	//.mapNotNull {
            	//	val path = it.getAnnotationByKey<Path>("path")?.value ?: return
				//	OrbitMangler.mangle(path)
            	//}

            val typeIds = result.ast
            	.search(TypeIdentifierNode::class.java)

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
			
			val semanticAnalyser = Analyser("Semantics", NestedTraitAnalyser, UnreachableReturnAnalyser)
			val semanticAnalysisReport = semanticAnalyser.execute(result.ast)
						
			println(semanticAnalysisReport.toString(printer))
        }
    }
}