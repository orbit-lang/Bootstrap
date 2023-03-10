package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.AnyArrow
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.ForNode
import org.orbit.util.Invocation

object ForInference : ITypeInference<ForNode, ITypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: ForNode, env: ITypeEnvironment): AnyType {
        val expr = TypeInferenceUtils.infer(node.iterable, env)
        val iter = expr as? IType.Array
            ?: throw invocation.make<TypeSystem>("For expressions can only iterate over Collections.", node.iterable)

        val arrow = when (val body = TypeInferenceUtils.infer(node.body, env)) {
            is AnyArrow -> body
            else -> throw invocation.make<TypeSystem>("The body of a For expression must be invokable. Found $body", node.body)
        }

        val elem = when (arrow.getDomain().count()) {
            1 -> arrow.getDomain()[0]
            else -> throw invocation.make<TypeSystem>("The body of a For expression must be invokable and accept a single parameter. Found ${arrow.getDomain()}", node.body)
        }

        if (!TypeUtils.checkEq(env, iter.element, elem)) {
            throw invocation.make<TypeSystem>("", node.body)
        }

        return IType.Array(arrow.getCodomain(), iter.size)
    }
}