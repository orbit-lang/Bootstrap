package org.orbit.util

import com.github.ajalt.clikt.core.CliktCommand
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module
import org.orbit.core.Path
import org.orbit.core.components.CompilationEventBus
import org.orbit.core.nodes.*
import org.orbit.core.phase.CompilerGenerator
import org.orbit.graph.components.*
import org.orbit.graph.pathresolvers.*
import org.orbit.graph.pathresolvers.util.ContextCompositionPathResolver
import org.orbit.graph.pathresolvers.util.ContextInstantiationPathResolver
import org.orbit.graph.pathresolvers.util.PathResolverUtil

val mainModule = module {
	single { Invocation(Unix) }
	single { CompilerGenerator(get()) }
	single { CompilationEventBus() }
	single { Printer(get<Invocation>().platform.getPrintableFactory()) }
	single {
		val util = PathResolverUtil()

		util.registerPathResolver(ContainerPathResolver(), ModuleNode::class.java)
		util.registerPathResolver(ContainerPathResolver(), ApiDefNode::class.java)
		util.registerPathResolver(AssignmentPathResolver(), AssignmentStatementNode::class.java)
		util.registerPathResolver(MethodDefPathResolver(), MethodDefNode::class.java)
		util.registerPathResolver(MethodSignaturePathResolver(), MethodSignatureNode::class.java)
		util.registerPathResolver(BlockPathResolver(), BlockNode::class.java)
		util.registerPathResolver(PropertyPairPathResolver(), PairNode::class.java)
		util.registerPathResolver(ConstructorPathResolver(), ConstructorInvocationNode::class.java)
		util.registerPathResolver(MethodCallPathResolver(), MethodCallNode::class.java)
		util.registerPathResolver(TypeIdentifierPathResolver(), TypeIdentifierNode::class.java)
		util.registerPathResolver(ExpressionPathResolver(), ExpressionNode::class.java)
		util.registerPathResolver(RValuePathResolver(), RValueNode::class.java)
		util.registerPathResolver(SymbolLiteralPathResolver, SymbolLiteralNode::class.java)
		util.registerPathResolver(IntLiteralPathResolver, IntLiteralNode::class.java)
		util.registerPathResolver(BinaryExpressionResolver(), BinaryExpressionNode::class.java)
		util.registerPathResolver(UnaryExpressionPathResolver(), UnaryExpressionNode::class.java)
		util.registerPathResolver(IdentifierExpressionPathResolver(), IdentifierNode::class.java)
		util.registerPathResolver(PrintPathResolver(), PrintNode::class.java)
		util.registerPathResolver(MetaTypePathResolver, MetaTypeNode::class.java)
		util.registerPathResolver(TypeProjectionPathResolver, ProjectionNode::class.java)
		util.registerPathResolver(TypeExpressionPathResolver, TypeExpressionNode::class.java)
		util.registerPathResolver(LambdaLiteralPathResolver, LambdaLiteralNode::class.java)
		util.registerPathResolver(ReferenceCallPathResolver, ReferenceCallNode::class.java)
		util.registerPathResolver(WhereClausePathResolver, WhereClauseNode::class.java)
		util.registerPathResolver(WhereClauseTypeBoundsExpressionResolver, WhereClauseTypeBoundsExpressionNode::class.java)
		util.registerPathResolver(TypeIndexResolver, TypeIndexNode::class.java)
		util.registerPathResolver(TypeOfPathResolver, TypeOfNode::class.java)
		util.registerPathResolver(ExpandPathResolver, ExpandNode::class.java)
		util.registerPathResolver(MirrorPathResolver, MirrorNode::class.java)
		util.registerPathResolver(ContextInstantiationPathResolver, ContextInstantiationNode::class.java)
		util.registerPathResolver(ContextCompositionPathResolver, ContextCompositionNode::class.java)
		util.registerPathResolver(WhereClauseByExpressionPathResolver, WhereClauseByExpressionNode::class.java)
		util.registerPathResolver(ParameterNodePathResolver, ParameterNode::class.java)
		util.registerPathResolver(MethodReferencePathResolver, MethodReferenceNode::class.java)

		util
	}

//	single<CodeGenFactory<SwiftHeader>>(CodeGeneratorQualifier.Swift) {
//		SwiftCodeGenFactory
//	}
//
//	single<CodeGenFactory<CHeader>>(CodeGeneratorQualifier.C) {
//		CCodeGenFactory
//	}
//
//	single<Mangler>(CodeGeneratorQualifier.C) {
//		CMangler
//	}
//
//	single<Mangler>(CodeGeneratorQualifier.Swift) {
//		SwiftMangler
//	}

	single<Gson> {
		GsonBuilder()
			.registerTypeAdapter(Binding.Kind::class.java, KindSerialiser)
			.registerTypeAdapter(Binding.Kind::class.java, KindDeserialiser)
			.registerTypeAdapter(Path::class.java, PathSerialiser)
			.registerTypeAdapter(Path::class.java, PathDeserialiser)
			.create()
	}

	single { NodeAnnotationMap() }
}

inline fun <reified T> getKoinInstance(): T {
	return object : KoinComponent {
		val value: T by inject()
	}.value
}

class Orbit : CliktCommand() {
	override fun run() {}
}