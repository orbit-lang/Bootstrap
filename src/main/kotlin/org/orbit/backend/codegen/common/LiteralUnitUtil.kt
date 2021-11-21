package org.orbit.backend.codegen.common

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.core.CodeGeneratorQualifier
import org.orbit.core.injectQualified
import org.orbit.core.nodes.IntLiteralNode
import org.orbit.core.nodes.LiteralNode
import org.orbit.core.nodes.SymbolLiteralNode
import org.orbit.core.nodes.TypeIdentifierNode
import java.math.BigInteger

object LiteralUnitUtil : KoinComponent {
    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory<*> by injectQualified(codeGeneratorQualifier)

    fun <T> generateLiteralUnit(node: LiteralNode<T>, depth: Int) : AbstractLiteralUnit<T> = when (node) {
        is IntLiteralNode -> codeGenFactory.getIntLiteralUnit(node as LiteralNode<Pair<Int, BigInteger>>, depth) as AbstractLiteralUnit<T>
        is SymbolLiteralNode -> codeGenFactory.getSymbolLiteralUnit(node, depth) as AbstractLiteralUnit<T>
        is TypeIdentifierNode -> codeGenFactory.getTypeLiteralUnit(node, depth) as AbstractLiteralUnit<T>
        else -> TODO("@LiteralUnitUtil:19")
    }
}