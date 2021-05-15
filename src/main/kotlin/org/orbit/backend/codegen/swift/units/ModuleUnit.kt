package org.orbit.backend.codegen.swift.units

import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.*
import org.orbit.core.nodes.MethodDefNode
import org.orbit.core.nodes.ModuleNode
import org.orbit.core.nodes.TypeDefNode
import org.orbit.frontend.Parser
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

        val methodDefs = node.methodDefs
            .map(partial(::MethodDefUnit, depth))
            .map(partial(MethodDefUnit::generate, mangler))
            .joinToString(newline())

        return """
            |$header
            |$typeDefs
            |$methodDefs
        """.trimMargin()
    }
}