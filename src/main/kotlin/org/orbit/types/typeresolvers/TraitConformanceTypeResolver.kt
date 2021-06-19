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
import org.orbit.frontend.phase.Parser
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.extensions.annotate
import org.orbit.types.components.*
import org.orbit.types.phase.TypeChecker
import org.orbit.util.Invocation

class TraitConformanceTypeResolver(override val node: TypeDefNode, override val binding: Binding) : EntityTypeResolver<TypeDefNode, Type>,
    KoinComponent {
    override val invocation: Invocation by inject()
    private val parserResult: Parser.Result by injectResult(CompilationSchemeEntry.parser)

    constructor(pair: Pair<TypeDefNode, Binding>) : this(pair.first, pair.second)

    override fun resolve(environment: Environment, context: Context): Type {
        var partialType = node.getType() as Type

        if (node.traitConformances.isEmpty()) return partialType

        for (tc in node.traitConformances) {
            val traitPath = tc.getPath()
            val traitType = TypeExpressionTypeResolver(tc, binding)
                .resolve(environment, context)
                .evaluate(context)
                as? Trait
                ?: throw invocation.make<TypeChecker>("Types may only declare conformance to Traits, found ${traitPath.toString(
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