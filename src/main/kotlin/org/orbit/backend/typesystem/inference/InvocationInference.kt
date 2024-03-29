package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.AnyArrow
import org.orbit.backend.typesystem.utils.EffectUtils
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.InvocationNode
import org.orbit.util.Invocation

object InvocationInference : ITypeInference<InvocationNode, IMutableTypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: InvocationNode, env: IMutableTypeEnvironment): AnyType {
        var type = TypeInferenceUtils.infer(node.invokable, env)

        type = type.flatten(Always, env) as? AnyArrow ?: throw invocation.make<TypeSystem>("Cannot invoke non Arrow Type $type", node.invokable)

        val arrow = when (env.getCurrentContext().isComplete()) {
            true -> env.getCurrentContext().applySpecialisations(type) as AnyArrow
            else -> type
        }

        val args = TypeInferenceUtils.inferAll(node.arguments, env).map { it.flatten(it, env) }
        val expectedArgsCount = arrow.getDomain().count()

        if (args.count() != expectedArgsCount) {
            throw invocation.make<TypeSystem>("Invocation of $arrow requires $expectedArgsCount arguments, found ${args.count()}", node.invokable)
        }

        for (item in args.zip(arrow.getDomain()).withIndex()) {
            val lType = item.value.first.flatten(Always, env)
            val rType = item.value.second.flatten(Always, env)

            if (!TypeUtils.checkEq(env, lType, rType)) {
                throw invocation.make<TypeSystem>("Lambda `$arrow` expects argument of Type $rType at index ${item.index}, found $lType", node.arguments[item.index])
            }
        }

        // TODO - Pass in EffectHandler
        EffectUtils.check(arrow, node.effectHandler, GlobalEnvironment)

        val lambda = GlobalEnvironment.lambdaBody(arrow)
            ?: return arrow.getCodomain()

        val nEnv = env.fork()

        lambda.bindings.map { it.identifierNode }.zip(args).forEach {
            nEnv.bind(it.first.identifier, it.second, it.first.index)
        }

        // If we can determine the original lambda, we can re-check its
        // body given the information we now have about the call-site
        // NOTE - If we don't do this, it is possible to circumvent the Type Checker in a lambda literal, e.g:
        // `sqr = { x -> x * x }` // There may not be an infix operator in scope for `x -> x`
        TypeInferenceUtils.infer(lambda.body, nEnv)

        return arrow.getCodomain()
    }
}