package org.orbit.types.typeresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.TraitDefNode
import org.orbit.core.nodes.TypeAliasNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.extensions.annotate
import org.orbit.types.components.*
import org.orbit.types.phase.TypeChecker
import org.orbit.util.Invocation

class TypeAliasTypeResolver(override val node: TypeAliasNode, override val binding: Binding) : TypeResolver<TypeAliasNode, TypeAlias>, KoinComponent {
    override val invocation: Invocation by inject()

    constructor(pair: Pair<TypeAliasNode, Binding>) : this(pair.first, pair.second)

    override fun resolve(environment: Environment, context: Context) : TypeAlias {
        val targetType = context.getTypeByPath(node.targetTypeIdentifier.getPath())

        return when (targetType) {
            is Type -> TypeAlias(node.sourceTypeIdentifier.getPath().toString(OrbitMangler), targetType)
            is TypeConstructor -> {
                val metaType = MetaType(targetType, targetType.typeParameters as List<ValuePositionType>)

                val nType = metaType.evaluate(context) as? Type
                    ?: throw invocation.make<TypeChecker>("Attempting to create a type alias '${node.sourceTypeIdentifier.value}' to a constructed type derived from entity constructor '${targetType.name}'", node.targetTypeIdentifier)

                TypeAlias(node.sourceTypeIdentifier.getPath().toString(OrbitMangler), nType)
            }

            is TraitConstructor -> throw invocation.make<TypeChecker>("Attempted to create a type alias '${node.sourceTypeIdentifier.value}' to a constructed trait. Use `trait ${node.sourceTypeIdentifier.value} = ${node.targetTypeIdentifier.value}<${
                targetType.typeParameters.joinToString(", ") { it.name }}>` instead", node.targetTypeIdentifier)

            else -> TODO("???")
        }
    }
}

class TraitDefTypeResolver(override val node: TraitDefNode, override val binding: Binding) : EntityTypeResolver<TraitDefNode, Trait>,
    KoinComponent {
    override val invocation: Invocation by inject()

    constructor(pair: Pair<TraitDefNode, Binding>) : this(pair.first, pair.second)

    override fun resolve(environment: Environment, context: Context): Trait {
        var trait = Trait(node.getPath(), emptyList())

        val members = mutableListOf<Property>()
        for (propertyPair in node.propertyPairs) {
            val propertyType = context.getTypeByPath(propertyPair.getPath())

            if (propertyType == trait) {
                throw invocation.make<TypeChecker>("Traits must not declare properties of their own type: Found property (${propertyPair.identifierNode.identifier} ${propertyType.name}) in trait ${trait.name}", propertyPair.typeExpressionNode)
            }

            if (propertyType is Entity) {
                val cyclicProperties = propertyType.properties.filter { it.type == trait }

                if (cyclicProperties.isNotEmpty()) {
                    throw invocation.make<TypeChecker>("Detected cyclic definition between trait '${trait.name}' and its property (${propertyPair.identifierNode.identifier} ${propertyType.name})", propertyPair.typeExpressionNode)
                }
            }

            members.add(Property(propertyPair.identifierNode.identifier, propertyType))
        }

        trait = Trait(node.getPath(), properties = members)

        node.annotate(trait, Annotations.Type)

        return trait
    }
}