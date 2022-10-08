package org.orbit.backend.typesystem.inference.evidence

import org.koin.core.component.KoinComponent
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.phase.globalContext
import org.orbit.core.nodes.INode
import java.lang.reflect.TypeVariable

sealed interface IEvidence {
    operator fun plus(other: IEvidence) : IEvidence
}

fun IEvidence.asSuccessOrNull() : Env? = when (this) {
    is ContextualEvidence -> context
    is CompoundEvidence -> evidence.reduce(IEvidence::plus).asSuccessOrNull()
    else -> null
}

data class Contradiction(val exhibitA: IEvidence, val exhibitB: IEvidence) : IEvidence {
    override fun plus(other: IEvidence): IEvidence = this
}

data class TypeVariableEvidence(val abstract: IType.TypeVar, val concrete: AnyType) : IEvidence {
    override fun plus(other: IEvidence): IEvidence = CompoundEvidence(listOf(this, other))
}

data class ContextualEvidence(val context: Env) : IEvidence {
    companion object : KoinComponent {
        private val globalContext: Env by globalContext()

        val unit = ContextualEvidence(globalContext)
    }

    override fun plus(other: IEvidence): IEvidence = when (other) {
        is ContextualEvidence -> ContextualEvidence(context + other.context)
        else -> TODO()
    }
}

data class CompoundEvidence(val evidence: List<IEvidence>) : IEvidence {
    override fun plus(other: IEvidence): IEvidence = when (other) {
        is CompoundEvidence -> CompoundEvidence(evidence + other.evidence)
        else -> CompoundEvidence(evidence + other)
    }
}

//data class ContextualEvidence(val evidence: List<Pair<AnyType, Env>>) : IEvidence {
//    constructor(type: AnyType, context: Env) : this(listOf(Pair(type, context)))
//    constructor() : this(emptyList())
//
//    companion object : KoinComponent {
//        private val globalContext: Env by globalContext()
//
//        val unit = ContextualEvidence(IType.Unit, globalContext)
//    }
//
//    val isEmpty: Boolean get() = evidence.isEmpty()
//
//    fun merge() : Env
//        = evidence.fold(Env()) { acc, next -> acc + next.second }
//
//    override fun plus(other: IEvidence): IEvidence = when (other) {
//        is ContextualEvidence -> ContextualEvidence(evidence + other.evidence)
//    }
//
//    operator fun plus(other: ContextualEvidence) : ContextualEvidence
//        = ContextualEvidence(evidence + other.evidence)
//}

interface IContextualEvidenceProvider<N: INode> {
    fun provideEvidence(env: Env, node: N) : IEvidence
}