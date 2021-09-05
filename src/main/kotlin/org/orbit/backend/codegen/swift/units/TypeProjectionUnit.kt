package org.orbit.backend.codegen.swift.units

import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.Mangler
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.TypeProjectionNode
import org.orbit.util.partial

class TypeProjectionUnit(override val node: TypeProjectionNode, override val depth: Int) :
    CodeUnit<TypeProjectionNode> {
    override fun generate(mangler: Mangler): String {
        val typePath = node.typeIdentifier.getPath()
        val traitPath = node.traitIdentifier.getPath()

        val header = "/* type projection ${typePath.toString(OrbitMangler)} : ${traitPath.toString(OrbitMangler)} */"
        val clauses = node.whereNodes
            .map(partial(::ProjectionWhereClauseUnit, depth + 1))
            .joinToString(newline(), transform = partial(ProjectionWhereClauseUnit::generate, mangler))

        return """
            |$header
            |extension ${typePath.toString(mangler)} : ${traitPath.toString(mangler)} {
            |$clauses
            |}
        """.trimMargin()
            .prependIndent(indent())
    }
}