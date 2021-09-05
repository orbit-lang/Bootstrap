package org.orbit.backend.codegen.swift.units

import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.Mangler
import org.orbit.core.OrbitMangler
import org.orbit.core.getType
import org.orbit.core.nodes.AssignmentStatementNode
import org.orbit.core.plus

class PropertyProjectionClauseUnit(override val node: AssignmentStatementNode, override val depth: Int) :
    CodeUnit<AssignmentStatementNode> {
    override fun generate(mangler: Mangler): String {
        val rhsUnit = ExpressionUnit(node.value, depth)
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