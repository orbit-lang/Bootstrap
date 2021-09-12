package org.orbit.backend.codegen.swift.units

import org.koin.core.component.KoinComponent
import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.*
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.nodes.TypeDefNode
import org.orbit.types.components.Context
import org.orbit.types.components.Property
import org.orbit.types.components.Type
import org.orbit.util.partial

class TypeDefUnit(override val node: TypeDefNode, override val depth: Int) : CodeUnit<TypeDefNode>, KoinComponent {
    private val context: Context by injectResult(CompilationSchemeEntry.typeSystem)

    companion object {
        fun generateMonomorphisedType(type: Type, mangler: Mangler) : String {
            val nMangler = (OrbitMangler + mangler)

            val properties = type.properties.map {
                val header = "/* ${it.name} ${it.type.name} */"
                val swift = "let ${it.name}: ${nMangler.invoke(it.type.name)}"

                """
                $header
                $swift
                """
            }.joinToString("\n\t\t")

            val header = "/* type ${type.name} */"

            return """
            $header
            struct ${nMangler.invoke(type.name)} {
                $properties
            }
            """.trimIndent()
        }
    }

    private fun generateProperty(property: Property, mangler: Mangler) : String {
        val propertyType = (OrbitMangler + mangler).invoke(property.type.name)
        val defaultValue = when (property.defaultValue) {
            null -> ""
            else -> " = " + ExpressionUnit(property.defaultValue!!, depth).generate(mangler)
        }

        return "${property.name}: $propertyType$defaultValue"
    }

    private fun generatePropertyLet(property: Property, mangler: Mangler) : String {
        val propertyType = (OrbitMangler + mangler).invoke(property.type.name)

        val header = "/* ${property.name} $propertyType */"
        val prop = "let ${property.name}: $propertyType"

        return """
            |$header
            |$prop
        """.trimMargin()
            .prependIndent(indent(depth + 1))
    }

    private fun generateInitialiser(properties: List<Property>, mangler: Mangler) : String {
        // NOTE - Swift's auto-generated constructors can't handle default values!
        val arguments = properties.joinToString(", ", transform = partial(::generateProperty, mangler))
        val assignments = properties.joinToString("\n\t") {
            "\tself.${it.name} = ${it.name}"
        }

        return """
            |init($arguments) {
            |$assignments
            |}
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
            .joinToString("\n${indent()}") { generatePropertyLet(it, mangler) }

        val init = generateInitialiser(type.properties, mangler)

        return """
            |$header
            |$typeDef {
            |$propertyDefs
            |
            |$init
            |}
        """.trimMargin().prependIndent(indent())
    }
}

