package org.orbit.util

import com.github.ajalt.clikt.core.CliktCommand
import org.koin.dsl.module
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.c.units.CCodeGenFactory
import org.orbit.backend.codegen.c.units.CHeader
import org.orbit.backend.codegen.c.units.CMangler
import org.orbit.backend.codegen.swift.units.SwiftCodeGenFactory
import org.orbit.backend.codegen.swift.units.SwiftHeader
import org.orbit.backend.codegen.swift.units.SwiftMangler
import org.orbit.core.CodeGeneratorQualifier
import org.orbit.core.Mangler
import org.orbit.core.components.CompilationEventBus
import org.orbit.core.nodes.*
import org.orbit.core.phase.CompilerGenerator
import org.orbit.core.single
import org.orbit.graph.pathresolvers.*
import org.orbit.graph.pathresolvers.util.PathResolverUtil

val mainModule = module {
	single { Invocation(Unix) }
	single { CompilerGenerator(get()) }
	single { CompilationEventBus() }
	single { Printer(get<Invocation>().platform.getPrintableFactory()) }
	single {
		val util = PathResolverUtil()

		util.registerPathResolver(ContainerResolver(), ModuleNode::class.java)
		util.registerPathResolver(ContainerResolver(), ApiDefNode::class.java)
		util.registerPathResolver(AssignmentPathResolver(), AssignmentStatementNode::class.java)
		util.registerPathResolver(MethodDefPathResolver(), MethodDefNode::class.java)
		util.registerPathResolver(MethodSignaturePathResolver(), MethodSignatureNode::class.java)
		util.registerPathResolver(BlockPathResolver(), BlockNode::class.java)
		util.registerPathResolver(PropertyPairPathResolver(), PairNode::class.java)
		util.registerPathResolver(ConstructorPathResolver(), ConstructorNode::class.java)
		util.registerPathResolver(CallPathResolver(), CallNode::class.java)
		util.registerPathResolver(TypeIdentifierPathResolver(), TypeIdentifierNode::class.java)
		util.registerPathResolver(ExpressionPathResolver(), ExpressionNode::class.java)
		util.registerPathResolver(RValuePathResolver(), RValueNode::class.java)
		util.registerPathResolver(SymbolLiteralPathResolver, SymbolLiteralNode::class.java)
		util.registerPathResolver(IntLiteralPathResolver, IntLiteralNode::class.java)
		util.registerPathResolver(BinaryExpressionResolver(), BinaryExpressionNode::class.java)
		util.registerPathResolver(UnaryExpressionResolver(), UnaryExpressionNode::class.java)
		util.registerPathResolver(IdentifierExpressionPathResolver(), IdentifierNode::class.java)
		util.registerPathResolver(PrintPathResolver(), PrintNode::class.java)
		util.registerPathResolver(MetaTypePathResolver, MetaTypeNode::class.java)
		util.registerPathResolver(TypeProjectionPathResolver, TypeProjectionNode::class.java)
		util.registerPathResolver(TypeExpressionPathResolver, TypeExpressionNode::class.java)
		util.registerPathResolver(CollectionLiteralPathResolver, CollectionLiteralNode::class.java)

		util
	}

	single<CodeGenFactory<SwiftHeader>>(CodeGeneratorQualifier.Swift) {
		SwiftCodeGenFactory
	}

	single<CodeGenFactory<CHeader>>(CodeGeneratorQualifier.C) {
		CCodeGenFactory
	}

	single<Mangler>(CodeGeneratorQualifier.C) {
		CMangler
	}

	single<Mangler>(CodeGeneratorQualifier.Swift) {
		SwiftMangler
	}

	factory { ASTUtil() }
}

class Orbit : CliktCommand() {
	override fun run() {}
}