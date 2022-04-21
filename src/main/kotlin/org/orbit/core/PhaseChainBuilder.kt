package org.orbit.core

import org.orbit.core.phase.AdaptablePhase
import org.orbit.core.phase.PhaseLinker
import org.orbit.frontend.phase.CommentParser
import org.orbit.frontend.phase.Lexer
import org.orbit.frontend.phase.Parser
import org.orbit.graph.components.Environment

abstract class PhaseChainBuilder<I: Any, D: DeferredPhase<I, *, *, *>> : AdaptablePhase<I, D>()

typealias FrontendPhaseType = DeferredPhase<SourceProvider, SourceProvider, CommentParser.Result, Lexer.Result>
typealias SemanticPhaseType = DeferredPhase<Parser.InputType, Parser.Result, Parser.Result, Environment>

data class DeferredPhase<I1: Any, I2: Any, O1: Any, O2: Any>(
    val phaseLinker: PhaseLinker<I1, I2, O1, O2>,
    val initialPhaseInput: I1
)
