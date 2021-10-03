package org.orbit.backend.codegen.swift.units

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.common.AbstractProjectionWhereClauseUnit
import org.orbit.core.CodeGeneratorQualifier
import org.orbit.core.Mangler
import org.orbit.core.injectQualified
import org.orbit.core.nodes.AssignmentStatementNode
import org.orbit.core.nodes.WhereClauseNode

class ProjectionWhereClauseUnit(override val node: WhereClauseNode, override val depth: Int) : AbstractProjectionWhereClauseUnit, KoinComponent {
    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory by injectQualified(codeGeneratorQualifier)

    override fun generate(mangler: Mangler): String {
        val clauseUnit = when (node.whereStatement) {
            is AssignmentStatementNode -> codeGenFactory.getPropertyProjectionUnit(node.whereStatement, depth)
            else -> TODO("ProjectionWhereClause")
        }

        return clauseUnit.generate(mangler)
    }
}