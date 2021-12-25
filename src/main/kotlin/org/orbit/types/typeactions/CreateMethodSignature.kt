package org.orbit.types.typeactions

import org.koin.core.component.KoinComponent
import org.orbit.core.OrbitMangler
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.getPath
import org.orbit.core.injectResult
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.core.nodes.ModuleNode
import org.orbit.graph.components.Binding
import org.orbit.graph.phase.NameResolverResult
import org.orbit.types.components.Context
import org.orbit.types.components.Module
import org.orbit.types.components.SignatureProtocol
import org.orbit.types.typeresolvers.MethodSignatureTypeResolver
import org.orbit.util.Printer

class CreateMethodSignature(private val node: MethodSignatureNode, private val moduleNode: ModuleNode) : TypeAction,
    KoinComponent {
    private val nameResolverResult: NameResolverResult by injectResult(CompilationSchemeEntry.canonicalNameResolver)
    private var result: SignatureProtocol<*>? = null

    override fun execute(context: Context) {
        val module = context.getTypeByPath(moduleNode.getPath()) as Module
        val typeResolver = MethodSignatureTypeResolver(node, Binding.Self, null)
        result = typeResolver.resolve(nameResolverResult.environment, context)

        context.bind(node.getPath().toString(OrbitMangler), result!!)

        val nModule = Module(module.name, entities = module.entities, signatures = module.signatures + result!!)

        context.remove(nModule.name)
        context.add(nModule)
    }

    override fun describe(printer: Printer): String {
        return "Create method signature type\n\t\t${result!!.toString(printer)}"
    }
}