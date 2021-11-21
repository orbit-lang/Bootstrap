package org.orbit.backend.codegen.common

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.CodeGeneratorQualifier
import org.orbit.core.Mangler
import org.orbit.core.injectQualified
import org.orbit.core.nodes.BinaryExpressionNode

interface AbstractBinaryExpressionUnit : CodeUnit<BinaryExpressionNode>

class BinaryExpressionUnit(override val node: BinaryExpressionNode, override val depth: Int) : AbstractBinaryExpressionUnit, KoinComponent {
    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory<*> by injectQualified(codeGeneratorQualifier)

    override fun generate(mangler: Mangler): String {
        val left = codeGenFactory.getExpressionUnit(node.left, depth).generate(mangler)
        val right = codeGenFactory.getExpressionUnit(node.right, depth).generate(mangler)

        return "(($left) ${node.operator} ($right))"
    }
}