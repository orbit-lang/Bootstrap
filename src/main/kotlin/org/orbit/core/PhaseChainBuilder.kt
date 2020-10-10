package org.orbit.core

import org.orbit.analysis.Analyser
import org.orbit.analysis.semantics.NestedTraitAnalyser
import org.orbit.analysis.semantics.RedundantReturnAnalyser
import org.orbit.analysis.semantics.UnreachableReturnAnalyser
import org.orbit.analysis.types.IntLiteralAnalyser
import org.orbit.core.nodes.ProgramNode
import org.orbit.frontend.CommentParser
import org.orbit.frontend.Lexer
import org.orbit.frontend.Parser
import org.orbit.frontend.TokenTypes
import org.orbit.frontend.rules.ProgramRule
import org.orbit.graph.CanonicalNameResolver
import org.orbit.graph.Environment
import org.orbit.util.Invocation

abstract class PhaseChainBuilder<I: Any, D: DeferredPhase<I, *, *, *>> : AdaptablePhase<I, D>()

typealias FrontendPhaseType = DeferredPhase<SourceProvider, SourceProvider, CommentParser.Result, Lexer.Result>
typealias SemanticPhaseType = DeferredPhase<Parser.InputType, Parser.Result, Parser.Result, Environment>

data class DeferredPhase<I1: Any, I2: Any, O1: Any, O2: Any>(
    val phaseLinker: PhaseLinker<I1, I2, O1, O2>,
    val initialPhaseInput: I1
)

class Frontend(override val invocation: Invocation) : PhaseChainBuilder<SourceProvider, FrontendPhaseType>() {
    override val inputType: Class<SourceProvider> = SourceProvider::class.java
    override val outputType: Class<FrontendPhaseType> = synthesiseOutputType<FrontendPhaseType>()

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified P: FrontendPhaseType> synthesiseOutputType() : Class<FrontendPhaseType> {
        return P::class.java as Class<FrontendPhaseType>
    }

    override fun execute(input: SourceProvider) : FrontendPhaseType {
        val commentParser = CommentParser(invocation)
        val lexer = Lexer(invocation, TokenTypes)
        val linker = PhaseLinker(invocation, initialPhase = commentParser, finalPhase = lexer)

        return FrontendPhaseType(linker, input)
    }
}

class Semantics(override val invocation: Invocation) : PhaseChainBuilder<Parser.InputType, SemanticPhaseType>() {
    override val inputType: Class<Parser.InputType> = Parser.InputType::class.java
    override val outputType: Class<SemanticPhaseType> = synthesiseOutputType<SemanticPhaseType>()

    init {
        registerAdapter(Parser.FrontendAdapter)
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified P: SemanticPhaseType> synthesiseOutputType() : Class<SemanticPhaseType> {
        return P::class.java as Class<SemanticPhaseType>
    }

    override fun execute(input: Parser.InputType): SemanticPhaseType {
        val parser = Parser(invocation, ProgramRule)
        val canonicalNameResolver = CanonicalNameResolver(invocation)
        val linker = PhaseLinker(invocation, initialPhase = parser, finalPhase = canonicalNameResolver)

        return SemanticPhaseType(linker, input)
    }
}

class Correctness(override val invocation: Invocation) : AdaptablePhase<ProgramNode, List<Analyser.Report>>() {
    override val inputType: Class<ProgramNode> = ProgramNode::class.java
    override val outputType: Class<List<Analyser.Report>> = synthesiseOutputType()

    private inline fun <reified L: List<Analyser.Report>> synthesiseOutputType() : Class<L> {
        return L::class.java
    }

    init {
        registerAdapter(Analyser.SemanticsAdapter)
    }

    override fun execute(input: ProgramNode) : List<Analyser.Report> {
        val semanticAnalyser = Analyser(invocation, "Semantics",
            NestedTraitAnalyser(invocation),
            UnreachableReturnAnalyser(invocation),
            RedundantReturnAnalyser(invocation)
        )

        val typeAnalyser = Analyser(invocation, "Types", IntLiteralAnalyser(invocation))
        val phase = ImmediateParallelPhase(invocation, ProgramNode::class.java, listOf(semanticAnalyser, typeAnalyser))

        return phase.execute(input)
    }
}