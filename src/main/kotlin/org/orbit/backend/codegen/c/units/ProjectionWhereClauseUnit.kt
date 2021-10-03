package org.orbit.backend.codegen.c.units

import org.orbit.backend.codegen.common.AbstractProjectionWhereClauseUnit
import org.orbit.core.Mangler
import org.orbit.core.nodes.WhereClauseNode

class ProjectionWhereClauseUnit(override val node: WhereClauseNode, override val depth: Int) : AbstractProjectionWhereClauseUnit {
    override fun generate(mangler: Mangler): String {
        // TODO
        return ""
    }
}