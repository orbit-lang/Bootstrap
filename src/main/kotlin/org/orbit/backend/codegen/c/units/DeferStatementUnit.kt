package org.orbit.backend.codegen.c.units

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.common.AbstractDeferStatementUnit
import org.orbit.backend.codegen.common.BlockUnit
import org.orbit.core.CodeGeneratorQualifier
import org.orbit.core.Mangler
import org.orbit.core.injectQualified
import org.orbit.core.nodes.DeferNode

class DeferStatementUnit(override val node: DeferNode, override val depth: Int) : AbstractDeferStatementUnit, KoinComponent {
    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory by injectQualified(codeGeneratorQualifier)

    // TODO
    override fun generate(mangler: Mangler): String {
        val block = codeGenFactory.getBlockUnit(node.blockNode, depth, true)
            .generate(mangler)

        val retId = when (node.returnValueIdentifier) {
            null -> ""
            else -> "${node.returnValueIdentifier!!.identifier} in"
        }

        return """
            |let __on_defer = { $retId
            |$block
            |}
        """.trimMargin().prependIndent()
    }
}