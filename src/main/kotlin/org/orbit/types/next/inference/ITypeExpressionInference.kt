package org.orbit.types.next.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.*
import org.orbit.types.next.components.*
import org.orbit.types.next.intrinsics.Native
import org.orbit.types.next.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.Printer
import org.orbit.util.next.find

object AnyTypeExpressionInferenceContext : InferenceContext {
    override val nodeType: Class<out Node> = TypeExpressionNode::class.java

    override fun <N : Node> clone(clazz: Class<N>): InferenceContext = this
}

interface ITypeExpressionInference<N: TypeExpressionNode, T: TypeComponent> : Inference<N, T>

object AnyTypeExpressionInference : ITypeExpressionInference<TypeExpressionNode, TypeComponent> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: TypeExpressionNode): InferenceResult = when (node) {
        is TypeIdentifierNode -> TypeLiteralInference.infer(inferenceUtil, context, node)
        is MetaTypeNode -> MetaTypeInference.infer(inferenceUtil, context, node)
        is TypeIndexNode -> TypeIndexInference.infer(inferenceUtil, context, node)
        is MirrorNode -> MirrorInference.infer(inferenceUtil, context, node)
        else -> InferenceResult.Failure(Never("Failed to infer Type Expression: ${node::class.java.simpleName}"))
    }
}

object MirrorInferenceContext : InferenceContext {
    override val nodeType: Class<out Node> = TypeIdentifierNode::class.java

    override fun <N : Node> clone(clazz: Class<N>): InferenceContext = this
}

object TypeLiteralInference : ITypeExpressionInference<TypeIdentifierNode, TypeComponent>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: TypeIdentifierNode): InferenceResult = when (context) {
        is MirrorInferenceContext -> {
            val type = inferenceUtil.find<TypeSystem>(node.getPath(), invocation, node)

            Native.Types.Mirror(type)
                .type.inferenceResult()
        }

        else -> inferenceUtil.find<TypeSystem>(node.getPath(), invocation, node).inferenceResult()
    }
}

object MetaTypeInference : ITypeExpressionInference<MetaTypeNode, MonomorphicType<*>>, KoinComponent {
    private val printer: Printer by inject()

    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: MetaTypeNode): InferenceResult {
        val polyType = inferenceUtil.inferAs<TypeIdentifierNode, PolymorphicType<TypeComponent>>(node.typeConstructorIdentifier)
        val parameters = inferenceUtil.inferAllAs<TypeExpressionNode, TypeComponent>(node.typeParameters, AnyInferenceContext(TypeExpressionNode::class.java))
            .mapIndexed { idx, type -> Pair(idx, type) }

        return when (polyType.baseType) {
            is Type -> TypeMonomorphiser.monomorphise(inferenceUtil.toCtx(), polyType as PolymorphicType<FieldAwareType>, parameters, MonomorphisationContext.Any)
                .toInferenceResult(printer)

            is Trait -> TraitMonomorphiser.monomorphise(inferenceUtil.toCtx(), polyType as PolymorphicType<ITrait>, parameters, MonomorphisationContext.TraitConformance(inferenceUtil.self))
                .toInferenceResult(printer)

            is TypeFamily<*> -> FamilyMonomorphiser.monomorphise(inferenceUtil.toCtx(), polyType as PolymorphicType<TypeFamily<*>>, parameters, MonomorphisationContext.Any)
                .toInferenceResult(printer)

            else -> InferenceResult.Failure(Never("Cannot specialise Polymorphic Type ${polyType.toString(printer)}"))
        }
    }
}

object TypeIndexInference : Inference<TypeIndexNode, TypeComponent>, KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: TypeIndexNode): InferenceResult {
        val self = inferenceUtil.self ?: throw invocation.make<TypeSystem>("Cannot infer Self in this context", node)
        val idx = AbstractTypeParameter(self.getPath(OrbitMangler) + node.index.value)

        return when (self) {
            is ParameterisedType -> when (self.contains(idx)) {
                true -> InferenceResult.Success(idx)
                else -> InferenceResult.Failure(Never("Contextual Self type ${self.toString(printer)} does not declare a Type Parameter ${idx.toString(printer)}", node.index.firstToken.position))
            }

            else -> {
                val conformance = inferenceUtil.getConformance(self)
                val parameterised = conformance.filterIsInstance<ParameterisedType>()
                val relativeIdx = AbstractTypeParameter(node.index.value)
                val matches = parameterised.mapNotNull { when (it.indexOfRelative(relativeIdx)) {
                    -1 -> null
                    else -> it.typeOfRelative(relativeIdx)
                }}

                if (matches.count() == 1) return InferenceResult.Success(matches[0])

                InferenceResult.Failure(Never("Type ${self.toString(printer)} cannot be indexed by Parameter ${relativeIdx.toString(printer)}", node.index.firstToken.position))
            }
        }
    }
}

object TypeSynthesisInference : Inference<TypeSynthesisNode, ITrait>, KoinComponent  {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: TypeSynthesisNode): InferenceResult {
        if (node.kind != IntrinsicKinds.Trait) throw invocation.compilerError<TypeSystem>("Only Trait synthesis is currently supported, found ${node.kind.keyword.identifier}", node)

        val target = inferenceUtil.infer(node.targetNode)

        return when (target) {
            is IType -> target.deriveTrait(inferenceUtil.toCtx())
            else -> Never("Cannot synthesise a Trait from ${target.toString(printer)} (Kind: ${target.kind.toString(printer)}), only Types")
        }.inferenceResult()
    }
}
