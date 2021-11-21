package org.orbit.backend.codegen.c.units

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.common.AbstractPropertyProjectionUnit
import org.orbit.core.*
import org.orbit.core.nodes.AssignmentStatementNode

class PropertyProjectionUnit(override val node: AssignmentStatementNode, override val depth: Int) : AbstractPropertyProjectionUnit, KoinComponent {
    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory<CHeader> by injectQualified(codeGeneratorQualifier)

    override fun generate(mangler: Mangler): String {
        // TODO
        return ""
    }
}