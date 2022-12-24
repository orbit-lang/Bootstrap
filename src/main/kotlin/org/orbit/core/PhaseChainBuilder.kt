package org.orbit.core

import org.orbit.core.phase.PhaseLinker
import org.orbit.frontend.phase.CommentParser
import org.orbit.frontend.phase.Lexer

typealias FrontendPhaseType = DeferredPhase<SourceProvider, SourceProvider, CommentParser.Result, Lexer.Result>

data class DeferredPhase<I1: Any, I2: Any, O1: Any, O2: Any>(
    val phaseLinker: PhaseLinker<I1, I2, O1, O2>,
    val initialPhaseInput: I1
)
