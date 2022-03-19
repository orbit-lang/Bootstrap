package org.orbit.types.next.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.*
import org.orbit.types.next.components.Func
import org.orbit.types.next.components.IType
import org.orbit.types.next.intrinsics.Native
import org.orbit.types.phase.TypeSystem
import org.orbit.util.Invocation

object IntLiteralInference : Inference<IntLiteralNode> {
    override fun infer(inferenceUtil: InferenceUtil, node: IntLiteralNode): IType = Native.Type.Int.type
}

object SymbolLiteralInference : Inference<SymbolLiteralNode> {
    override fun infer(inferenceUtil: InferenceUtil, node: SymbolLiteralNode): IType = Native.Type.Symbol.type
}

object BlockInference : Inference<BlockNode>, KoinComponent {
    override fun infer(inferenceUtil: InferenceUtil, node: BlockNode): IType {
        val bodyTypes = node.body.map { inferenceUtil.infer(it) }

        return bodyTypes.lastOrNull() ?: Native.Type.Unit.type
    }
}

object LambdaLiteralInference : Inference<LambdaLiteralNode>, KoinComponent {
    private val inferenceUtil: InferenceUtil by inject()

    override fun infer(inferenceUtil: InferenceUtil, node: LambdaLiteralNode): IType {
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

        return lambda
    }
}

object VariableInference : Inference<IdentifierNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(inferenceUtil: InferenceUtil, node: IdentifierNode): IType {
        val type = inferenceUtil.getType(node.identifier)
            ?: throw invocation.make<TypeSystem>("Could not infer type of ${node.identifier}", node)

        inferenceUtil.set(node, type)

        return type
    }
}