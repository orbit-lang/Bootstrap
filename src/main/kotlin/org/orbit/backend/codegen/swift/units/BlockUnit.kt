package org.orbit.backend.codegen.swift.units

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.common.*
import org.orbit.core.CodeGeneratorQualifier
import org.orbit.core.Mangler
import org.orbit.core.injectQualified
import org.orbit.core.nodes.*
import org.orbit.graph.components.StringKey

class ReturnStatementUnit(override val node: ReturnStatementNode, override val depth: Int, private val resultIsDeferred: Boolean, override val deferFunctions: List<StringKey>) : AbstractReturnStatementUnit, KoinComponent {
    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory<SwiftHeader> by injectQualified(codeGeneratorQualifier)

    override fun generate(mangler: Mangler): String {
        val retVal = codeGenFactory.getRValueUnit(node.valueNode, depth)
            .generate(mangler)

        return if (resultIsDeferred) {
            """
            |let __ret_val = $retVal
            |__on_defer(__ret_val)
            |return __ret_val
            """.trimMargin()
        } else {
            """
            |return $retVal
            """.trimMargin()
        }.prependIndent(indent())
    }
}

class PrintStatementUnit(override val node: PrintNode, override val depth: Int) : AbstractPrintStatementUnit, KoinComponent {
    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory<SwiftHeader> by injectQualified(codeGeneratorQualifier)

    override fun generate(mangler: Mangler): String {
        val value = codeGenFactory.getExpressionUnit(node.expressionNode, depth)
            .generate(mangler)

        return "print($value)".prependIndent(indent())
    }
}

class AssignmentStatementUnit(override val node: AssignmentStatementNode, override val depth: Int) : AbstractAssignmentStatementUnit, KoinComponent {
    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory<SwiftHeader> by injectQualified(codeGeneratorQualifier)

    override fun generate(mangler: Mangler): String {
        // TODO - Mutability?!
        val rhs = codeGenFactory.getExpressionUnit(node.value, depth)
            .generate(mangler)

        return "let ${node.identifier.identifier} = $rhs".prependIndent(indent())
    }
}

class DeferStatementUnit(override val node: DeferNode, override val depth: Int) : AbstractDeferStatementUnit, KoinComponent {
    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory<SwiftHeader> by injectQualified(codeGeneratorQualifier)

    override fun generate(mangler: Mangler): String {
        val block = codeGenFactory.getBlockUnit(node.blockNode, depth, stripBraces = true, isMethodBody = false)
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

class DeferCallUnit(override val node: DeferNode, override val depth: Int) : AbstractDeferCallUnit {
    override fun generate(mangler: Mangler): String {
        return ""
//        if (!node.containsDefer) return ""
//        if (node.containsReturn) return ""
//
//        return if (node.containsReturn) {
//            "__on_defer(__ret_val)"
//        } else {
//            "__on_defer()"
//        }.prependIndent(indent())
    }
}
