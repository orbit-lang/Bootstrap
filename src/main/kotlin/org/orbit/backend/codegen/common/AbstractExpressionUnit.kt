package org.orbit.backend.codegen.common

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.CodeGeneratorQualifier
import org.orbit.core.Mangler
import org.orbit.core.injectQualified
import org.orbit.core.nodes.*

interface AbstractExpressionUnit : CodeUnit<ExpressionNode>

class ExpressionUnit(override val node: ExpressionNode, override val depth: Int) : AbstractExpressionUnit, KoinComponent {
    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory<*> by injectQualified(codeGeneratorQualifier)

    override fun generate(mangler: Mangler): String = when (node) {
        is LiteralNode<*> ->
            LiteralUnitUtil.generateLiteralUnit(node, depth).generate(mangler)
        is RValueNode ->
            codeGenFactory.getRValueUnit(node, depth).generate(mangler)
        is ConstructorNode ->
            codeGenFactory.getConstructorUnit(node, depth).generate(mangler)
        is IdentifierNode ->
            codeGenFactory.getIdentifierUnit(node, depth).generate(mangler)
        is CallNode ->
            codeGenFactory.getCallUnit(node, depth).generate(mangler)
        is BinaryExpressionNode ->
            codeGenFactory.getBinaryExpressionUnit(node, depth).generate(mangler)
        is UnaryExpressionNode ->
            codeGenFactory.getUnaryExpressionUnit(node, depth).generate(mangler)
        is CollectionLiteralNode ->
                codeGenFactory.getCollectionLiteralUnit(node, depth).generate(mangler)

        else -> TODO("@ExpressionUnit:32")
    }
}