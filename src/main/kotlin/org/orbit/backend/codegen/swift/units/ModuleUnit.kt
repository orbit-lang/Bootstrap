package org.orbit.backend.codegen.swift.units

import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.*
import org.orbit.core.nodes.*
import org.orbit.types.components.IntrinsicTypes
import org.orbit.util.partial

//class ApiUnit(override val node: ApiDefNode, override val depth: Int) : CodeUnit<ApiDefNode> {
//    override fun generate(mangler: Mangler): String {
//        val traitConstructors
//    }
//}

class ModuleUnit(override val node: ModuleNode, override val depth: Int) : CodeUnit<ModuleNode> {
    override fun generate(mangler: Mangler): String {
        val moduleName = node.getPath().toString(OrbitMangler)

        // TODO - This is just a temporary hack to allow us to omit arbitrary definitions from code generation
        //  (because they are defined in the OrbCore Swift module)
        val stubAnnotation = node.phaseAnnotationNodes.find {
            val path = it.getPathOrNull() ?: return@find false

            path == IntrinsicTypes.BootstrapCoreStub.path
        }

        if (stubAnnotation != null) {
            return ""
        }

        val header = "/* module $moduleName */"
        //val moduleDef = "class ${node.getPath().toString(mangler)} "

        val traitConstructorDefs = node.entityConstructors
            .filterIsInstance<TraitConstructorNode>()
            .map(partial(::TraitConstructorUnit, depth))
            .joinToString(newline(2), transform = partial(TraitConstructorUnit::generate, mangler))

        val typeDefs = node.entityDefs
            .filterIsInstance<TypeDefNode>()
            .map(partial(::TypeDefUnit, depth))
            .joinToString(newline(2), transform = partial(TypeDefUnit::generate, mangler))

        val traitDefs = node.entityDefs
            .filterIsInstance<TraitDefNode>()
            .map(partial(::TraitDefUnit, depth))
            .joinToString(newline(2), transform = partial(TraitDefUnit::generate, mangler))

        val typeAliases = node.typeAliasNodes
            .map(partial(::TypeAliasUnit, depth))
            .joinToString(newline(2), transform = partial(TypeAliasUnit::generate, mangler))

        val typeProjections = node.typeProjections
            .map(partial(::TypeProjectionUnit, depth))
            .joinToString(newline(2), transform = partial(TypeProjectionUnit::generate, mangler))

        val methodDefs = node.methodDefs
            .map(partial(::MethodDefUnit, depth))
            .joinToString(newline(2), transform = partial(MethodDefUnit::generate, mangler))

        return """
            |$header
            |$traitConstructorDefs
            |
            |$typeDefs
            |
            |$traitDefs
            |
            |$typeAliases
            |
            |$typeProjections
            |
            |$methodDefs
        """.trimMargin()
    }
}