package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.MethodCallNode
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.utils.TypeUtils
import org.orbit.util.Invocation

object MethodCallInference : ITypeInference<MethodCallNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: MethodCallNode, env: Env): IType<*> {
        val receiverType = TypeSystemUtils.infer(node.receiverExpression, env)
        val argTypes = TypeSystemUtils.inferAll(node.parameterNodes, env)
        var possibleArrows = env.getSignatures(node.messageIdentifier.identifier, receiverType)
        val expected = TypeSystemUtils.popTypeAnnotation() ?: IType.Always

        if (possibleArrows.isEmpty()) {
            throw invocation.make<TypeSystem>("No methods found matching signature `${receiverType.id}.${node.messageIdentifier.identifier} : (${argTypes.map { it.id }.joinToString(", ")}) -> ???`", node)
        }

        if (possibleArrows.count() > 1) {
            // See if we can differentiate by return type
            possibleArrows = possibleArrows.filter { TypeUtils.checkEq(env, it.returns, expected) }
        }

        if (possibleArrows.isEmpty()) {
            throw invocation.make<TypeSystem>("No methods found matching signature `${receiverType.id}.${node.messageIdentifier.identifier} : (${argTypes.map { it.id }.joinToString(", ")}) -> ???`", node)
        }

        if (possibleArrows.count() > 1) {
            // We've failed to narrow down the results, we have to error now
            throw invocation.make<TypeSystem>("Multiple methods found matching signature `${possibleArrows[0].id}`", node)
        }

        val arrow = possibleArrows[0]

        if (!TypeUtils.checkEq(env, arrow.returns, expected)) {
            throw invocation.make<TypeSystem>("Return Type of method `${receiverType.id}.${node.messageIdentifier.identifier}` does not match the expected Type in this context. Expected `${expected.id}`, found `${arrow.returns.id}`", node)
        }

        val argsCount = argTypes.count()
        val paramsCount = arrow.parameters.count()

        if (argsCount != paramsCount) {
            throw invocation.make<TypeSystem>("Method `${node.messageIdentifier.identifier}` expects $paramsCount arguments, found $argsCount", node)
        }

        val zip = argTypes.zip(arrow.parameters)
        for ((idx, pair) in zip.withIndex()) {
            if (!TypeUtils.checkEq(env, pair.first, pair.second)) {
                throw invocation.make<TypeSystem>("Method `${node.messageIdentifier.identifier}` expects argument of Type `${pair.second.id}` at index $idx, found `${pair.first.id}`", node.parameterNodes[idx])
            }
        }

        return arrow.returns
    }
}