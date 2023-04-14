package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.MethodReferenceNode
import org.orbit.util.Invocation

object MethodReferenceInference : ITypeInference<MethodReferenceNode, ITypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: MethodReferenceNode, env: ITypeEnvironment): AnyType {
        val receiver = TypeInferenceUtils.infer(node.typeExpressionNode, env)

        if (node.isConstructor) {
            return Signature(receiver, "__init__", emptyList(), receiver, false)
        }

        var possibleSignatures = env.getSignatures(node.identifierNode.identifier)
        val error = "No methods found matching `${node.identifierNode.identifier} : (${receiver}, *) -> *`"

        if (possibleSignatures.isEmpty()) {
            throw invocation.make<TypeSystem>(error, node.identifierNode)
        }

        if (possibleSignatures.count() == 1) {
            val signature = possibleSignatures[0]
            if (TypeUtils.checkEq(env, signature.component.receiver, receiver)) {
                return signature.component
            }

            throw invocation.make<TypeSystem>(error, node.identifierNode)
        }

        possibleSignatures = possibleSignatures.filter { TypeUtils.checkEq(env, it.component.receiver, receiver) }

        if (possibleSignatures.isEmpty()) {
            throw invocation.make<TypeSystem>(error, node.identifierNode)
        }

        if (possibleSignatures.count() == 1) {
            return possibleSignatures[0].component
        }

        if (possibleSignatures.isEmpty()) {
            throw invocation.make<TypeSystem>(error, node)
        }

        val pretty = possibleSignatures.joinToString("\n\t") { it.component.toString() }

        throw invocation.make<TypeSystem>("Multiple methods can be referenced by identifier `${node.identifierNode.identifier}`. Please specify the intended receiver Type to disambiguate between these options:\n\t$pretty", node)
    }
}