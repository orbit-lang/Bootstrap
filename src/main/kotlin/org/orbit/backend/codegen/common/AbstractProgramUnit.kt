package org.orbit.backend.codegen.common

import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.nodes.ProgramNode

interface AbstractProgramUnit<H: AbstractHeader> : CodeUnit<ProgramNode> {
    val header: H
}