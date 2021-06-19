package org.orbit.types.typeresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.getPath
import org.orbit.core.nodes.TraitConstructorNode
import org.orbit.core.nodes.TypeConstructorNode
import org.orbit.core.nodes.TypeDefNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.extensions.annotate
import org.orbit.types.components.*
import org.orbit.types.phase.TypeChecker
import org.orbit.util.Invocation

class TypeConstructorTypeResolver(override val node: TypeConstructorNode, override val binding: Binding) : TypeResolver<TypeConstructorNode, TypeConstructor>, KoinComponent {
    override val invocation: Invocation by inject()

    constructor(pair: Pair<TypeConstructorNode, Binding>) : this(pair.first, pair.second)

    override fun resolve(environment: Environment, context: Context): TypeConstructor {
        val typeParameters = node.typeParameterNodes.map { TypeParameter(it.value) }

        // TODO - Properties (construct new, temporary Context containing type parameters)

        return TypeConstructor(node.getPath(), typeParameters)
    }
}

class TraitConstructorTypeResolver(override val node: TraitConstructorNode, override val binding: Binding) : TypeResolver<TraitConstructorNode, TraitConstructor>, KoinComponent {
    override val invocation: Invocation by inject()

    constructor(pair: Pair<TraitConstructorNode, Binding>) : this(pair.first, pair.second)

    override fun resolve(environment: Environment, context: Context): TraitConstructor {
        val typeParameters = node.typeParameterNodes.map { TypeParameter(it.value) }

        // TODO - Properties & signatures (construct new, temporary Context containing type parameters)

        val type = TraitConstructor(node.getPath(), typeParameters)

        node.annotate(type, Annotations.Type)

        return type
    }
}

class TypeDefTypeResolver(override val node: TypeDefNode, override val binding: Binding) : EntityTypeResolver<TypeDefNode, Type>, KoinComponent {
    override val invocation: Invocation by inject()

    constructor(pair: Pair<TypeDefNode, Binding>) : this(pair.first, pair.second)

    override fun resolve(environment: Environment, context: Context): Type {
        var type = Type(node.getPath(), emptyList(), isRequired = node.isRequired)

        val members = mutableListOf<Property>()
        for (propertyPair in node.propertyPairs) {
            val propertyType = context.getTypeByPath(propertyPair.getPath())

            if (propertyType == type) {
                throw invocation.make<TypeChecker>("Types must not declare properties of their own type: Found property (${propertyPair.identifierNode.identifier} ${propertyType.name}) in type ${type.name}", propertyPair.typeExpressionNode)
            }

            if (propertyType is Entity) {
                val cyclicProperties = propertyType.properties.filter { it.type == type }

                if (cyclicProperties.isNotEmpty()) {
                    throw invocation.make<TypeChecker>("Detected cyclic definition between type '${type.name}' and its property (${propertyPair.identifierNode.identifier} ${propertyType.name})", propertyPair.typeExpressionNode)
                }
            }

            members.add(Property(propertyPair.identifierNode.identifier, propertyType))
        }

        type = Type(node.getPath(), properties = members, isRequired = node.isRequired)

        node.annotate(type, Annotations.Type)

        return type
    }
}

