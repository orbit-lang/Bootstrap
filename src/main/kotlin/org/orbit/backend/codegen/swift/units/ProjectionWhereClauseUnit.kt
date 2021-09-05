package org.orbit.backend.codegen.swift.units

import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.Mangler
import org.orbit.core.nodes.AssignmentStatementNode
import org.orbit.core.nodes.WhereClauseNode

class ProjectionWhereClauseUnit(override val node: WhereClauseNode, override val depth: Int) :
    CodeUnit<WhereClauseNode> {
    override fun generate(mangler: Mangler): String {
        val clauseUnit = when (node.whereStatement) {
            is AssignmentStatementNode -> PropertyProjectionClauseUnit(node.whereStatement, depth)
            else -> TODO("ProjectionWhereClause")
        }

        return clauseUnit.generate(mangler)
    }
}