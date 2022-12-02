package org.orbit.util

import com.github.ajalt.clikt.core.CliktCommand
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.definition.BeanDefinition
import org.koin.core.parameter.DefinitionParameters
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools
import org.orbit.backend.codegen.ICodeGenerator
import org.orbit.backend.codegen.swift.*
import org.orbit.backend.codegen.utils.CodeGenUtil
import org.orbit.backend.codegen.utils.ICodeGenTarget
import org.orbit.backend.codegen.utils.IntrinsicCodeGenTarget
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.inference.*
import org.orbit.core.components.CompilationEventBus
import org.orbit.core.nodes.*
import org.orbit.core.phase.CompilerGenerator
import org.orbit.frontend.rules.CollectionTypeInference
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
		util.registerPathResolver(TupleTypePathResolver, TupleTypeNode::class.java)
		util.registerPathResolver(StructTypePathResolver, StructTypeNode::class.java)
		util.registerPathResolver(ContextOfPathResolver, ContextOfNode::class.java)
		util.registerPathResolver(CheckPathResolver, CheckNode::class.java)
		util.registerPathResolver(ProjectedPropertyAssignmentPathResolver, ProjectedPropertyAssignmentNode::class.java)
		util.registerPathResolver(UnitPathResolver, UnitNode::class.java)
		util.registerPathResolver(CollectionLiteralPathResolver, CollectionLiteralNode::class.java)
		util.registerPathResolver(CollectionTypePathResolver, CollectionTypeNode::class.java)
		util.registerPathResolver(InvocationPathResolver, InvocationNode::class.java)
		util.registerPathResolver(RefOfPathResolver, RefOfNode::class.java)
		util.registerPathResolver(LambdaTypePathResolver, LambdaTypeNode::class.java)
		util.registerPathResolver(TypeLambdaPathResolver, TypeLambdaNode::class.java)
		util.registerPathResolver(TypeLambdaInvocationPathResolver, TypeLambdaInvocationNode::class.java)

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

	single { NodeAnnotationMap() }
	single { ImportManager(emptyList()) }

	// Type Inference
	single(AlgebraicConstructorInference)
	single(AnonymousParameterInference)
	single(AssignmentInference)
	single(BinaryExpressionInference)
	single(BlockInference)
	single(BoolLiteralInference)
	single(CaseInference)
	single(ConstructorInvocationInference)
	single(ContextCompositionInference)
	single(ContextInference)
	single(ContextInstantiationInference)
	single(ContextOfInference)
	single(ElseInference)
	single(ExtensionInference)
	single(IdentifierInference)
	single(IntLiteralInference)
	single(LambdaLiteralInference)
	single(MethodCallInference)
	single(MethodDefInference)
	single(MethodDelegateInference)
	single(MethodReferenceInference)
	single(ModuleInference)
	single(OperatorDefInference)
	single(PairInference)
	single(PanicInference)
	single(ParameterInference)
	single(ProgramInference)
	single(ProjectionInference)
	single(ReturnStatementInference)
	single(RValueInference)
	single(SelectInference)
	inferenceFactory { shouldDeclare -> SignatureInference(shouldDeclare.get()) }
	single(StructTypeInference)
	single(StructuralPatternInference)
	single(TraitDefInference)
	single(TupleLiteralInference)
	single(TupleTypeInference)
	single(TypeAliasInference)
	single(TypeBindingPatternInference)
	single(TypeDefInference)
	single(TypeIdentifierInference)
	single(TypeOfInference)
	single(UnaryExpressionInference)
	single(WhereClauseInference)
	single(WhereClauseTypeBoundsExpressionInference)
	single(CheckInference)
	single(ExpandInference)
	single(IdentifierBindingPatternInference)
	single(ProjectedPropertyAssignmentInference)
	single(UnitInference)
	single(CollectionLiteralInference)
	single(CollectionTypeInference)
	single(DeferInference)
	single(InvocationInference)
	single(RefOfInference)
	single(LambdaTypeInference)
	single(TypeLambdaInference)
	single(TypeLambdaInvocationInference)

	// Code Gen
	single { CodeGenUtil(IntrinsicCodeGenTarget.Swift) }

	// Swift
	swiftSingle(ProgramGenerator)
	swiftSingle(ModuleGenerator)
	swiftSingle(TypeGenerator)
	swiftSingle(AlgebraicConstructorGenerator)
	swiftSingle(ContextGenerator)
	swiftSingle(TypeAliasGenerator)
}

private inline fun <reified N: INode> org.koin.core.module.Module.single(target: ICodeGenTarget, generator: ICodeGenerator<N>) : BeanDefinition<ICodeGenerator<N>>
	= single(named("codeGen${target.getTargetName()}${N::class.java.simpleName}")) { generator }

private inline fun <reified N: INode> org.koin.core.module.Module.swiftSingle(generator: ICodeGenerator<N>) : BeanDefinition<ICodeGenerator<N>>
	= single(IntrinsicCodeGenTarget.Swift, generator)

private inline fun <reified N: INode, reified E: ITypeEnvironment> org.koin.core.module.Module.single(inference: ITypeInference<N, E>) : BeanDefinition<ITypeInference<N, E>>
	= single(named("infer${N::class.java.simpleName}")) { inference }

private inline fun <reified N: INode, reified E: ITypeEnvironment> org.koin.core.module.Module.inferenceFactory(crossinline generator: (DefinitionParameters) -> ITypeInference<N, E>) : BeanDefinition<ITypeInference<N, E>>
	= factory(named("infer${N::class.java.simpleName}")) { params -> generator(params) }

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