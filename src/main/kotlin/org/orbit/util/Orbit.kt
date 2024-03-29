package org.orbit.util

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.definition.BeanDefinition
import org.koin.core.parameter.DefinitionParameters
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools
import org.orbit.backend.codegen.ICodeGenerator
import org.orbit.backend.codegen.swift.*
import org.orbit.backend.codegen.utils.CodeGenUtil
import org.orbit.backend.codegen.utils.ICodeGenTarget
import org.orbit.backend.codegen.utils.IntrinsicCodeGenTarget
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.components.kinds.IKindInspector
import org.orbit.backend.typesystem.components.kinds.IntrinsicKindInspector
import org.orbit.backend.typesystem.inference.*
import org.orbit.backend.typesystem.utils.ArrowArrowUnifier
import org.orbit.backend.typesystem.utils.ITypeUnifier
import org.orbit.backend.typesystem.utils.TypeTypeUnifier
import org.orbit.core.nodes.*
import org.orbit.frontend.rules.CollectionTypeInference
import org.orbit.frontend.rules.TaggedTypeExpressionPathResolver
import org.orbit.graph.pathresolvers.*
import org.orbit.graph.pathresolvers.util.ContextCompositionPathResolver
import org.orbit.graph.pathresolvers.util.ContextInstantiationPathResolver
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import kotlin.reflect.KClass

val mainModule = module {
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
		util.registerPathResolver(StarPathResolver, StarNode::class.java)
		util.registerPathResolver(NeverPathResolver, NeverNode::class.java)
		util.registerPathResolver(AttributeArrowPathResolver, AttributeArrowNode::class.java)
		util.registerPathResolver(AttributeOperatorExpressionPathResolver, AttributeOperatorExpressionNode::class.java)
		util.registerPathResolver(AttributeInvocationPathResolver, AttributeInvocationNode::class.java)
		util.registerPathResolver(TypeLambdaConstraintPathResolver, TypeLambdaConstraintNode::class.java)
		util.registerPathResolver(CompoundAttributeExpressionPathResolver, CompoundAttributeExpressionNode::class.java)
		util.registerPathResolver(SumTypePathResolver, SumTypeNode::class.java)
		util.registerPathResolver(TaggedTypeExpressionPathResolver, TaggedTypeExpressionNode::class.java)
		util.registerPathResolver(RealLiteralPathResolver, RealLiteralNode::class.java)
		util.registerPathResolver(ProjectionEffectPathResolver, ProjectionEffectNode::class.java)
		util.registerPathResolver(TypeEffectInvocationPathResolver, TypeEffectInvocationNode::class.java)
		util.registerPathResolver(AttributeMetaTypeExpressionPathResolver, AttributeMetaTypeExpressionNode::class.java)
		util.registerPathResolver(EffectHandlerPathResolver, EffectHandlerNode::class.java)
		util.registerPathResolver(CausePathResolver, CauseNode::class.java)
		util.registerPathResolver(ForPathResolver, ForNode::class.java)
		util.registerPathResolver(IndexSlicePathResolver, IndexSliceNode::class.java)
		util.registerPathResolver(RangeSlicePathResolver, RangeSliceNode::class.java)
		util.registerPathResolver(TypeQueryPathResolver, TypeQueryExpressionNode::class.java)
		util.registerPathResolver(SelfPathResolver, SelfNode::class.java)
		util.registerPathResolver(EnumCaseReferencePathResolver, EnumCaseReferenceNode::class.java)
		util.registerPathResolver(EffectDeclarationPathResolver, EffectDeclarationNode::class.java)
		util.registerPathResolver(StringLiteralPathResolver, StringLiteralNode::class.java)

		util
	}

	single { NodeAnnotationMap() }

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
	single(SignatureInference)
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
	single(StarInference)
	single(NeverInference)
	single(AttributeDefInference)
	single(TypeLambdaConstraintInference)
	single(AttributeInvocationInference)
	single(AttributeOperatorExpressionInference)
	single(CompoundAttributeExpressionInference)
	single(SumTypeInference)
	single(TaggedTypeExpressionInference)
	single(TypedIdentifierBindingPatternInference)
	single(DiscardBindingPatternInference)
	single(RealLiteralInference)
	single(TypeEffectInference)
	single(TypeEffectInvocationInference)
	single(AttributeMetaTypeExpressionInference)
	single(EffectInference)
	single(CauseInference)
	single(ForInference)
	single(IndexSliceInference)
	single(RangeSliceInference)
	single(MirrorInference)
	single(TypeQueryInference)
	single(SelfInference)
	single(EnumCaseReferenceInference)
	single(EffectDeclarationInference)
	single(StringLiteralInference)

	// Code Gen
	single { CodeGenUtil(IntrinsicCodeGenTarget.Swift) }

	// Swift
	swiftSingle(ProgramGenerator)
	swiftSingle(ModuleGenerator)
	swiftSingle(TypeGenerator)
	swiftSingle(AlgebraicConstructorGenerator)
	swiftSingle(ContextGenerator)
	swiftSingle(TypeAliasGenerator)

	// Kind Inspectors
	single(IntrinsicKindInspector.TypeKindInspector)
	single(IntrinsicKindInspector.TraitKindInspector)
	single(IntrinsicKindInspector.IFunctionKindInspector.F0)
	single(IntrinsicKindInspector.IFunctionKindInspector.F1)
	single(IntrinsicKindInspector.IFunctionKindInspector.F2)
	single(IntrinsicKindInspector.IFunctionKindInspector.F3)
	single(IntrinsicKindInspector.HigherKindInspector)
	single(IntrinsicKindInspector.AliasKindInspector)
	single(IntrinsicKindInspector.TypeVariableKindInspector)
	single(IntrinsicKindInspector.AlwaysKindInspector)
	single(IntrinsicKindInspector.TupleKindInspector)
	single(IntrinsicKindInspector.StructKindInspector)
	single(IntrinsicKindInspector.ArrayKindInspector)
	single(IntrinsicKindInspector.UnionKindInspector)
	single(IntrinsicKindInspector.Level0Inspector)

	// Type Unifiers
	single(TypeTypeUnifier)
	single(ArrowArrowUnifier)

	// Pattern Matchers
	single(EnumPatternMatcher)
	single(TuplePatternMatcher)
}

