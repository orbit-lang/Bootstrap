package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.core.nodes.INode
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.components.Inf
import org.orbit.util.Invocation
import org.orbit.util.getKoinInstance

interface ITypeInference<N: INode> {
    fun infer(node: N, env: Env) : AnyType
}

fun <N: INode> ITypeInference<N>.run(node: N, env: Env) : AnyType = when (val result = infer(node, env)) {
    is IType.Never -> throw getKoinInstance<Invocation>().make<TypeSystem>(result.message, node)
    else -> result
}