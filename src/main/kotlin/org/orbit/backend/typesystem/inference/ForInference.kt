package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.AnyArrow
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.ForNode
import org.orbit.frontend.rules.CollectionTypeInference
import org.orbit.util.Invocation

object ForInference : ITypeInference<ForNode, ITypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: ForNode, env: ITypeEnvironment): AnyType {
        val expr = TypeInferenceUtils.infer(node.iterable, env)
        val iter = expr as? IType.Array
            ?: throw invocation.make<TypeSystem>("For expressions can only iterate over Collections.", node.iterable)

        // TODO - Ensure `iter` conforms to `Iterable` Trait

        return when (val body = TypeInferenceUtils.infer(node.body, env)) {
            is AnyArrow -> IType.Array(body.getCodomain(), iter.size)
            else -> throw invocation.make<TypeSystem>("The body of a For expression must be invokable. Found $body", node.body)
        }
    }
}