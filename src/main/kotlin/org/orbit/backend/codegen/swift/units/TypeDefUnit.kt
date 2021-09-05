package org.orbit.backend.codegen.swift.units

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.*
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.nodes.TypeDefNode
import org.orbit.types.components.Context
import org.orbit.types.components.Property
import org.orbit.types.components.Type
import org.orbit.types.components.TypeProtocol
import org.orbit.util.ASTUtil
import org.orbit.util.partial

class TypeDefUnit(override val node: TypeDefNode, override val depth: Int) : CodeUnit<TypeDefNode>, KoinComponent {
    private val context: Context by injectResult(CompilationSchemeEntry.typeSystem)

    private fun generateProperty(property: Property, mangler: Mangler) : String {
        val propertyType = (OrbitMangler + mangler).invoke(property.type.name)
        val defaultValue = when (property.defaultValue) {
            null -> ""
            else -> " = " + ExpressionUnit(property.defaultValue!!, depth).generate(mangler)
        }

        val header = "/* ${property.name} $propertyType$defaultValue */"
        val prop = "let ${property.name}: $propertyType$defaultValue"

        return """
            |$header
            |$prop
        """.trimMargin()
            .prependIndent(indent(depth + 1))
    }

    override fun generate(mangler: Mangler) : String {
        val typePath = node.getPath()
        val type = context.getTypeByPath(typePath) as Type

        // TODO - Lookup value semantics for this type (i.e. class or struct)
        val header = "/* type ${typePath.toString(OrbitMangler)} */"

//        var adoptedProtocols = node.traitConformances
//            .map(partial(::TypeExpressionUnit, depth))
//            .joinToString(", ", transform = partial(TypeExpressionUnit::generate, mangler))
//
//        if (node.traitConformances.isNotEmpty()) {
//            adoptedProtocols = " : $adoptedProtocols"
//        }

        val typeDef = "struct ${typePath.toString(mangler)}"

        val propertyDefs = type.properties
            .joinToString("\n${indent()}") { generateProperty(it, mangler) }

        return """
            |$header
            |$typeDef {
            |$propertyDefs
            |}
        """.trimMargin().prependIndent(indent())
    }
}

