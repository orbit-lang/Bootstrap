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

class Interpreter : Phase<ProgramNode, Unit>, KoinComponent {
    override val invocation: Invocation by inject()

    private val propositions = mutableMapOf<String, Proposition>()

    fun addProposition(id: String, proposition: Proposition) {
        propositions[id] = proposition
    }

    fun getProposition(id: String) : Proposition? = propositions[id]

    override fun execute(input: ProgramNode) {
//        input.statements.forEach { it.walk(this) }
    }
}