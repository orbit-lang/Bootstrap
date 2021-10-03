package org.orbit.backend.codegen.swift.units

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.common.AbstractMethodDefUnit
import org.orbit.backend.codegen.common.AbstractModuleUnit
import org.orbit.backend.codegen.common.AbstractTypeAliasUnit
import org.orbit.backend.codegen.common.AbstractTypeDefUnit
import org.orbit.core.*
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.nodes.*
import org.orbit.types.components.Context
import org.orbit.types.components.IntrinsicTypes
import org.orbit.types.components.Type
import org.orbit.util.partial

//class ApiUnit(override val node: ApiDefNode, override val depth: Int) : CodeUnit<ApiDefNode> {
//    override fun generate(mangler: Mangler): String {
//        val traitConstructors
//    }
//}

class ModuleUnit(override val node: ModuleNode, override val depth: Int) : AbstractModuleUnit {
    private companion object : KoinComponent {
        private val context: Context by injectResult(CompilationSchemeEntry.typeSystem)
    }

    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory by injectQualified(codeGeneratorQualifier)

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

        val typeDefs = node.entityDefs
            .filterIsInstance<TypeDefNode>()
            .map(partial(codeGenFactory::getTypeDefUnit, depth))
            .joinToString(newline(2), transform = partial(AbstractTypeDefUnit::generate, mangler))

        val typeAliases = node.typeAliasNodes
            .map(partial(codeGenFactory::getTypeAliasUnit, depth))
            .joinToString(newline(2), transform = partial(AbstractTypeAliasUnit::generate, mangler))

        val monos = context.monomorphisedTypes.values
            .filterNot(Type::isEphemeral)
            .joinToString("\n", transform = partial(TypeDefUnit.Companion::generateMonomorphisedType, mangler))

//        val typeProjections = node.typeProjections
//            .map(partial(::TypeProjectionUnit, depth))
//            .joinToString(newline(2), transform = partial(TypeProjectionUnit::generate, mangler))

        val methodDefs = node.methodDefs
            .map(partial(codeGenFactory::getMethodDefUnit, depth))
            .joinToString(newline(2), transform = partial(AbstractMethodDefUnit::generate, mangler))

        return """
            |$header
            |
            |$typeDefs
            |
            |$typeAliases
            |
            |$monos
            |
            |$methodDefs
        """.trimMargin()
    }
}