private inline fun <reified T: AnyType> org.koin.core.module.Module.single(inspector: IKindInspector<T>) : BeanDefinition<IKindInspector<T>>
	= single(named("kind${T::class.java.simpleName}")) { inspector }

private inline fun <reified N: INode> org.koin.core.module.Module.single(target: ICodeGenTarget, generator: ICodeGenerator<N>) : BeanDefinition<ICodeGenerator<N>>
	= single(named("codeGen${target.getTargetName()}${N::class.java.simpleName}")) { generator }

private inline fun <reified N: INode> org.koin.core.module.Module.swiftSingle(generator: ICodeGenerator<N>) : BeanDefinition<ICodeGenerator<N>>
	= single(IntrinsicCodeGenTarget.Swift, generator)

private inline fun <reified N: INode, reified E: ITypeEnvironment> org.koin.core.module.Module.single(inference: ITypeInference<N, E>) : BeanDefinition<ITypeInference<N, E>>
	= single(named("infer${N::class.java.simpleName}")) { inference }

private inline fun <reified N: INode, reified E: ITypeEnvironment> org.koin.core.module.Module.inferenceFactory(crossinline generator: (DefinitionParameters) -> ITypeInference<N, E>) : BeanDefinition<ITypeInference<N, E>>
	= factory(named("infer${N::class.java.simpleName}")) { params -> generator(params) }

inline fun <reified T> getKoinInstance(qualifier: Qualifier? = null): T {
	return object : KoinComponent {
		val value: T by inject(qualifier)
	}.value
}

inline fun <reified A: AnyType, reified B: AnyType>  org.koin.core.module.Module.single(unifier: ITypeUnifier<A, B>) : BeanDefinition<ITypeUnifier<A, B>>
	= single(named("unify${A::class.java.simpleName}_${B::class.java.simpleName}")) { unifier }

inline fun <reified P: AnyType>  org.koin.core.module.Module.single(patternMatcher: IPatternMatcher<P>) : BeanDefinition<IPatternMatcher<P>>
	= single(named("match${P::class.java.simpleName}")) { patternMatcher }

fun <T: Any> getKoinInstance(clazz: KClass<T>) : T
	= KoinPlatformTools.defaultContext().get().get(clazz)

inline fun <reified T: Any> getKoinInstance(named: String) : T
	= KoinPlatformTools.defaultContext().get().get(named(named))

class Orbit : CliktCommand() {
	private val measure by option("-m", "--measure" ,help = "Print time take by each phase, and total time")
		.flag(default = false)

	override fun run() {
		startKoin {
			modules(mainModule, module {
				single { Invocation(Unix, InvocationOptions(measure, measure)) }
				single { Printer(get<Invocation>().platform.getPrintableFactory()) }
			})
		}
	}
}