package org.orbit.types.components

import org.orbit.core.nodes.LambdaLiteralNode

object LambdaLiteralInference : TypeInference<LambdaLiteralNode> {
    override fun infer(context: Context, node: LambdaLiteralNode, typeAnnotation: TypeProtocol?): TypeProtocol = context.withSubContext { ctx ->
        IntrinsicTypes.Unit.type
//        node.bindings.forEach {
//            val type = TypeInferenceUtil.infer(context, it.typeExpressionNode)
//
//            it.annotate(type, Annotations.Type)
//            it.typeExpressionNode.annotate(type, Annotations.Type)
//            ctx.bind(it.identifierNode.identifier, type)
//        }
//
//        val resultType = TypeInferenceUtil.infer(ctx, node.body, typeAnnotation)
//        val parameterTypes = node.bindings.map { ctx.get(it.identifierNode.identifier)!! }
//        val result = Function("_", parameterTypes, resultType)
//
//        node.annotate(result, Annotations.Type)
//
//        result
    }
}