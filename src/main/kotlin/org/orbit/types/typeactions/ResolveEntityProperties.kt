package org.orbit.types.typeactions

import org.orbit.core.getPath
import org.orbit.core.nodes.EntityDefNode
import org.orbit.core.nodes.TraitDefNode
import org.orbit.core.nodes.TypeDefNode
import org.orbit.types.components.*
import org.orbit.util.Printer

class ResolveEntityProperties<N: EntityDefNode, E: Entity>(private val node: N) : TypeAction {
    private var properties: List<Property> = emptyList()

    override fun execute(context: Context) {
        val stub = context.getTypeByPath(node.getPath())
        val propertyTypes = node.propertyPairs.map {
            val pType = context.getTypeByPath(it.typeExpressionNode.getPath())

            Property(it.identifierNode.identifier, pType)
        }

        properties = propertyTypes

        val nType = when (node) {
            is TypeDefNode -> Type(stub.name, properties = propertyTypes)
            is TraitDefNode -> Trait(stub.name, properties = propertyTypes)
            else -> TODO("Unreachable")
        }

        // Update the type definition
        context.remove(stub.name)
        context.add(nType)
    }

    override fun describe(printer: Printer): String {
        return """
            Resolve properties for entity ${node.getPath().toString(printer)}
                    ${properties.joinToString(", ", transform = { it.toString(printer) })}
        """.trimIndent()
    }
}