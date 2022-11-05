package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.MethodCallNode
import org.orbit.util.Invocation

object MethodCallInference : ITypeInference<MethodCallNode, ITypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: MethodCallNode, env: ITypeEnvironment): AnyType {
        val receiver = TypeInferenceUtils.infer(node.receiverExpression, env)
        val args = TypeInferenceUtils.inferAll(node.parameterNodes, env)
        var possibleArrows = env.getSignatures(node.messageIdentifier.identifier)
        val expected = (env as? AnnotatedTypeEnvironment)?.typeAnnotation ?: IType.Always

        if (possibleArrows.isEmpty()) {
            throw invocation.make<TypeSystem>("No methods found matching signature `$receiver.${node.messageIdentifier.identifier} : (${args.joinToString(", ")}) -> ???`", node)
        }

        if (possibleArrows.count() > 1) {
            // See if we can differentiate by return type
            possibleArrows = possibleArrows.filter { TypeUtils.checkEq(env, it.component.returns, expected) }
        }

        if (possibleArrows.isEmpty()) {
            throw invocation.make<TypeSystem>("No methods found matching signature `$receiver.${node.messageIdentifier.identifier} : (${args.joinToString(", ")}) -> ???`", node)
        }

        possibleArrows = possibleArrows.filter { TypeUtils.checkEq(env, it.component.receiver, receiver) }

        if (possibleArrows.count() > 1) {
            // We've failed to narrow down the results, we have to error now
            throw invocation.make<TypeSystem>("Multiple methods found matching signature `${possibleArrows[0]}`", node)
        }

        if (possibleArrows.isEmpty()) {
            throw invocation.make<TypeSystem>("No methods found matching signature `$receiver.${node.messageIdentifier.identifier} : (${args.joinToString(", ")}) -> ???`", node)
        }

        val arrow = possibleArrows[0].component

        val argsCount = args.count()
        val paramsCount = arrow.parameters.count()

        if (argsCount != paramsCount) {
            throw invocation.make<TypeSystem>("Method `${node.messageIdentifier.identifier}` expects $paramsCount arguments, found $argsCount", node)
        }

        val zip = args.zip(arrow.parameters)
        for ((idx, pair) in zip.withIndex()) {
            if (!TypeUtils.checkEq(env, pair.first, pair.second)) {
                throw invocation.make<TypeSystem>("Method `${node.messageIdentifier.identifier}` expects argument of Type `$pair.second` at index $idx, found `$pair`", node.parameterNodes[idx])
            }
        }

        return arrow.returns
    }
}