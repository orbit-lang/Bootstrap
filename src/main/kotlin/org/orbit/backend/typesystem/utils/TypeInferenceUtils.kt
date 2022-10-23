package org.orbit.backend.typesystem.utils

import org.koin.core.parameter.DefinitionParameters
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatformTools
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.inference.ITypeInference
import org.orbit.backend.typesystem.inference.run
import org.orbit.core.nodes.INode

object TypeInferenceUtils {
    inline fun <reified N: INode, reified E: ITypeEnvironment> infer(node: N, env: E, parameters: DefinitionParameters? = null) : AnyType {
        val inference = KoinPlatformTools.defaultContext().get().get<ITypeInference<N, E>>(named("infer${node::class.java.simpleName}")) { parameters ?: parametersOf() }

        return inference.run(node, env)
    }

    inline fun <reified N: INode, reified T: AnyType> inferAs(node: N, env: ITypeEnvironment, parameters: DefinitionParameters? = null) : T
        = infer(node, env, parameters) as T

    inline fun <reified N: INode> inferAll(nodes: List<N>, env: ITypeEnvironment, parameters: DefinitionParameters? = null) : List<AnyType>
        = nodes.map { infer(it, env, parameters) }

    inline fun <reified N: INode, reified T: AnyType> inferAllAs(nodes: List<N>, env: ITypeEnvironment, parameters: DefinitionParameters? = null) : List<T>
        = nodes.map { inferAs(it, env, parameters) }
}