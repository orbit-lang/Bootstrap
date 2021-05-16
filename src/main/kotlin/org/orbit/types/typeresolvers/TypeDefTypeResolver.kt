package org.orbit.types.typeresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.getPath
import org.orbit.core.nodes.TraitDefNode
import org.orbit.core.nodes.TypeDefNode
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.types.components.*
import org.orbit.types.phase.TypeChecker
import org.orbit.util.Invocation

class TraitDefTypeResolver(override val node: TraitDefNode, override val binding: Binding) : EntityTypeResolver<TraitDefNode, Trait>, KoinComponent {
    private val invocation: Invocation by inject()

    constructor(pair: Pair<TraitDefNode, Binding>) : this(pair.first, pair.second)

    override fun resolve(environment: Environment, context: Context): Trait {
        return Trait(Path.empty)
    }
}

class TypeDefTypeResolver(override val node: TypeDefNode, override val binding: Binding) : EntityTypeResolver<TypeDefNode, Type>, KoinComponent {
    private val invocation: Invocation by inject()

    constructor(pair: Pair<TypeDefNode, Binding>) : this(pair.first, pair.second)

    override fun resolve(environment: Environment, context: Context): Type {
        var type = Type(node.getPath(), emptyList())

        val members = mutableListOf<Property>()
        for (propertyPair in node.propertyPairs) {
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

        type = Type(node.getPath(), members)

        //context.add(type)

        return type
    }
}