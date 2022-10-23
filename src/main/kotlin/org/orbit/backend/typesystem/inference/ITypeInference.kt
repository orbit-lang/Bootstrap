package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.core.nodes.INode
import org.orbit.util.Invocation
import org.orbit.util.getKoinInstance

interface ITypeInference<N: INode, E: ITypeEnvironment> {
    fun infer(node: N, env: E) : AnyType
}

fun <N: INode, E: ITypeEnvironment> ITypeInference<N, E>.run(node: N, env: E) : AnyType = when (val result = infer(node, env)) {
    is IType.Never -> throw getKoinInstance<Invocation>().make<TypeSystem>(result.message, node)
    else -> result
}