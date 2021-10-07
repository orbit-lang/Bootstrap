package org.orbit.backend.codegen.c.units

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.CodeUnit
import org.orbit.backend.codegen.common.AbstractPrintStatementUnit
import org.orbit.core.CodeGeneratorQualifier
import org.orbit.core.Mangler
import org.orbit.core.injectQualified
import org.orbit.core.nodes.PrintNode

class PrintStatementUnit(override val node: PrintNode, override val depth: Int) : AbstractPrintStatementUnit, KoinComponent {
    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory by injectQualified(codeGeneratorQualifier)

    override fun generate(mangler: Mangler): String {
        val value = codeGenFactory.getExpressionUnit(node.expressionNode, depth)
            .generate(mangler)

        // TODO - printf
        return "printf(\"%s\\n\", $value);".prependIndent(indent())
    }
}