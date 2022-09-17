package org.orbit.backend.typesystem.utils

import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatformTools
import org.orbit.backend.typesystem.inference.ITypeInference
import org.orbit.backend.typesystem.inference.run
import org.orbit.core.nodes.INode
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType

object TypeSystemUtils {
    inline fun <reified N: INode> infer(node: N, env: Env) : IType<*> {
        val inference = KoinPlatformTools.defaultContext().get().get<ITypeInference<N>>(named("infer${node::class.java.simpleName}"))

        return inference.run(node, env)
    }

    inline fun <reified N: INode, reified T: IType<T>> inferAs(node: N, env: Env) : T
        = infer(node, env) as T

    inline fun <reified N: INode> inferAll(nodes: List<N>, env: Env) : List<IType<*>>
        = nodes.map { infer(it, env) }

    inline fun <reified N: INode, reified T: IType<T>> inferAllAs(nodes: List<N>, env: Env) : List<T>
        = nodes.map { inferAs(it, env) }
}