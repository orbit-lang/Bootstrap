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
			
            //print(ProgramNode.JsonSerialiser.serialise(result.ast as ProgramNode).toString(4))
			
            val types = result.ast
            	.search(TypeDefNode::class.java)

			val traits = result.ast
				.search(TraitDefNode::class.java)
			
            val methods = result.ast
            	.search(MethodSignatureNode::class.java)
            	//.mapNotNull {
            	//	val path = it.getAnnotationByKey<Path>("path")?.value ?: return
				//	OrbitMangler.mangle(path)
            	//}

			println("TYPES: ${types.size}")
			println("TRAITS: ${traits.size}")
			println("METHODS: ${methods.size}")
			
			types.forEach { println(it) }
			traits.forEach { println(it) }
			methods.forEach { println(it) }
			
			print(environment)
        }
    }
}