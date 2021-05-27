package org.orbit.core

import org.orbit.analysis.Analyser
import org.orbit.analysis.semantics.NestedTraitAnalyser
import org.orbit.analysis.semantics.RedundantReturnAnalyser
import org.orbit.analysis.semantics.UnreachableReturnAnalyser
import org.orbit.analysis.types.IntLiteralAnalyser
import org.orbit.core.nodes.ProgramNode
import org.orbit.core.phase.AdaptablePhase
import org.orbit.core.phase.ImmediateParallelPhase
import org.orbit.core.phase.PhaseLinker
import org.orbit.frontend.phase.CommentParser
import org.orbit.frontend.phase.Lexer
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.components.TokenTypes
import org.orbit.frontend.rules.ProgramRule
import org.orbit.graph.phase.CanonicalNameResolver
import org.orbit.graph.components.Environment
import org.orbit.util.Invocation

abstract class PhaseChainBuilder<I: Any, D: DeferredPhase<I, *, *, *>> : AdaptablePhase<I, D>()

typealias FrontendPhaseType = DeferredPhase<SourceProvider, SourceProvider, CommentParser.Result, Lexer.Result>
typealias SemanticPhaseType = DeferredPhase<Parser.InputType, Parser.Result, Parser.Result, Environment>

data class DeferredPhase<I1: Any, I2: Any, O1: Any, O2: Any>(
    val phaseLinker: PhaseLinker<I1, I2, O1, O2>,
    val initialPhaseInput: I1
)
