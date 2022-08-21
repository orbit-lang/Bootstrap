package org.orbit.precess.backend.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.phase.Phase
import org.orbit.precess.backend.ast.NodeWalker
import org.orbit.precess.backend.components.Decl
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

    fun toWalkResult() : NodeWalker.WalkResult = when (this) {
        is True -> NodeWalker.WalkResult.Success { env }
        is False -> NodeWalker.WalkResult.Failure(reason)
    }
}

typealias Proposition = (Env) -> PropositionResult

operator fun Proposition.plus(other: Proposition) : Proposition = { env ->
    other.invoke(invoke(env).invoke())
}

class Interpreter : Phase<ProgramNode, Unit>, KoinComponent {
    override val invocation: Invocation by inject()

    private val contexts = mutableMapOf<String, (Env) -> Env>()
    private val propositions = mutableMapOf<String, Proposition>()

    val root = Env()

    fun with(env: Pair<String, Env>) : Interpreter {
        val pContexts = contexts
        val pPropositions = propositions

        return Interpreter().apply {
            this.contexts.putAll(pContexts)
            this.propositions.putAll(pPropositions)

            this.addContext(env.first) { env.second }
        }
    }

    fun addContext(id: String, fn: (Env) -> Env) {
        contexts[id] = fn
    }

    fun getContext(id: String) : ((Env) -> Env)? = contexts[id]

    fun addProposition(id: String, proposition: Proposition) {
        propositions[id] = proposition
    }

    fun getProposition(id: String) : Proposition? = propositions[id]

    override fun execute(input: ProgramNode) {
//        input.statements.forEach { it.walk(this) }
    }
}