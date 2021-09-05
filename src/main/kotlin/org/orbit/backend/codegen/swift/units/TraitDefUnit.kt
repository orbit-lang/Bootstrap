package org.orbit.backend.codegen.swift.units

import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.Mangler
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.TraitDefNode
import org.orbit.util.partial

class TraitDefUnit(override val node: TraitDefNode, override val depth: Int) : CodeUnit<TraitDefNode> {
    override fun generate(mangler: Mangler): String {
        val traitPath = node.getPath()

        val header = "/* trait ${traitPath.toString(OrbitMangler)} */"
        val traitDef = "protocol ${traitPath.toString(mangler)}"

        val propertyDefs = node.propertyPairs
            .map(partial(::PropertyDefUnit, depth + 2, true))
            .joinToString(newline(), transform = partial(PropertyDefUnit::generate, mangler))

        return """
            |$header
            |$traitDef {
            |$propertyDefs
            |}
        """.trimMargin()
            .prependIndent(indent())
    }
}