package org.orbit.backend.codegen.swift.units

import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.Mangler
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.ModuleNode
import org.orbit.core.nodes.TypeDefNode
import org.orbit.util.partial

class ModuleUnit(override val node: ModuleNode, override val depth: Int) : CodeUnit<ModuleNode> {
    override fun generate(mangler: Mangler): String {
        val moduleName = node.getPath().toString(OrbitMangler)

        if (moduleName == "Orb::Types::Intrinsics") {
            return ""
        }

        val header = "/* module $moduleName */"
        //val moduleDef = "class ${node.getPath().toString(mangler)} "

        val typeDefs = node.entityDefs
            .filterIsInstance<TypeDefNode>()
            .map(partial(::TypeDefUnit, depth))
            .map(partial(TypeDefUnit::generate, mangler))
            .joinToString(newline())

        return """
            |$header
            |$typeDefs
        """.trimMargin()
    }
}