package org.orbit.precess.backend.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.phase.Phase
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType
import org.orbit.precess.frontend.components.nodes.ProgramNode
import org.orbit.util.Invocation

sealed interface PropositionResult {
    data class True(val env: Env) : PropositionResult
    data class False(val reason: IType.Never) : PropositionResult

    operator fun invoke() : Env = when (this) {
        is True -> env
        is False -> throw Exception(reason.message)
    }

    operator fun plus(other: PropositionResult) : PropositionResult = when (this) {
        is True -> when (other) {
            is True -> True(env + other.env)
            is False -> other
        }

        is False -> this
    }
}

typealias Proposition = (Env) -> PropositionResult

operator fun Proposition.plus(other: Proposition) : Proposition = { env ->
    other.invoke(invoke(env).invoke())
}

class Interpreter : Phase<ProgramNode, IType.IMetaType<*>>, KoinComponent {
    override val invocation: Invocation by inject()

    private val propositions = mutableMapOf<String, Proposition>()
    private val snapshots = mutableListOf<String>()

    fun addProposition(id: String, proposition: Proposition) {
        propositions[id] = proposition
    }

    fun getProposition(id: String) : Proposition? = propositions[id]

    fun takeSnapshot(env: Env, tag: String? = null) = when (tag) {
        null -> snapshots.add("Dump: $env")
        else -> snapshots.add("Dump @$tag: $env")
    }

    fun dumpSnapshots() : String
        = snapshots.joinToString("\n")

    override fun execute(input: ProgramNode) : IType.IMetaType<*> = input.statements.fold(IType.Always as IType.IMetaType<*>) { acc, next ->
        acc + next.walk(this)
    }

    override fun toString(): String
        = "{${propositions.map { it.key }}}"
}