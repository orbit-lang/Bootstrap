package org.orbit.backend.codegen.swift.units

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.common.AbstractCollectionLiteralUnit
import org.orbit.core.CodeGeneratorQualifier
import org.orbit.core.Mangler
import org.orbit.core.injectQualified
import org.orbit.core.nodes.CollectionLiteralNode

class CollectionLiteralUnit(override val node: CollectionLiteralNode, override val depth: Int) : AbstractCollectionLiteralUnit, KoinComponent {
    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory<*> by injectQualified(codeGeneratorQualifier)

    override fun generate(mangler: Mangler) : String {
        val elements = node.elements
            .map { codeGenFactory.getExpressionUnit(it, depth + 1) }
            .joinToString(", ") { it.generate(mangler) }

        return """
            |[${elements}]
        """.trimMargin()
    }
}
