package org.orbit.types.typeresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.getPath
import org.orbit.core.getType
import org.orbit.core.injectResult
import org.orbit.core.nodes.MetaTypeNode
import org.orbit.core.nodes.TraitDefNode
import org.orbit.core.nodes.TypeDefNode
import org.orbit.core.nodes.TypeProjectionNode
import org.orbit.frontend.phase.Parser
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.extensions.annotate
import org.orbit.types.components.*
import org.orbit.types.phase.TypeSystem
import org.orbit.util.DuplicateTraitConformance
import org.orbit.util.Invocation
import org.orbit.util.error

/**
 * Type projections extend the list of traits the affected type conforms to. Conformance is checked by a later phase.
 */
class TypeProjectionTypeResolver(override val node: TypeProjectionNode, override val binding: Binding) : TypeResolver<TypeProjectionNode, TypeProjection>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun resolve(environment: Environment, context: Context): TypeProjection {
        val typePath = node.typeIdentifier.getPath()
        val traitPath = node.traitIdentifier.getPath()

        val type = context.getTypeByPath(typePath) as Type
        val trait = context.getTypeByPath(traitPath) as Trait

        if (type.traitConformance.contains(trait)) {
            throw invocation.error<TypeSystem>(DuplicateTraitConformance(trait, type))
        }

        val nType = Type(type.name, type.typeParameters, type.properties, type.traitConformance + trait, type.equalitySemantics, type.isRequired, typeConstructor = type.typeConstructor)

        context.remove(type.name)
        context.add(nType)

        val typeProjection = TypeProjection(type, trait)

        return typeProjection
    }
}

class TraitConformanceTypeResolver(override val node: TypeDefNode, override val binding: Binding) : EntityTypeResolver<TypeDefNode, Type>,
    KoinComponent {
    override val invocation: Invocation by inject()
    private val parserResult: Parser.Result by injectResult(CompilationSchemeEntry.parser)

    override fun resolve(environment: Environment, context: Context): Type {
        var partialType = node.getType() as Type

        if (node.traitConformances.isEmpty()) return partialType

        for (tc in node.traitConformances) {
            val traitPath = tc.getPath()
            val traitType = TypeExpressionTypeResolver(tc, binding)
                .resolve(environment, context)
                .evaluate(context)
                as? Trait
                ?: throw invocation.make<TypeSystem>("Types may only declare conformance to Traits, found ${traitPath.toString(
                    OrbitMangler
                )} which is not a Trait", node)

            tc.annotate(traitType, Annotations.Type)

            if (tc !is MetaTypeNode) {

                val traitNodes = parserResult.ast.search(TraitDefNode::class.java)
                    .filter { it.typeIdentifierNode == tc }

                assert(traitNodes.size == 1) {
                    "FATAL - TRAIT NOT NOT FOUND -- ${tc.value}"
                }

                val traitNode = traitNodes.first()

                // Synthesise Trait properties for this concrete type
                traitNode.propertyPairs
                    .forEach(node::extendProperties)
            }

            // 1. Injected trait property definitions into type
            partialType = Type(node.getPath(), properties = traitType.properties, isRequired = node.isRequired)

            node.annotate(partialType, Annotations.Type)
            context.add(partialType)

            if (traitType.signatures.isEmpty()) continue

            // 2. We expect to see a method signature matching for each of those declared by the Trait
            for (signature in traitType.signatures) {
                val matches = context.bindings.values
                    .filterIsInstance<SignatureProtocol<*>>()
                    .filter { it.name == signature.name }
                    .filter {
                        it.receiver == partialType
                    }
                    .filter {
                        signature.isSatisfied(context, it)
                    }

                if (matches.isEmpty()) {
                    val sig = "(${partialType.name}) (${signature.parameters.joinToString(", ") { "${it.name} ${it.type.name}" }}) (${signature.returnType.name})"
                    throw invocation.make<TypeSystem>("Type '${partialType.name}' declares conformance to Trait '${traitType.name}', but does not fulfill its contract. Must declare a method matching signature '$sig'", node)
                }
            }
        }

        return partialType
    }
}