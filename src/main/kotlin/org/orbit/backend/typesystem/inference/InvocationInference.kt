package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.AnyArrow
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.IExpressionNode
import org.orbit.core.nodes.InvocationNode
import org.orbit.util.Invocation

object InvocationInference : ITypeInference<InvocationNode, IMutableTypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: InvocationNode, env: IMutableTypeEnvironment): AnyType {
        val arrow = TypeInferenceUtils.inferAs<IExpressionNode, AnyArrow>(node.invokable, env)
        val args = TypeInferenceUtils.inferAll(node.arguments, env).map { it.flatten(it, env) }
        val expectedArgsCount = arrow.getDomain().count()

        if (args.count() != expectedArgsCount) {
            throw invocation.make<TypeSystem>("Invocation of Type $arrow requires $expectedArgsCount arguments, found ${args.count()}", node.invokable)
        }

        for (item in args.zip(arrow.getDomain()).withIndex()) {
            val lType = item.value.first.flatten(IType.Always, env)
            val rType = item.value.second.flatten(IType.Always, env)

            if (!TypeUtils.checkEq(env, lType, rType)) {
                throw invocation.make<TypeSystem>("Lambda `$arrow` expects argument of Type $rType at index ${item.index}, found $lType", node.arguments[item.index])
            }
        }

        val body = GlobalEnvironment.lambdaBody(arrow)
            ?: return arrow.getCodomain()

        val nEnv = env.fork()

        body.bindings.map { it.identifierNode.identifier }.zip(args).forEach {
            nEnv.bind(it.first, it.second)
        }

        // If we can determine the original lambda, we can re-check its
        // body given the information we now have about the call-site
        // NOTE - If we don't do this, it is possible to circumvent the Type Checker in a lambda literal, e.g:
        // `sqr = { x -> x * x }` // There may not be an infix operator in scope for `x -> x`
        TypeInferenceUtils.infer(body, nEnv)

        return arrow.getCodomain()
    }
}