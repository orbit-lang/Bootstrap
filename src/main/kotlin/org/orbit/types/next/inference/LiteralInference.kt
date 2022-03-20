package org.orbit.types.next.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.getPath
import org.orbit.core.nodes.*
import org.orbit.types.next.components.Func
import org.orbit.types.next.components.IType
import org.orbit.types.next.components.Type
import org.orbit.types.next.intrinsics.Native
import org.orbit.types.next.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.next.find
import org.orbit.util.next.getTypeOrNever

object IntLiteralInference : Inference<IntLiteralNode, Type> {
    override fun infer(inferenceUtil: InferenceUtil, node: IntLiteralNode): InferenceResult
        = Native.Types.Int.type.inferenceResult()
}

object SymbolLiteralInference : Inference<SymbolLiteralNode, Type> {
    override fun infer(inferenceUtil: InferenceUtil, node: SymbolLiteralNode): InferenceResult
        = Native.Types.Symbol.type.inferenceResult()
}

object BlockInference : Inference<BlockNode, IType>, KoinComponent {
    override fun infer(inferenceUtil: InferenceUtil, node: BlockNode): InferenceResult {
        val bodyTypes = node.body.map { inferenceUtil.infer(it) }

        return (bodyTypes.lastOrNull() ?: Native.Types.Unit.type)
            .inferenceResult()
    }
}

object LambdaLiteralInference : Inference<LambdaLiteralNode, IType>, KoinComponent {
    override fun infer(inferenceUtil: InferenceUtil, node: LambdaLiteralNode): InferenceResult {
        val parameterTypes = node.bindings.map {
            val type = inferenceUtil.infer(it)

            inferenceUtil.set(it, type)
            inferenceUtil.set(it.typeExpressionNode, type)

            type
        }

        val resultType = inferenceUtil.infer(node.body)
        val result = Func(parameterTypes, resultType)
        val lambda = result.curry()

        inferenceUtil.set(node, lambda)

        return lambda.inferenceResult()
    }
}

object VariableInference : Inference<IdentifierNode, IType>, KoinComponent {
    override fun infer(inferenceUtil: InferenceUtil, node: IdentifierNode): InferenceResult
        = inferenceUtil.getTypeOrNever(node.identifier).inferenceResult()
}

object TypeLiteralInference : Inference<TypeIdentifierNode, IType>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(inferenceUtil: InferenceUtil, node: TypeIdentifierNode): InferenceResult {
        return inferenceUtil.find<TypeSystem>(node.getPath(), invocation, node)
            .inferenceResult()
    }
}