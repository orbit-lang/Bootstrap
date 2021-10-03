package org.orbit.backend.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.core.CodeGeneratorQualifier
import org.orbit.core.Mangler
import org.orbit.core.phase.Phase
import org.orbit.core.injectQualified
import org.orbit.core.nodes.ProgramNode
import org.orbit.util.Invocation

object CodeWriter : Phase<ProgramNode, String>, KoinComponent {
    override val invocation: Invocation by inject()
    private val codeGenerationTarget: CodeGeneratorQualifier by inject()
    private val mangler: Mangler by injectQualified(codeGenerationTarget)
    private val codeGenFactory: CodeGenFactory by injectQualified(codeGenerationTarget)

    override fun execute(input: ProgramNode): String {
        return codeGenFactory.getProgramUnit(input, 0)
            .generate(mangler)
    }
}