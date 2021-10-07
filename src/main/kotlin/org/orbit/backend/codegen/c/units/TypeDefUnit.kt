package org.orbit.backend.codegen.c.units

import org.koin.core.component.KoinComponent
import org.orbit.backend.codegen.common.AbstractTypeDefUnit
import org.orbit.core.*
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.nodes.TypeDefNode
import org.orbit.types.components.Context
import org.orbit.types.components.Property
import org.orbit.types.components.Type

class TypeDefUnit(override val node: TypeDefNode, override val depth: Int) : AbstractTypeDefUnit, KoinComponent {
    private val context: Context by injectResult(CompilationSchemeEntry.typeSystem)

    companion object {
        fun generateMonomorphisedType(type: Type, mangler: Mangler) : String {
            val nMangler = (OrbitMangler + mangler)

            val properties = type.properties.joinToString("\n\t\t") {
                val typeName = nMangler(it.type.name)
                val header = "/* ${it.name} ${it.type.name} */"
                val c = "$typeName ${it.name};"

                """
                $header
                $c
                """
            }

            val header = "/* type ${type.name} */"

            return """
            $header
            typedef struct __${nMangler.invoke(type.name).lowercase()} {
                $properties
            } ${nMangler.invoke(type.name)};
            """.trimIndent()
        }
    }

    private fun generatePropertyLet(property: Property, mangler: Mangler) : String {
        val propertyType = (OrbitMangler + mangler).invoke(property.type.name)

        val header = "/* ${property.name} ${property.type.name} */"
        val prop = "$propertyType ${property.name};"

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

        val typeDef = "struct ${typePath.toString(mangler)}"

        val propertyDefs = type.properties
            .joinToString("\n${indent()}") { generatePropertyLet(it, mangler) }

        return """
            |$header
            |$typeDef {
            |$propertyDefs
            |};
        """.trimMargin().prependIndent(indent())
    }
}