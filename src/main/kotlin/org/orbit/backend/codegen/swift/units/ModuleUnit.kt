package org.orbit.backend.codegen.swift.units

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.common.*
import org.orbit.core.*
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.nodes.*
import org.orbit.types.components.*
import org.orbit.util.partial

object SwiftHeader : AbstractHeader {
    override fun generate(): String = ""
}

data class SyntheticTraitUnit(private val trait: Trait) {
    fun generate(mangler: Mangler) : String {
        val nMangler = (OrbitMangler + mangler)
        val header = "/* synthetic trait ${OrbitMangler.mangle(trait.getFullyQualifiedPath())} */"

        return """
            |$header
            |protocol ${nMangler(trait.name)} {
            |}
        """.trimMargin()
    }
}

data class SyntheticPropertyUnit(private val property: Property) {
    fun generate(mangler: Mangler) : String {
        val nMangler = (OrbitMangler + mangler)
        val header = "/* synthetic ${property.name} ${OrbitMangler.mangle(property.type.getFullyQualifiedPath())} */"

        return """
            |$header
            |var ${property.name}: ${nMangler(property.type.name)} { get }
        """.trimMargin()
    }
}

data class SyntheticTypeUnit(private val type: Type) {
    fun generate(mangler: Mangler) : String {
        val typePath = type.getFullyQualifiedPath()
        val header = "/* synthetic type ${OrbitMangler.mangle(type.getFullyQualifiedPath())} */"
        val properties = type.properties.map(::SyntheticPropertyUnit)
            .joinToString("\n") { it.generate(mangler) }

        return """
            |$header
            |protocol ${mangler.mangle(typePath)} {
            |$properties
            |}
        """.trimMargin()
    }
}

class ModuleUnit(override val node: ModuleNode, override val depth: Int) : AbstractModuleUnit {
    private companion object : KoinComponent {
        private val context: Context by injectResult(CompilationSchemeEntry.typeSystem)
    }

    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory<SwiftHeader> by injectQualified(codeGeneratorQualifier)

    override fun generate(mangler: Mangler): String {
        val moduleName = node.getPath().toString(OrbitMangler)

        // TODO - This is just a temporary hack to allow us to omit arbitrary definitions from code generation
        //  (because they are defined in the OrbCore Swift module)
        val stubAnnotation = node.phaseAnnotationNodes.find {
            val path = it.getPathOrNull() ?: return@find false

            path == IntrinsicTypes.CodeGenOmit.path
        }

        if (stubAnnotation != null) {
            return ""
        }

        if (node.isEmpty && context.monomorphisedTypes.isEmpty()) {
            return ""
        }

        val header = "/* module $moduleName */"

        val typeDefs = node.entityDefs
            .filterIsInstance<TypeDefNode>()
            .map(partial(codeGenFactory::getTypeDefUnit, depth))
            .joinToString(newline(2), transform = partial(AbstractTypeDefUnit::generate, mangler))

        val typeAliases = node.typeAliasNodes
            .map(partial(codeGenFactory::getTypeAliasUnit, depth))
            .joinToString(newline(2), transform = partial(AbstractTypeAliasUnit::generate, mangler))

        val monos = context.monomorphisedTypes.values
            .filterNot(Type::isEphemeral)
            // TODO - Each Module should have its own context!
            .filter { it.name.startsWith(node.getPath().toString(OrbitMangler)) }
            .joinToString("\n", transform = partial(TypeDefUnit.Companion::generateMonomorphisedType, mangler))

        val syntheticTraits = context.syntheticTraits
            .filter { it.name.startsWith(node.getPath().toString(OrbitMangler)) }
            .map(::SyntheticTraitUnit)
            .joinToString("\n") { it.generate(mangler) }

        val syntheticTypes = context.syntheticTypes
            .filter { it.name.startsWith(node.getPath().toString(OrbitMangler)) }
            .map(::SyntheticTypeUnit)
            .joinToString("\n") { it.generate(mangler) }

        val intrinsicTypeAliases = context.intrinsicTypeAliases.map {
                val sType = (OrbitMangler + mangler).invoke(it.key.name)
                val eType = (OrbitMangler + mangler).invoke(it.value.name)

                "typealias $sType = [$eType]"
            }.joinToString("\n")

        val monoMethods = context.specialisedMethods.values.joinToString("\n") { spec ->
            val nSignature = spec.signature.toNode(context)
            val sig = codeGenFactory.getMethodSignatureUnit(nSignature, depth)
                .generate(mangler)

            val body = codeGenFactory.getBlockUnit(spec.body, depth, false, true)
                .generate(mangler)

            """
                |$sig
                |$body
            """.trimMargin()
        }

        val extMethods = context.specialisedExtensionMethods.values.joinToString("\n") { spec ->
            val nSignature = spec.signature.toNode(context)
            val sig = codeGenFactory.getMethodSignatureUnit(nSignature, depth)
                .generate(mangler)

            val body = codeGenFactory.getBlockUnit(spec.body, depth, false, true)
                .generate(mangler)

            """
                |$sig
                |$body
            """.trimMargin()
        }

        val methodDefs = node.methodDefs
            .map(partial(codeGenFactory::getMethodDefUnit, depth))
            .joinToString(newline(2), transform = partial(AbstractMethodDefUnit::generate, mangler))

        return """
            |$header
            |$typeDefs
            |$typeAliases
            |$intrinsicTypeAliases
            |$monos
            |$extMethods
            |$methodDefs
            |$monoMethods
        """.trimMargin().trim()
    }
}