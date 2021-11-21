package org.orbit.types.typeactions

import org.koin.core.component.KoinComponent
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.getPath
import org.orbit.core.injectResult
import org.orbit.core.nodes.TraitDefNode
import org.orbit.graph.components.Binding
import org.orbit.graph.phase.NameResolverResult
import org.orbit.types.components.Context
import org.orbit.types.components.Trait
import org.orbit.types.components.Type
import org.orbit.types.components.TypeSignature
import org.orbit.types.typeresolvers.MethodSignatureTypeResolver
import org.orbit.util.Printer

class ResolveTraitSignatures(private val node: TraitDefNode) : TypeAction {
    private companion object : KoinComponent {
        private val nameResolverResult: NameResolverResult by injectResult(CompilationSchemeEntry.canonicalNameResolver)
    }

    private lateinit var trait: Trait
    private lateinit var signatures: List<TypeSignature>

    override fun execute(context: Context) {
        trait = context.getTypeByPath(node.getPath()) as Trait

        val localContext = Context(context)

        localContext.add(Type("Self"))

        signatures = node.signatures
            .map {
                MethodSignatureTypeResolver(it, Binding(Binding.Kind.Method, it.identifierNode.identifier, it.getPath()), trait)
                    .resolve(nameResolverResult.environment, localContext)
            }

        trait = Trait(trait.name, trait.typeParameters, trait.properties, trait.traitConformance, signatures, trait.equalitySemantics, trait.implicit)

        context.replace(trait)
    }

    override fun describe(printer: Printer): String {
        val sigs = signatures.joinToString("\n\t\t") { it.toString(printer) }
        return """
            |Resolving Trait signatures for Trait ${trait.toString(printer)}
            |        $sigs
        """.trimMargin()
    }
}