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
import org.orbit.types.next.inference.*
import org.orbit.util.next.BindingScope
import org.orbit.util.next.TypeMap

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
		util.registerPathResolver(ConstructorPathResolver(), ConstructorNode::class.java)
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
		util.registerPathResolver(CollectionLiteralPathResolver, CollectionLiteralNode::class.java)
		util.registerPathResolver(LambdaLiteralPathResolver, LambdaLiteralNode::class.java)
		util.registerPathResolver(ReferenceCallPathResolver, ReferenceCallNode::class.java)
		util.registerPathResolver(WhereClausePathResolver, WhereClauseNode::class.java)
		util.registerPathResolver(WhereClauseTypeBoundsExpressionResolver, WhereClauseTypeBoundsExpressionNode::class.java)
		util.registerPathResolver(TypeIndexResolver, TypeIndexNode::class.java)
		util.registerPathResolver(TypeSynthesisPathResolver, TypeSynthesisNode::class.java)
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

	single {
		val util = InferenceUtil(TypeMap(), BindingScope.Root)

		util.registerInference(IntLiteralInference, IntLiteralNode::class.java)
		util.registerInference(SymbolLiteralInference, SymbolLiteralNode::class.java)
		util.registerInference(LambdaLiteralInference, LambdaLiteralNode::class.java)
		util.registerInference(VariableInference, IdentifierNode::class.java)
		util.registerInference(TypeLiteralInference, TypeIdentifierNode::class.java)
		util.registerInference(BlockInference, BlockNode::class.java)
		util.registerInference(TypeDefInference, TypeDefNode::class.java)
		util.registerInference(ModuleInference, ModuleNode::class.java)
		util.registerInference(FieldInference, ParameterNode::class.java)
		util.registerInference(TypeParameterInference, TypeLiteralInferenceContext.TypeParameterContext)
		util.registerInference(MetaTypeInference, MetaTypeNode::class.java)
		util.registerInference(TypeIndexInference, TypeIndexNode::class.java)
		util.registerInference(AnyTypeExpressionInference, TypeExpressionNode::class.java)
		util.registerInference(AnyExpressionInference, AnyExpressionContext)
		util.registerInference(WhereClauseInference, WhereClauseNode::class.java)
		util.registerInference(WhereAssignmentInference, WhereClauseExpressionInferenceContext.AssignmentContext)
		util.registerInference(RValueInference, RValueNode::class.java)
		util.registerInference(ConstructorInference, ConstructorNode::class.java)
		util.registerInference(TypeSynthesisInference, TypeSynthesisNode::class.java)
		util.registerInference(WhereConformanceInference, TypeConstraintWhereClauseNode::class.java)
		util.registerInference(TraitConformanceConstraintInference, TraitConformanceTypeConstraintNode::class.java)
		util.registerInference(TypeConstraintInference, TypeConstraintNode::class.java)
		util.registerInference(SignatureInference, MethodSignatureNode::class.java)
		util.registerInference(ReturnStatementInference, ReturnStatementNode::class.java)
		util.registerInference(MethodCallInference, MethodCallNode::class.java)
		util.registerInference(ReferenceCallInference, ReferenceCallNode::class.java)
		util.registerInference(AssignmentStatementInference, AssignmentStatementNode::class.java)
//		util.registerInference(TypeBoundsExpressionInference, WhereClauseExpressionInferenceContext.TypeBoundsContext)
		util.registerInference(TypeOfInference, TypeOfNode::class.java)
		util.registerInference(DeferInference, DeferNode::class.java)
		util.registerInference(ExpandInference, ExpandNode::class.java)
		util.registerInference(MirrorInference, MirrorNode::class.java)
		util.registerInference(TypeBoundsConstraintInference, WhereClauseTypeBoundsExpressionNode::class.java)
		util.registerInference(ContextInstantiationInference, ContextInstantiationNode::class.java)
		util.registerInference(ContextCompositionInference, ContextCompositionNode::class.java)
		util.registerInference(ContextInference, ContextExpressionNode::class.java)
		util.registerInference(PrintNodeInference, PrintNode::class.java)
		util.registerInference(WhereClauseByExpressionInference, WhereClauseByExpressionNode::class.java)
		util.registerInference(MethodReferenceInference, MethodReferenceNode::class.java)
		util.registerInference(BinaryExpressionInference, BinaryExpressionNode::class.java)
		util.registerInference(UnaryExpressionInference, UnaryExpressionNode::class.java)

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