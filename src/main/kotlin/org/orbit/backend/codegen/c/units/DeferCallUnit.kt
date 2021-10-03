package org.orbit.backend.codegen.c.units

import org.orbit.backend.codegen.CodeUnit
import org.orbit.backend.codegen.common.AbstractDeferCallUnit
import org.orbit.core.Mangler
import org.orbit.core.nodes.BlockNode

class DeferCallUnit(override val node: BlockNode, override val depth: Int) : AbstractDeferCallUnit {
    override fun generate(mangler: Mangler): String {
        if (!node.containsDefer) return ""
        if (node.containsReturn) return ""

        return if (node.containsReturn) {
            "__on_defer(__ret_val);"
        } else {
            "__on_defer();"
        }.prependIndent(indent())
    }
}