package org.orbit.backend.codegen.c.units

import org.orbit.backend.codegen.common.AbstractDeferCallUnit
import org.orbit.core.Mangler
import org.orbit.core.nodes.DeferNode

class DeferCallUnit(override val node: DeferNode, override val depth: Int) : AbstractDeferCallUnit {
    override fun generate(mangler: Mangler): String {
//        val deferFunc = node.getAnnotation<StringKey>(Annotations.DeferFunction)
//            ?.value
//            ?: return ""

        return ""

//        return if (node.containsReturn) {
//            "__on_defer(__ret_val);"
//        } else {
//            "__on_defer();"
//        }.prependIndent(indent())
    }
}