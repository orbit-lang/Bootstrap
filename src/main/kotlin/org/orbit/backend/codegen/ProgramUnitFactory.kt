package org.orbit.backend.codegen

import org.orbit.core.nodes.ProgramNode

interface ProgramUnitFactory {
    fun getProgramUnit(input: ProgramNode) : CodeUnit<ProgramNode>
}