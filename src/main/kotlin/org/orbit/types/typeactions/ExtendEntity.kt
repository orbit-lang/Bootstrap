package org.orbit.types.typeactions

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.getPath
import org.orbit.core.injectResult
import org.orbit.core.nodes.ExtensionNode
import org.orbit.core.nodes.ModuleNode
import org.orbit.graph.components.Binding
import org.orbit.graph.phase.NameResolverResult
import org.orbit.types.components.*
import org.orbit.types.phase.TypeSystem
import org.orbit.types.typeresolvers.MethodSignatureTypeResolver
import org.orbit.types.util.TraitConstructorMonomorphisation
import org.orbit.util.Invocation
import org.orbit.util.PrintableKey
import org.orbit.util.Printer

class ExtendEntity(private val node: ExtensionNode, private val moduleNode: ModuleNode) : TypeAction, KoinComponent {
    private val nameResolverResult: NameResolverResult by injectResult(CompilationSchemeEntry.canonicalNameResolver)
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()
    private lateinit var entity: Entity

    override fun execute(context: Context) {
        // TODO - Entity Constructors/Meta Types
        entity = when (val e = context.getTypeByPath(node.getPath())) {
            is Entity -> e
            is EntityConstructor -> {
                TypeInferenceUtil.infer(context, node.targetTypeNode) as Entity
            }
            else -> throw invocation.make<TypeSystem>("Only Entity types may be extended, found ${printer.apply(node.getPath().toString(OrbitMangler), PrintableKey.Italics, PrintableKey.Bold)}", node)
        }

        val localContext = Context(context)

        localContext.add(SelfType)

        if (entity is Trait) {
            // Any methods defined in a Trait Extension are templates for potential future monomorphisation
            for (methodDef in node.methodDefNodes) {
                val receiverPath = methodDef.signature.receiverTypeNode.getPath()

                if (receiverPath != Binding.Self.path) {
                    throw invocation.make<TypeSystem>("Trait Extensions may only define methods on receiver type Self", methodDef.signature.receiverTypeNode)
                }

                val typeResolver = MethodSignatureTypeResolver(methodDef.signature, Binding.Self, null)
                val result = typeResolver.resolve(nameResolverResult.environment, localContext)

                context.registerMonomorphisation(MethodTemplate(entity as Trait, result, methodDef.body))
            }
        } else if (entity is Type) {
            val trait = (entity as Type).synthesiseTrait()

            for (methodDef in node.methodDefNodes) {
                val receiverPath = methodDef.signature.receiverTypeNode.getPath()

                if (receiverPath != Binding.Self.path) {
                    throw invocation.make<TypeSystem>("Trait Extensions may only define methods on receiver type Self", methodDef.signature.receiverTypeNode)
                }

                val typeResolver = MethodSignatureTypeResolver(methodDef.signature, Binding.Self, null)
                val result = typeResolver.resolve(nameResolverResult.environment, localContext)

                context.registerMonomorphisation(MethodTemplate(trait, result, methodDef.body))
            }
        }
    }

    override fun describe(printer: Printer): String {
        return "Resolving extension methods for entity ${printer.apply(entity.name, PrintableKey.Italics, PrintableKey.Bold)}"
    }
}