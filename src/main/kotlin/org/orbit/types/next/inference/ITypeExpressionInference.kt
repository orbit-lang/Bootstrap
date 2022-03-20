package org.orbit.types.next.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.getPath
import org.orbit.core.nodes.MetaTypeNode
import org.orbit.core.nodes.TypeExpressionNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.types.next.components.*
import org.orbit.types.next.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.Printer
import org.orbit.util.next.find

interface ITypeExpressionInference<N: TypeExpressionNode, T: TypeComponent> : Inference<N, T>

object AnyTypeExpressionInference : ITypeExpressionInference<TypeExpressionNode, TypeComponent> {
    override fun infer(inferenceUtil: InferenceUtil, node: TypeExpressionNode): InferenceResult = when (node) {
        is TypeIdentifierNode -> TypeLiteralInference.infer(inferenceUtil, node)
        is MetaTypeNode -> MetaTypeInference.infer(inferenceUtil, node)
        else -> InferenceResult.Failure(Never("Failed to infer Type Expression: ${node::class.java.simpleName}"))
    }
}

object TypeLiteralInference : ITypeExpressionInference<TypeIdentifierNode, TypeComponent>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(inferenceUtil: InferenceUtil, node: TypeIdentifierNode): InferenceResult {
        return inferenceUtil.find<TypeSystem>(node.getPath(), invocation, node)
            .inferenceResult()
    }
}

object MetaTypeInference : ITypeExpressionInference<MetaTypeNode, MonomorphicType<*>>, KoinComponent {
    private val printer: Printer by inject()

    override fun infer(inferenceUtil: InferenceUtil, node: MetaTypeNode): InferenceResult {
        val polyType = inferenceUtil.inferAs<TypeIdentifierNode, PolymorphicType<Type>>(node.typeConstructorIdentifier)
        val parameters = inferenceUtil.inferAllAs<TypeExpressionNode, TypeComponent>(node.typeParameters, AnyInferenceContext(TypeExpressionNode::class.java))
            .mapIndexed { idx, type -> Pair(idx, type) }

        return TypeMonomorphiser.monomorphise(inferenceUtil.toCtx(), polyType, parameters)
            .toInferenceResult(printer)
    }
}