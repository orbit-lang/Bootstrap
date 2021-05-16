package org.orbit.types.typeresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.getPath
import org.orbit.core.nodes.TypeDefNode
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.types.components.*
import org.orbit.types.phase.TypeChecker
import org.orbit.util.Invocation

class TypeDefTypeResolver(private val typeDefNode: TypeDefNode) : TypeResolver, KoinComponent {
    private val invocation: Invocation by inject()

    override fun resolve(environment: Environment, context: Context, binding: Binding): TypeProtocol {
        var type = Type(typeDefNode.getPath(), emptyList())

        val members = mutableListOf<Property>()
        for (propertyPair in typeDefNode.propertyPairs) {
            val propertyType = context.getType(propertyPair.getPath())

            if (propertyType == type) {
                throw invocation.make<TypeChecker>("Types must not declare properties of their own type: Found property (${propertyPair.identifierNode.identifier} ${propertyType.name}) in type ${type.name}", propertyPair.typeIdentifierNode)
            }

            if (propertyType is Entity) {
                val cyclicProperties = propertyType.properties.filter { it.type == type }

                if (cyclicProperties.isNotEmpty()) {
                    throw invocation.make<TypeChecker>("Detected cyclic definition between type '${type.name}' and its property (${propertyPair.identifierNode.identifier} ${propertyType.name})", propertyPair.typeIdentifierNode)
                }
            }

            members.add(Property(propertyPair.identifierNode.identifier, propertyType))
        }

        type = Type(typeDefNode.getPath(), members)

        context.add(type)

        return type
    }
}