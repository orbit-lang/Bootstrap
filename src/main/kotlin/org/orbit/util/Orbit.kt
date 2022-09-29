package org.orbit.util

import com.github.ajalt.clikt.core.CliktCommand
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.definition.BeanDefinition
import org.koin.core.parameter.DefinitionParameters
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools
import org.orbit.backend.typesystem.inference.*
import org.orbit.backend.typesystem.inference.evidence.*
import org.orbit.core.Path
import org.orbit.core.components.CompilationEventBus
import org.orbit.core.nodes.*
import org.orbit.core.phase.CompilerGenerator
import org.orbit.graph.components.*
import org.orbit.graph.pathresolvers.*
import org.orbit.graph.pathresolvers.util.ContextCompositionPathResolver
import org.orbit.graph.pathresolvers.util.ContextInstantiationPathResolver
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import kotlin.reflect.KClass

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
		util.registerPathResolver(ExpressionPathResolver(), IExpressionNode::class.java)
		util.registerPathResolver(RValuePathResolver(), RValueNode::class.java)
		util.registerPathResolver(SymbolLiteralPathResolver, SymbolLiteralNode::class.java)
		util.registerPathResolver(IntLiteralPathResolver, IntLiteralNode::class.java)
		util.registerPathResolver(BinaryExpressionResolver(), BinaryExpressionNode::class.java)
		util.registerPathResolver(UnaryExpressionPathResolver(), UnaryExpressionNode::class.java)
		util.registerPathResolver(IdentifierExpressionPathResolver(), IdentifierNode::class.java)
		util.registerPathResolver(PrintPathResolver(), PrintNode::class.java)
		util.registerPathResolver(MetaTypePathResolver, MetaTypeNode::class.java)
		util.registerPathResolver(ProjectionPathResolver, ProjectionNode::class.java)
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
		util.registerPathResolver(MethodDelegatePathResolver, MethodDelegateNode::class.java)
		util.registerPathResolver(AnonymousParameterPathResolver, AnonymousParameterNode::class.java)
		util.registerPathResolver(SelectPathResolver, SelectNode::class.java)
		util.registerPathResolver(CasePathResolver, CaseNode::class.java)
		util.registerPathResolver(BoolLiteralPathResolver, BoolLiteralNode::class.java)
		util.registerPathResolver(AnyPathResolver(), ElseNode::class.java)
		util.registerPathResolver(PanicPathResolver, PanicNode::class.java)
		util.registerPathResolver(StructuralPatternPathResolver, StructuralPatternNode::class.java)
		util.registerPathResolver(TypedIdentifierBindingPathResolver, TypedIdentifierBindingPatternNode::class.java)
		util.registerPathResolver(TypeBindingPatternPathResolver, TypeBindingPatternNode::class.java)
		util.registerPathResolver(DiscardBindingPatternPathResolver, DiscardBindingPatternNode::class.java)
		util.registerPathResolver(AnyPathResolver(), IdentifierBindingPatternNode::class.java)
		util.registerPathResolver(AlgebraicConstructorPathResolver, AlgebraicConstructorNode::class.java)
		util.registerPathResolver(TupleLiteralPathResolver, TupleLiteralNode::class.java)

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
	single { ImportManager(emptyList()) }

	// Type Inference
	single(ProgramInference)
	single(ModuleInference)
	single(TypeDefInference)
	single(ContextInference)
	single(AlgebraicConstructorInference)
	single(TypeIdentifierInference)
	inferenceFactory { shouldDeclare -> SignatureInference(shouldDeclare.get()) }
	single(MethodDefInference)
	single(BlockInference)
	single(ReturnStatementInference)
	single(RValueInference)
	single(IdentifierInference)
	single(ConstructorInvocationInference)
	single(BinaryExpressionInference)
	single(SelectInference)
	single(IntLiteralInference)
	single(CaseInference)
	single(ElseInference)
	single(StructuralPatternInference)
	single(TypeBindingPatternInference)
	single(MethodCallInference)
	single(BoolLiteralInference)
	single(OperatorDefInference)
	single(MethodReferenceInference)
	single(ProjectionInference)
	single(PairInference)
	single(TraitDefInference)
	single(AssignmentInference)
	single(MethodDelegateInference)
	single(AnonymousParameterInference)
	single(TupleLiteralInference)
	single(ExtensionInference)
	single(ContextInstantiationInference)
	single(TypeOfInference)
	single(PanicInference)
	single(TypeAliasInference)

	// Contextual Evidence Gathering
	single(TypeIdentifierEvidenceProvider)
	single(SignatureEvidenceProvider)
	single(MethodDefEvidenceProvider)
	single(ProjectionEvidenceProvider)
}

private inline fun <reified N: INode> org.koin.core.module.Module.single(inference: ITypeInference<N>) : BeanDefinition<ITypeInference<N>>
	= single(named("infer${N::class.java.simpleName}")) { inference }

private inline fun <reified N: INode> org.koin.core.module.Module.inferenceFactory(crossinline generator: (DefinitionParameters) -> ITypeInference<N>) : BeanDefinition<ITypeInference<N>>
	= factory(named("infer${N::class.java.simpleName}")) { params -> generator(params) }

private inline fun <reified N: INode> org.koin.core.module.Module.single(inference: IContextualEvidenceProvider<N>) : BeanDefinition<IContextualEvidenceProvider<N>>
	= single(named("evidence${N::class.java.simpleName}")) { inference }

inline fun <reified T> getKoinInstance(): T {
	return object : KoinComponent {
		val value: T by inject()
	}.value
}

fun <T: Any> getKoinInstance(clazz: KClass<T>) : T
	= KoinPlatformTools.defaultContext().get().get(clazz)

inline fun <reified T: Any> getKoinInstance(named: String) : T
	= KoinPlatformTools.defaultContext().get().get(named(named))

class Orbit : CliktCommand() {
	override fun run() {}
}