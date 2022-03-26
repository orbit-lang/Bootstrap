package org.orbit.types.next.inference

import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.core.nodes.TypeExpressionNode
import org.orbit.types.next.components.*
import org.orbit.types.next.intrinsics.Native

object SignatureInference : Inference<MethodSignatureNode, ISignature> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: MethodSignatureNode): InferenceResult {
        val typeParameters: List<Parameter> = when (val pNode = node.typeParameters) {
            null -> emptyList()
            else -> inferenceUtil.inferAllAs(pNode.typeParameters, TypeLiteralInferenceContext.TypeParameterContext)
        }

        typeParameters.forEach {
            inferenceUtil.declare(it)
        }

        val receiver = inferenceUtil.infer(node.receiverTypeNode)
        val parameterTypeNodes = node.parameterNodes.map { it.typeExpressionNode }
        val parameters = inferenceUtil.inferAllAs<TypeExpressionNode, TypeComponent>(parameterTypeNodes, AnyInferenceContext(TypeExpressionNode::class.java))
        val returns: TypeComponent = when (val rNode = node.returnTypeNode) {
            null -> Native.Types.Unit.type
            else -> inferenceUtil.infer(rNode)
        }

        val signature = Signature(node.identifierNode.identifier, receiver, parameters, returns)

        return when (typeParameters.isEmpty()) {
            true -> signature
            else -> PolymorphicType(signature, typeParameters, false)
        }.inferenceResult()
    }
}