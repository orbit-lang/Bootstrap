package org.orbit.backend.codegen.swift.units

import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.*
import org.orbit.core.nodes.ModuleNode
import org.orbit.core.nodes.TraitDefNode
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
            .joinToString(newline(), transform = partial(TypeDefUnit::generate, mangler))

        val traitDefs = node.entityDefs
            .filterIsInstance<TraitDefNode>()
            .map(partial(::TraitDefUnit, depth))
            .joinToString(newline(), transform = partial(TraitDefUnit::generate, mangler))

        val methodDefs = node.methodDefs
            .map(partial(::MethodDefUnit, depth))
            .joinToString(newline(), transform = partial(MethodDefUnit::generate, mangler))

        return """
            |$header
            |$typeDefs
            |$traitDefs
            |$methodDefs
        """.trimMargin()
    }
}