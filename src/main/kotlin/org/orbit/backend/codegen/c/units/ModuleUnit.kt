package org.orbit.backend.codegen.c.units

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.common.*
import org.orbit.core.*
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.nodes.*
import org.orbit.types.components.Context
import org.orbit.types.components.IntrinsicTypes
import org.orbit.types.components.Type
import org.orbit.util.partial
import java.util.*

class CHeader : AbstractHeader {
    private val typedefs = mutableListOf<String>()

    fun add(typedef: String) {
        typedefs.add(typedef)
    }

    override fun generate() : String {
        return """
            |#pragma once
            |#include <Orb/OrbCore.h>
            |
            |${typedefs.joinToString("\n")}
        """.trimMargin()
    }
}

//object TypeDefSort {
//    fun sort(nodes: List<TypeDefNode>) : List<TypeDefNode> {
//        for (node in nodes) {
//            val propertyNames = node.propertyPairs.map { it. }
//        }
//    }
//}

class ModuleUnit(override val node: ModuleNode, override val depth: Int, private val cHeader: CHeader) : AbstractModuleUnit {
    private companion object : KoinComponent {
        private val context: Context by injectResult(CompilationSchemeEntry.typeSystem)
    }

    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory<CHeader> by injectQualified(codeGeneratorQualifier)

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

        val header = "/* module $moduleName */"

        node.entityDefs
            .filterIsInstance<TypeDefNode>()
            .map(partial(codeGenFactory::getTypeDefUnit, depth))
            .map(partial(AbstractTypeDefUnit::generate, mangler))
            .forEach(cHeader::add)

        val typeAliases = node.typeAliasNodes
            .map(partial(codeGenFactory::getTypeAliasUnit, depth))
            .joinToString(newline(2), transform = partial(AbstractTypeAliasUnit::generate, mangler))

        context.monomorphisedTypes.values
            .filterNot(Type::isEphemeral)
            .map(partial(TypeDefUnit.Companion::generateMonomorphisedType, mangler))
            .forEach(cHeader::add)
//            .joinToString("\n", transform = partial(TypeDefUnit.Companion::generateMonomorphisedType, mangler))

        val deferFuncs = node.search(DeferNode::class.java)
            .mapIndexed { idx, item -> DeferFunctionUnit(item, depth, idx) }
            .joinToString("\n", transform = partial(DeferFunctionUnit::generate, mangler))

        val methodDefs = node.methodDefs
            .map(partial(codeGenFactory::getMethodDefUnit, depth))
            .joinToString(newline(2), transform = partial(AbstractMethodDefUnit::generate, mangler))

        return """
            |$header
            |$typeAliases
            |$deferFuncs
            |$methodDefs
        """.trimMargin()
    }
}