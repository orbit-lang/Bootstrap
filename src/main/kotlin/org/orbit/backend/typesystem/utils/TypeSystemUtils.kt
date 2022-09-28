package org.orbit.backend.typesystem.utils

import org.koin.core.component.KoinComponent
import org.koin.core.parameter.DefinitionParameters
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatformTools
import org.orbit.backend.typesystem.inference.ITypeInference
import org.orbit.backend.typesystem.inference.evidence.ContextualEvidence
import org.orbit.backend.typesystem.inference.evidence.IContextualEvidenceProvider
import org.orbit.backend.typesystem.inference.evidence.IEvidence
import org.orbit.backend.typesystem.inference.run
import org.orbit.backend.typesystem.phase.globalContext
import org.orbit.core.nodes.INode
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.utils.AnyType
import org.orbit.util.getKoinInstance

object TypeSystemUtils : KoinComponent {
    private var typeAnnotation: AnyType? = null

    inline fun <reified N: INode> infer(node: N, env: Env, parameters: DefinitionParameters? = null) : AnyType {
        val inference = KoinPlatformTools.defaultContext().get().get<ITypeInference<N>>(named("infer${node::class.java.simpleName}")) { parameters ?: parametersOf() }

        return inference.run(node, env)
    }

    inline fun <reified N: INode, reified T: AnyType> inferAs(node: N, env: Env, parameters: DefinitionParameters? = null) : T
        = infer(node, env, parameters) as T

    inline fun <reified N: INode> inferAll(nodes: List<N>, env: Env, parameters: DefinitionParameters? = null) : List<AnyType>
        = nodes.map { infer(it, env, parameters) }

    inline fun <reified N: INode, reified T: AnyType> inferAllAs(nodes: List<N>, env: Env, parameters: DefinitionParameters? = null) : List<T>
        = nodes.map { inferAs(it, env, parameters) }

    inline fun <reified N: INode> gatherEvidence(node: N) : IEvidence {
        val globalContext = getKoinInstance<Env>("globalContext")
        val provider = KoinPlatformTools.defaultContext().get().get<IContextualEvidenceProvider<N>>(named("evidence${node::class.java.simpleName}"))

        return provider.provideEvidence(globalContext, node)
    }

    inline fun <reified N: INode> gatherAllEvidence(nodes: List<N>) : IEvidence
        = nodes.fold(ContextualEvidence.unit as IEvidence) { acc, next -> acc + gatherEvidence(next) }

    fun pushTypeAnnotation(type: AnyType) {
        typeAnnotation = type
    }

    fun popTypeAnnotation() : AnyType? {
        return typeAnnotation?.also { typeAnnotation = null }
    }
}