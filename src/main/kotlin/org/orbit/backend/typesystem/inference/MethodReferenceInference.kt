package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.MethodReferenceNode
import org.orbit.util.Invocation

object MethodReferenceInference : ITypeInference<MethodReferenceNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: MethodReferenceNode, env: Env): AnyType {
        val receiverType = TypeSystemUtils.infer(node.typeExpressionNode, env)

        if (node.isConstructor) {
            return IType.Signature(receiverType, "__init__", emptyList(), receiverType, false)
        }

        var possibleSignatures = env.getSignatures(node.identifierNode.identifier)

        val error = "No methods found matching `${node.identifierNode.identifier} : (${receiverType}, ???) -> ???`"

        if (possibleSignatures.isEmpty()) {
            throw invocation.make<TypeSystem>(error, node.identifierNode)
        }

        if (possibleSignatures.count() == 1) {
            val signature = possibleSignatures[0]
            if (TypeUtils.checkEq(env, signature.receiver, receiverType)) {
                return signature
            }

            throw invocation.make<TypeSystem>(error, node.identifierNode)
        }

        possibleSignatures = possibleSignatures.filter { TypeUtils.checkEq(env, it.receiver, receiverType) }

        if (possibleSignatures.isEmpty()) {
            throw invocation.make<TypeSystem>(error, node.identifierNode)
        }

        if (possibleSignatures.count() == 1) {
            return possibleSignatures[0]
        }

        throw invocation.make<TypeSystem>(error, node)
    }
}