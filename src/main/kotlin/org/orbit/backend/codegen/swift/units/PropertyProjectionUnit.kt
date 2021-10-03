package org.orbit.backend.codegen.swift.units

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.common.AbstractPropertyProjectionUnit
import org.orbit.core.*
import org.orbit.core.nodes.AssignmentStatementNode

class PropertyProjectionUnit(override val node: AssignmentStatementNode, override val depth: Int) : AbstractPropertyProjectionUnit, KoinComponent {
    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory by injectQualified(codeGeneratorQualifier)

    override fun generate(mangler: Mangler): String {
        val rhsUnit = codeGenFactory.getExpressionUnit(node.value, depth)
        val rhsType = node.value.getType()
        val rhsTypeName = (OrbitMangler + mangler).invoke(rhsType.name)
        val rhsValue = rhsUnit.generate(mangler)
        val propertyName = node.identifier.identifier

        val header = "/* where propertyName = $rhsValue */"

        return """
            |$header
            |var $propertyName: $rhsTypeName {
            |   return $rhsValue
            |}
        """.trimMargin()
            .prependIndent(indent())
    }
}