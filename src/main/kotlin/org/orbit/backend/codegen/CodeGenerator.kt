package org.orbit.backend.codegen

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.*
import org.orbit.core.nodes.Node
import org.orbit.core.nodes.ProgramNode
import org.orbit.util.Invocation

interface CodeUnit<N: Node> {
    val node: N
    val depth: Int

    fun generate(mangler: Mangler) : String
    fun indent(count: Int = depth) : String = "\t".repeat(count)
    fun newline(count: Int = 1) : String = "${"\n".repeat(count)}${indent()}"
}

interface ProgramUnitFactory {
    fun getProgramUnit(input: ProgramNode) : CodeUnit<ProgramNode>
}

object CodeWriter : Phase<ProgramNode, String>, KoinComponent {
    override val invocation: Invocation by inject()
    private val mangler: Mangler by injectQualified(CodeGeneratorQualifier.Swift)
    private val factory: ProgramUnitFactory by injectQualified(CodeGeneratorQualifier.Swift)

    override fun execute(input: ProgramNode): String {
        return factory.getProgramUnit(input).generate(mangler)
    }
}