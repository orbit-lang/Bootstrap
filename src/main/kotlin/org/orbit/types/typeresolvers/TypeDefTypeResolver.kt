package org.orbit.types.typeresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.*
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.nodes.Node
import org.orbit.core.nodes.TraitDefNode
import org.orbit.core.nodes.TypeDefNode
import org.orbit.frontend.phase.Parser
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Scope
import org.orbit.graph.extensions.annotate
import org.orbit.types.components.*
import org.orbit.types.phase.TypeChecker
import org.orbit.util.Invocation
import kotlin.math.sign

class TraitDefTypeResolver(override val node: TraitDefNode, override val binding: Binding) : EntityTypeResolver<TraitDefNode, Trait>, KoinComponent {
    private val invocation: Invocation by inject()

    constructor(pair: Pair<TraitDefNode, Binding>) : this(pair.first, pair.second)

    override fun resolve(environment: Environment, context: Context): Trait {
        var trait = Trait(node.getPath(), emptyList())

        val members = mutableListOf<Property>()
        for (propertyPair in node.propertyPairs) {
            val propertyType = context.getType(propertyPair.getPath())

            if (propertyType == trait) {
                throw invocation.make<TypeChecker>("Traits must not declare properties of their own type: Found property (${propertyPair.identifierNode.identifier} ${propertyType.name}) in trait ${trait.name}", propertyPair.typeIdentifierNode)
            }

            if (propertyType is Entity) {
                val cyclicProperties = propertyType.properties.filter { it.type == trait }

                if (cyclicProperties.isNotEmpty()) {
                    throw invocation.make<TypeChecker>("Detected cyclic definition between trait '${trait.name}' and its property (${propertyPair.identifierNode.identifier} ${propertyType.name})", propertyPair.typeIdentifierNode)
                }
            }

            members.add(Property(propertyPair.identifierNode.identifier, propertyType))
        }

        trait = Trait(node.getPath(), members)

        node.annotate(trait, Annotations.Type)

        return trait
    }
}

class TraitSignaturesTypeResolver(override val node: TraitDefNode, override val binding: Binding) : EntityTypeResolver<TraitDefNode, Trait>, KoinComponent {
    constructor(pair: Pair<TraitDefNode, Binding>) : this(pair.first, pair.second)

    override fun resolve(environment: Environment, context: Context) : Trait {
        var partialTrait = node.getType() as Trait

        val signatures = mutableListOf<SignatureProtocol<*>>()
        for (signature in node.signatures) {
            val signatureType = MethodSignatureTypeResolver(signature, binding, partialTrait)
                .resolve(environment, context)

            signatures.add(signatureType)
        }

        partialTrait = Trait(node.getPath(), partialTrait.properties, signatures)

        node.annotate(partialTrait, Annotations.Type)

        return partialTrait
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

        node.annotate(type, Annotations.Type)

        return type
    }
}

class TraitConformanceTypeResolver(override val node: TypeDefNode, override val binding: Binding) : EntityTypeResolver<TypeDefNode, Type>, KoinComponent {
    private val invocation: Invocation by inject()
    private val parserResult: Parser.Result by injectResult(CompilationSchemeEntry.parser)

    constructor(pair: Pair<TypeDefNode, Binding>) : this(pair.first, pair.second)

    override fun resolve(environment: Environment, context: Context): Type {
        var partialType = node.getType() as Type

        if (node.traitConformances.isEmpty()) return partialType

        for (tc in node.traitConformances) {
            val traitPath = tc.getPath()
            val traitType = context.getType(traitPath) as? Trait
                ?: throw invocation.make<TypeChecker>("Types may only declare conformance to Traits, found ${traitPath.toString(OrbitMangler)} which is not a Trait", node)

            val traitNodes = parserResult.ast.search(TraitDefNode::class.java)
                .filter { it.typeIdentifierNode == tc }

            assert(traitNodes.size == 1) {
                "FATAL - TRAIT NOT NOT FOUND -- ${tc.value}"
            }

            val traitNode = traitNodes.first()

            // Synthesise Trait properties for this concrete type
            traitNode.propertyPairs
                .forEach(node::extendProperties)

            // 1. Injected trait property definitions into type
            partialType = Type(node.getPath(), traitType.properties)

            node.annotate(partialType, Annotations.Type)
            context.add(partialType)

            if (traitType.signatures.isEmpty()) continue

            // 2. We expect to see a method signature matching for each of those declared by the Trait
            for (signature in traitType.signatures) {
                val matches = context.bindings.values
                    .filterIsInstance<SignatureProtocol<*>>()
                    .filter { it.name == signature.name }
                    .filter {
                        when (it) {
                            is InstanceSignature -> {
                                val t = context.refresh(it.receiver.type)
                                t == partialType
                            }
                            else -> it.receiver == partialType
                        }
                    }
                    .filter {
                        signature.isSatisfied(context, it)
                    }

                if (matches.isEmpty()) {
                    val sig = "(${partialType.name}) (${signature.parameters.joinToString(", ") { "${it.name} ${it.type.name}" }}) (${signature.returnType.name})"
                    throw invocation.make<TypeChecker>("Type '${partialType.name}' declares conformance to Trait '${traitType.name}', but does not fulfill its contract. Must declare a method matching signature '$sig'", node)
                }
            }
        }

        return partialType
    }
}